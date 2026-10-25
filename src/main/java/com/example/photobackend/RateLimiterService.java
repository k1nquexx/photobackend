package com.example.photobackend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);

    private static class WindowBucket {
        final long windowStart;
        int count;

        WindowBucket(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }

        boolean isExpired(long now, long windowSeconds) {
            return (now - windowStart) >= windowSeconds;
        }
    }

    // Потокобезопасный кэш с автоматическим вытеснением устаревших окон
    private final Map<String, WindowBucket> cache = new ConcurrentHashMap<>();

    private final AppConfigProperties appConfigProperties;

    public RateLimiterService() {
        this.appConfigProperties = null;
    }

    @Autowired
    public RateLimiterService(AppConfigProperties appConfigProperties) {
        this.appConfigProperties = appConfigProperties;
    }

    /**
     * Попытка выполнить запрос с ограничением частоты и автоматическим удалением протухших записей.
     */
    public boolean tryAcquire(String key, int maxRequests, long windowSeconds) {
        long now = Instant.now().getEpochSecond();

        // 1. Автоматическая очистка протухших записей на лету
        WindowBucket bucket = cache.compute(key, (k, existing) -> {
            if (existing == null || existing.isExpired(now, windowSeconds)) {
                return new WindowBucket(now, 1);
            }
            existing.count++;
            return existing;
        });

        // 2. Защита от переполнения: если размер кэша аномально вырос, выполняем принудительное вытеснение
        if (cache.size() > SecurityConstants.MAX_RATE_LIMITER_ENTRIES) {
            evictExpiredEntries();
        }

        return bucket.count <= maxRequests;
    }

    /**
     * Фоновое автоматическое удаление протухших записей из кэша по расписанию.
     */
    @Scheduled(fixedRate = 60000)
    public void evictExpiredEntries() {
        long now = Instant.now().getEpochSecond();
        int before = cache.size();
        // Удаляем любые записи, чье окно устарело более чем на 120 секунд
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired(now, 120));
        int after = cache.size();
        if (before != after) {
            log.debug("RateLimiter Cache: удалено {} устаревших записей, осталось {}", before - after, after);
        }
    }
}
