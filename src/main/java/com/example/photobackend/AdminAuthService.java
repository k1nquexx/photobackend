package com.example.photobackend;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AdminAuthService {

    private static final Logger log = LoggerFactory.getLogger(AdminAuthService.class);

    @Value("${admin.password:}")
    private String adminPassword;

    // Срок действия сессии администратора: 2 часа
    private static final long TOKEN_TTL_SECONDS = 2 * 3600;

    // Секретный ключ для подписи stateless HMAC-SHA256 токенов
    private byte[] hmacKey;

    // Черный список отозванных токенов при явном выходе (logout)
    private final Set<String> revokedTokens = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @PostConstruct
    public void init() {
        if (adminPassword == null || adminPassword.isBlank() || "admin2026".equalsIgnoreCase(adminPassword.trim())) {
            String generated = "admin-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            this.adminPassword = generated;

            String banner = "\n" +
                "==================================================================================\n" +
                "[ВНИМАНИЕ БЕЗОПАСНОСТИ] Переменная окружения ADMIN_PASSWORD не задана!\n" +
                "В целях защиты статический пароль 'admin2026' ОТКЛЮЧЕН.\n" +
                "Сгенерирован временный случайный пароль администратора:\n\n" +
                "    >>>  " + generated + "  <<<\n\n" +
                "Используйте его для входа в /admin.html.\n" +
                "Для установки постоянного пароля задайте переменную окружения: ADMIN_PASSWORD=...\n" +
                "==================================================================================";
            log.warn(banner);
        } else {
            log.info("Пароль администратора успешно инициализирован из защищенной конфигурации.");
        }

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            this.hmacKey = md.digest(("HMAC_SALT_2026:" + adminPassword.trim()).getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            log.error("Алгоритм SHA-256 недоступен для инициализации HMAC", e);
            this.hmacKey = adminPassword.trim().getBytes(StandardCharsets.UTF_8);
        }
    }

    public String login(String password) {
        if (password == null || adminPassword == null) {
            return null;
        }

        if (verifyPassword(password.trim(), adminPassword.trim())) {
            long now = Instant.now().getEpochSecond();
            long exp = now + TOKEN_TTL_SECONDS;
            String payload = now + ":" + exp + ":" + UUID.randomUUID().toString();
            String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
            String signature = sign(encodedPayload);
            return encodedPayload + "." + signature;
        }
        return null;
    }

    public boolean isValid(String token) {
        if (token == null || token.isBlank() || !token.contains(".")) {
            return false;
        }

        if (revokedTokens.contains(token)) {
            return false;
        }

        int dotIdx = token.lastIndexOf('.');
        String payloadPart = token.substring(0, dotIdx);
        String sigPart = token.substring(dotIdx + 1);

        String expectedSig = sign(payloadPart);
        if (!MessageDigest.isEqual(expectedSig.getBytes(StandardCharsets.UTF_8), sigPart.getBytes(StandardCharsets.UTF_8))) {
            return false;
        }

        try {
            String decoded = new String(Base64.getUrlDecoder().decode(payloadPart), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":");
            if (parts.length < 2) return false;

            long exp = Long.parseLong(parts[1]);
            return Instant.now().getEpochSecond() <= exp;
        } catch (IllegalArgumentException e) {
            log.debug("Невалидный payload токена: {}", e.getMessage());
            return false;
        }
    }

    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            revokedTokens.add(token);
            if (revokedTokens.size() > SecurityConstants.MAX_REVOKED_TOKENS) {
                evictExpiredTokens();
            }
        }
    }

    // Регулярная очистка устаревших токенов из памяти каждые 5 минут для защиты Heap от переполнения
    @Scheduled(fixedRate = 300000)
    public void evictExpiredTokens() {
        int before = revokedTokens.size();
        revokedTokens.removeIf(this::isTokenExpired);
        int after = revokedTokens.size();
        if (before != after) {
            log.debug("Очистка отозванных токенов: удалено {} устаревших записей, осталось {}", before - after, after);
        }
    }

    private boolean isTokenExpired(String token) {
        if (token == null || !token.contains(".")) return true;
        try {
            int dotIdx = token.lastIndexOf('.');
            String payloadPart = token.substring(0, dotIdx);
            String decoded = new String(Base64.getUrlDecoder().decode(payloadPart), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":");
            if (parts.length < 2) return true;
            long exp = Long.parseLong(parts[1]);
            return Instant.now().getEpochSecond() > exp;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(this.hmacKey, "HmacSHA256"));
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Критическая ошибка создания HMAC подписи токена", e);
            throw new IllegalStateException("Ошибка подписи токена", e);
        }
    }

    private boolean verifyPassword(String inputPassword, String expectedPassword) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] inputHash = md.digest(inputPassword.getBytes(StandardCharsets.UTF_8));
            byte[] expectedHash = md.digest(expectedPassword.getBytes(StandardCharsets.UTF_8));
            return MessageDigest.isEqual(inputHash, expectedHash);
        } catch (NoSuchAlgorithmException e) {
            log.error("Алгоритм SHA-256 недоступен для проверки пароля", e);
            return inputPassword.equals(expectedPassword);
        }
    }
}
