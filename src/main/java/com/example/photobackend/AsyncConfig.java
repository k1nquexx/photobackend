package com.example.photobackend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    @Override
    @Bean(name = "notificationExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("NotificationAsync-");

        // Graceful Shutdown для фонового пула потоков:
        // Ждем завершения выполнения активных задач перед остановкой контейнера
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }

    /**
     * Централизованный обработчик необработанных исключений в асинхронных методах (void @Async).
     * Предотвращает бесследное растворение ошибок внешних API (почта, мессенджеры).
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            log.error("КРИТИЧЕСКАЯ ОШИБКА в асинхронном методе '{}': {}", 
                    method.getName(), throwable.getMessage(), throwable);
            for (int i = 0; i < params.length; i++) {
                log.error("Параметр [{}]: {}", i, params[i]);
            }
        };
    }
}
