package com.example.photobackend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceUnitTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @Mock
    private PricingService pricingService;

    @Mock
    private RateLimiterService rateLimiterService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private BookingService bookingService;

    @Test
    void createBooking_Success_PublishesEventAndReturnsPublicUuid() {
        BookingCreateRequest req = new BookingCreateRequest();
        req.setClientName("Анна Каренина");
        req.setClientPhone("+7 (911) 222-33-44");
        req.setDetails("Студийная съемка");
        req.setBookingDate("2026-11-15");
        req.setBookingTime("14:00");
        req.setShootTypeRate(2500);
        req.setHours(2);

        when(pricingService.calculatePrice(nullable(BigDecimal.class), any(), anyBoolean(), anyBoolean(), any()))
                .thenReturn(new BigDecimal("5000.00"));
        when(timeSlotRepository.bookSlotAtomically("2026-11-15", "14:00")).thenReturn(1);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(101L);
            return b;
        });

        var result = bookingService.createBooking(req, "192.168.1.10");

        assertNotNull(result);
        assertTrue((Boolean) result.get("success"));
        assertNotNull(result.get("publicId"));
        // Публичный идентификатор должен быть UUID v4 (36 символов, дефисы)
        String publicId = (String) result.get("publicId");
        assertEquals(36, publicId.length());
        assertTrue(publicId.matches("^[0-9a-fA-F\\-]{36}$"));
        assertNotNull(result.get("trackingToken"));

        // Проверяем вызов атомарной фиксации слота
        verify(timeSlotRepository).bookSlotAtomically("2026-11-15", "14:00");

        // Проверяем публикацию асинхронного события приложения
        ArgumentCaptor<BookingCreatedEvent> eventCaptor = ArgumentCaptor.forClass(BookingCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        BookingCreatedEvent publishedEvent = eventCaptor.getValue();
        assertNotNull(publishedEvent);
        assertEquals(publicId, publishedEvent.getBookingDto().getPublicId());
    }

    @Test
    void createBooking_SlotAlreadyBooked_ThrowsSlotConflictException() {
        BookingCreateRequest req = new BookingCreateRequest();
        req.setClientName("Иван Иванов");
        req.setClientPhone("+79991234567");
        req.setBookingDate("2026-11-15");
        req.setBookingTime("14:00");

        when(pricingService.calculatePrice(nullable(BigDecimal.class), any(), anyBoolean(), anyBoolean(), any()))
                .thenReturn(new BigDecimal("2000.00"));
        when(timeSlotRepository.bookSlotAtomically("2026-11-15", "14:00")).thenReturn(0);

        assertThrows(SlotConflictException.class, () -> bookingService.createBooking(req, "127.0.0.1"));
        verify(bookingRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void checkStatus_ByTrackingToken_Success() {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setPublicId("pub-uuid-1234");
        booking.setClientName("Дмитрий Соколов");
        booking.setClientPhone("+7 (900) 123-45-67");
        booking.setStatus("Готово");
        booking.setTrackingToken("TRK-TEST1234");
        booking.setDownloadUrl("https://example.com/photos");

        when(rateLimiterService.tryAcquire(anyString(), anyInt(), anyLong())).thenReturn(true);
        when(bookingRepository.findByTrackingToken("TRK-TEST1234")).thenReturn(Optional.of(booking));

        BookingResponseDto dto = bookingService.checkStatus("TRK-TEST1234", null, null, "127.0.0.1");

        assertNotNull(dto);
        assertEquals("pub-uuid-1234", dto.getPublicId());
        assertEquals("Дмитрий С.", dto.getMaskedClientName());
        assertEquals("Готово", dto.getStatus());
        assertEquals("https://example.com/photos", dto.getDownloadUrl());
    }

    @Test
    void checkStatus_WrongPhone_ThrowsResourceNotFoundException() {
        Booking booking = new Booking();
        booking.setId(2L);
        booking.setPublicId("uuid-abc-def");
        booking.setClientName("Ольга Петрова");
        booking.setClientPhone("+7 (999) 555-44-33");

        when(rateLimiterService.tryAcquire(anyString(), anyInt(), anyLong())).thenReturn(true);
        when(bookingRepository.findByPublicId("uuid-abc-def")).thenReturn(Optional.of(booking));

        assertThrows(ResourceNotFoundException.class, () -> 
            bookingService.checkStatus(null, "uuid-abc-def", "+7 (999) 000-00-00", "127.0.0.1")
        );
    }

    @Test
    void checkStatus_RateLimitExceeded_ThrowsTooManyRequestsException() {
        when(rateLimiterService.tryAcquire(anyString(), anyInt(), anyLong())).thenReturn(false);

        assertThrows(TooManyRequestsException.class, () -> 
            bookingService.checkStatus("TRK-ANY", null, null, "127.0.0.1")
        );
    }

    @Test
    void updateStatus_Cancelled_ReleasesTimeSlot() {
        Booking booking = new Booking();
        booking.setId(5L);
        booking.setPublicId("uuid-order-5");
        booking.setBookingDate("2026-12-01");
        booking.setBookingTime("16:00");
        booking.setStatus("Новая");

        TimeSlot slot = new TimeSlot("2026-12-01", "16:00");
        slot.setBooked(true);

        when(bookingRepository.findByPublicId("uuid-order-5")).thenReturn(Optional.of(booking));
        when(timeSlotRepository.findBySlotDateAndSlotTime("2026-12-01", "16:00")).thenReturn(Optional.of(slot));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        BookingResponseDto updated = bookingService.updateStatus("uuid-order-5", "Отменена");

        assertEquals("Отменена", updated.getStatus());
        assertFalse(slot.isBooked(), "Слот должен быть освобожден при отмене");
        verify(timeSlotRepository).save(slot);
        verify(bookingRepository).save(booking);
    }

    @Test
    void updateDownloadUrl_InvalidUrl_ThrowsBadRequestException() {
        assertThrows(BadRequestException.class, () -> 
            bookingService.updateDownloadUrl("uuid-1", "javascript:alert(1)")
        );
    }

    @Test
    void deleteBooking_ReleasesSlotAndDeletesEntity() {
        Booking booking = new Booking();
        booking.setId(10L);
        booking.setPublicId("uuid-del-10");
        booking.setBookingDate("2026-12-10");
        booking.setBookingTime("11:00");

        TimeSlot slot = new TimeSlot("2026-12-10", "11:00");
        slot.setBooked(true);

        when(bookingRepository.findByPublicId("uuid-del-10")).thenReturn(Optional.of(booking));
        when(timeSlotRepository.findBySlotDateAndSlotTime("2026-12-10", "11:00")).thenReturn(Optional.of(slot));

        bookingService.deleteBooking("uuid-del-10");

        assertFalse(slot.isBooked());
        verify(timeSlotRepository).save(slot);
        verify(bookingRepository).delete(booking);
    }
}
