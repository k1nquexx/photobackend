package com.example.photobackend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

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
     * Ограничение CORS источников исключительно доверенными доменами сайта из конфигурации.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> allowedOrigins = appConfigProperties != null ? appConfigProperties.getCorsAllowedOrigins() : List.of("http://localhost:8080");
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.toArray(new String[0]))
                .allowedMethods("GET", "POST", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
