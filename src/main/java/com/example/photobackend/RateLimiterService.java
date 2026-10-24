package com.example.photobackend;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    private static class Counter {
        long windowStart;
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

        // Очистка памяти при большом числе клиентов
        if (requestCounters.size() > 10000) {
            requestCounters.entrySet().removeIf(e -> (now - e.getValue().windowStart) > (windowSeconds * 2));
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
}
