package com.example.photobackend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Валидатор безопасности продакшн-окружения.
 * Прерывает старт приложения с фатальной ошибкой, если в боевом профиле 'prod'
 * используются дефолтные или пустые секреты.
 */
@Component
public class ProductionSecurityValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ProductionSecurityValidator.class);

    private final Environment environment;

    public ProductionSecurityValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        if (activeProfiles.contains("prod")) {
            log.info("Проверка конфигурации боевых секретов для профиля 'prod'...");

            String adminHash = environment.getProperty("admin.password.hash");
            String adminPass = environment.getProperty("admin.password");

            boolean hasSecureHash = adminHash != null && !adminHash.isBlank();
            boolean isInsecurePass = adminPass != null && ("admin2026".equalsIgnoreCase(adminPass.trim()) || "admin".equalsIgnoreCase(adminPass.trim()));

            if (!hasSecureHash && (adminPass == null || adminPass.isBlank() || isInsecurePass)) {
                throw new IllegalStateException("КРИТИЧЕСКИЙ СБОЙ: Запуск в профиле 'prod' с дефолтным ('admin2026') или отсутствующим паролем администратора запрещен! Задайте переменную окружения ADMIN_PASSWORD_HASH.");
            }

            String dbPass = environment.getProperty("spring.datasource.password");
            if (dbPass == null || dbPass.isBlank() || "password".equalsIgnoreCase(dbPass.trim())) {
                throw new IllegalStateException("КРИТИЧЕСКИЙ СБОЙ: Запуск в профиле 'prod' со стандартным паролем базы данных ('password') запрещен! Задайте переменную окружения SPRING_DATASOURCE_PASSWORD.");
            }

            log.info("Проверка безопасности пройдена: боевые секреты сконфигурированы.");
        }
    }
}
