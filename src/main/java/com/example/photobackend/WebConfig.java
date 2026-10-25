package com.example.photobackend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AdminAuthInterceptor adminAuthInterceptor;
    private final AppConfigProperties appConfigProperties;

    @Autowired
    public WebConfig(AdminAuthInterceptor adminAuthInterceptor, AppConfigProperties appConfigProperties) {
        this.adminAuthInterceptor = adminAuthInterceptor;
        this.appConfigProperties = appConfigProperties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/api/**");
    }

    /**
     * Кэширование статических изображений на клиенте и промежуточных CDN
     * со сроком действия 30 дней (max-age=2592000).
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/img/**")
                .addResourceLocations("classpath:/static/img/")
                .setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic());
    }

    /**
     * Ограничение CORS исключительно доверенными доменами сайта из конфигурации.
     * Запрещает подстановочный символ '*' при передаче учетных данных (Credentials).
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> rawOrigins = appConfigProperties != null ? appConfigProperties.getCorsAllowedOrigins() : null;
        List<String> safeOrigins;

        if (rawOrigins == null || rawOrigins.isEmpty()) {
            safeOrigins = List.of("http://localhost:8080", "https://photographer.ru");
        } else {
            safeOrigins = rawOrigins.stream()
                    .map(String::trim)
                    .filter(origin -> !origin.isEmpty() && !"*".equals(origin))
                    .toList();
            if (safeOrigins.isEmpty()) {
                safeOrigins = List.of("http://localhost:8080", "https://photographer.ru");
            }
        }

        registry.addMapping("/api/**")
                .allowedOrigins(safeOrigins.toArray(new String[0]))
                .allowedMethods("GET", "POST", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "Accept", SecurityConstants.AUTH_HEADER_NAME)
                .exposedHeaders(SecurityConstants.AUTH_HEADER_NAME)
                .allowCredentials(true)
                .maxAge(3600);
    }
}
