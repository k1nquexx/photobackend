package com.example.photobackend;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Безопасный определитель IP-адреса клиента.
 * Доверяет заголовкам переадресации (X-Forwarded-For, X-Real-IP) ТОЛЬКО если
 * сетевой сокет-адрес входит в список доверенных прокси. Предотвращает обход RateLimiter.
 */
@Component
public class ClientIpResolver {

    private final AppConfigProperties appConfigProperties;

    private static final Set<String> DEFAULT_TRUSTED_PROXIES = Set.of(
            "127.0.0.1", "0:0:0:0:0:0:0:1", "::1", "localhost"
    );

    @Autowired
    public ClientIpResolver(AppConfigProperties appConfigProperties) {
        this.appConfigProperties = appConfigProperties;
    }

    public String resolveClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";

        String remoteAddr = request.getRemoteAddr();
        if (remoteAddr == null || remoteAddr.isBlank()) {
            return "unknown";
        }

        // Если сетевой сокет принадлежит доверенному reverse proxy (Nginx / Cloudflare):
        if (isTrustedProxy(remoteAddr.trim())) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                // Первый IP в списке — реальный клиентский адрес
                String clientIp = forwarded.split(",")[0].trim();
                if (!clientIp.isEmpty()) {
                    return clientIp;
                }
            }
            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank()) {
                return realIp.trim();
            }
        }

        // Если запрос отправлен напрямую или через недоверенный узел,
        // игнорируем любые пользовательские заголовки переадресации
        return remoteAddr.trim();
    }

    private boolean isTrustedProxy(String ip) {
        if (DEFAULT_TRUSTED_PROXIES.contains(ip)) {
            return true;
        }
        if (appConfigProperties != null && appConfigProperties.getSecurity() != null) {
            List<String> configured = appConfigProperties.getSecurity().getTrustedProxies();
            return configured != null && configured.contains(ip);
        }
        return false;
    }
}
