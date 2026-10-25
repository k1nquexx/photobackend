package com.example.photobackend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeSlotServiceUnitTest {

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @InjectMocks
    private TimeSlotService timeSlotService;

    @Test
    void getAvailableSlots_ReturnsOnlyUnbookedSlotsAsDto() {
        TimeSlot slot1 = new TimeSlot("2026-11-20", "10:00");
        slot1.setId(1L);
        slot1.setBooked(false);

        TimeSlot slot2 = new TimeSlot("2026-11-20", "12:00");
        slot2.setId(2L);
        slot2.setBooked(false);

        when(timeSlotRepository.findBySlotDateAndIsBookedFalseOrderBySlotTimeAsc("2026-11-20"))
                .thenReturn(List.of(slot1, slot2));

        List<TimeSlotResponseDto> dtos = timeSlotService.getAvailableSlots("2026-11-20");

        assertNotNull(dtos);
        assertEquals(2, dtos.size());
        assertEquals("10:00", dtos.get(0).getSlotTime());
        assertEquals("12:00", dtos.get(1).getSlotTime());
        assertFalse(dtos.get(0).isBooked());
    }

    @Test
    void addSlot_Success_ReturnsDto() {
        TimeSlotRequestDto req = new TimeSlotRequestDto("2026-11-25", "15:00");

        when(timeSlotRepository.findBySlotDateAndSlotTime("2026-11-25", "15:00"))
                .thenReturn(Optional.empty());
        when(timeSlotRepository.save(any(TimeSlot.class))).thenAnswer(i -> {
            TimeSlot s = i.getArgument(0);
            s.setId(10L);
            return s;
        });

        TimeSlotResponseDto created = timeSlotService.addSlot(req);

        assertNotNull(created);
        assertEquals(10L, created.getId());
        assertEquals("2026-11-25", created.getSlotDate());
        assertEquals("15:00", created.getSlotTime());
        assertFalse(created.isBooked());
        verify(timeSlotRepository).save(any(TimeSlot.class));
    }

    @Test
    void addSlot_AlreadyExists_ThrowsBadRequestException() {
        TimeSlotRequestDto req = new TimeSlotRequestDto("2026-11-25", "15:00");
        TimeSlot existing = new TimeSlot("2026-11-25", "15:00");

        when(timeSlotRepository.findBySlotDateAndSlotTime("2026-11-25", "15:00"))
                .thenReturn(Optional.of(existing));

        assertThrows(BadRequestException.class, () -> timeSlotService.addSlot(req));
        verify(timeSlotRepository, never()).save(any());
    }

    @Test
    void addSlot_StartTimeAfterEndTime_ThrowsBadRequestException() {
        TimeSlotRequestDto req1 = new TimeSlotRequestDto("2026-11-25", "16:00 - 14:00");
        assertThrows(BadRequestException.class, () -> timeSlotService.addSlot(req1));

        TimeSlotRequestDto req2 = new TimeSlotRequestDto("2026-11-25", "15:00", "13:00");
        assertThrows(BadRequestException.class, () -> timeSlotService.addSlot(req2));

        TimeSlotRequestDto req3 = new TimeSlotRequestDto("2026-11-25", "14:00 - 14:00");
        assertThrows(BadRequestException.class, () -> timeSlotService.addSlot(req3));
    }

    @Test
    void deleteSlot_Success() {
        when(timeSlotRepository.existsById(42L)).thenReturn(true);
        timeSlotService.deleteSlot(42L);
        verify(timeSlotRepository).deleteById(42L);
    }
}
