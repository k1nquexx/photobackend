package com.example.photobackend;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    @Autowired
    private AdminAuthService adminAuthService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Пропускаем CORS preflight (OPTIONS)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        if (handler instanceof HandlerMethod handlerMethod) {
            boolean requiresAdmin = handlerMethod.hasMethodAnnotation(AdminOnly.class)
                    || handlerMethod.getBeanType().isAnnotationPresent(AdminOnly.class);

            if (requiresAdmin) {
                String token = extractToken(request);
                if (!adminAuthService.isValid(token)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setCharacterEncoding("UTF-8");
                    response.setContentType("text/plain;charset=UTF-8");
                    response.getWriter().write("Доступ запрещен: требуется действующий токен администратора");
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Извлечение авторизационного токена администратора:
     * 1. В приоритете защищенная HttpOnly сессионная кука 'admin_token'.
     * 2. Резервный заголовок X-Admin-Token для обратной совместимости с REST API / юнит-тестами.
     */
    public static String extractToken(HttpServletRequest request) {
        if (request == null) return null;

        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if ("admin_token".equals(c.getName())) {
                    String val = c.getValue();
                    if (val != null && !val.isBlank()) {
                        return val.trim();
                    }
                }
            }
        }

        String headerToken = request.getHeader(SecurityConstants.AUTH_HEADER_NAME);
        if (headerToken != null && !headerToken.isBlank()) {
            return headerToken.trim();
        }

        return null;
    }
}
