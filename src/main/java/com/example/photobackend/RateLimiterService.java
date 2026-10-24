package com.example.photobackend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);

    private static class Counter {
        final long windowStart;
        int count;

        Counter(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }

    private final Map<String, Counter> requestCounters = new ConcurrentHashMap<>();

    /**
     * Попытка выполнить запрос с ограничением частоты.
     * @param key Уникальный ключ (например, "login:192.168.1.1")
     * @param maxRequests Максимальное количество разрешенных запросов в окне
     * @param windowSeconds Размер скользящего окна в секундах
     * @return true, если запрос разрешен; false, если лимит превышен
     */
    public boolean tryAcquire(String key, int maxRequests, long windowSeconds) {
        long now = Instant.now().getEpochSecond();

        // Защита памяти при пиковой нагрузке: если размер превысил лимит, немедленно очищаем устаревшие записи
        if (requestCounters.size() > SecurityConstants.MAX_RATE_LIMITER_ENTRIES) {
            evictExpiredEntries();
        }

        Counter counter = requestCounters.compute(key, (k, existing) -> {
            if (existing == null || (now - existing.windowStart) >= windowSeconds) {
                return new Counter(now, 1);
            }
            existing.count++;
            return existing;
        });

        return counter.count <= maxRequests;
    }

    /**
     * Фоновая периодическая очистка устаревших записей rate-limiter каждые 60 секунд.
     * Предотвращает утечку памяти (Memory Leak) в Heap при миллионах уникальных клиентских IP.
     */
    @Scheduled(fixedRate = 60000)
    public void evictExpiredEntries() {
        long now = Instant.now().getEpochSecond();
        int before = requestCounters.size();
        // Удаляем записи, чье окно завершилось более 120 секунд назад
        requestCounters.entrySet().removeIf(e -> (now - e.getValue().windowStart) > 120);
        int after = requestCounters.size();
        if (before != after) {
            log.debug("Очистка rate-limiter памяти: удалено {} устаревших счетчиков, осталось {}", before - after, after);
        }
    }
}
