package com.example.photobackend;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Фильтр безопасности HTTP-заголовков.
 * Защищает от Clickjacking (iframe), MIME-Sniffing, утечек Referrer с токенами и XSS.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (response instanceof HttpServletResponse httpResponse) {
            // Защита от Clickjacking: запрет отображения сайта во враждебных <iframe>
            httpResponse.setHeader("X-Frame-Options", "SAMEORIGIN");

            // Защита от MIME-Sniffing: браузер обязан строго следовать Content-Type
            httpResponse.setHeader("X-Content-Type-Options", "nosniff");

            // Защита от утечки полного URL с токенами во внешние сервисы
            httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

            // Ограничение доступа браузера к оборудованию
            httpResponse.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");

            // Content-Security-Policy: ограничение источников скриптов, стилей и фреймов
            httpResponse.setHeader("Content-Security-Policy",
                    "default-src 'self'; " +
                    "script-src 'self'; " +
                    "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                    "font-src 'self' https://fonts.gstatic.com; " +
                    "img-src 'self' data: https: blob:; " +
                    "connect-src 'self'; " +
                    "frame-ancestors 'self'; " +
                    "base-uri 'self'; " +
                    "form-action 'self';");
        }
        chain.doFilter(request, response);
    }
}
