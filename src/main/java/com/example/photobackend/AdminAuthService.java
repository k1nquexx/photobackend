package com.example.photobackend;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Value("${admin.password.hash:}")
    private String adminPasswordHash;

    private final AppConfigProperties appConfigProperties;

    // Секретный ключ для подписи stateless HMAC-SHA256 токенов
    private byte[] hmacKey;

    // Ожидаемый хэш пароля администратора для безопасного побайтового сравнения за постоянное время
    private byte[] expectedPasswordHashBytes;

    // Черный список отозванных токенов при явном выходе (logout)
    private final Set<String> revokedTokens = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Autowired
    public AdminAuthService(AppConfigProperties appConfigProperties) {
        this.appConfigProperties = appConfigProperties;
    }

    @PostConstruct
    public void init() {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            // 1. Проверяем наличие постоянного хэша из переменной окружения ADMIN_PASSWORD_HASH
            if (adminPasswordHash != null && !adminPasswordHash.isBlank()) {
                this.expectedPasswordHashBytes = hexToBytes(adminPasswordHash.trim());
                this.hmacKey = md.digest(("HMAC_SALT_2026_HASH:" + adminPasswordHash.trim()).getBytes(StandardCharsets.UTF_8));
                log.info("Пароль администратора успешно инициализирован из постоянного хэша ADMIN_PASSWORD_HASH.");
                return;
            }

            // 2. Если хэш не задан, используем пароль из конфигурации/переменной окружения
            String effectivePassword = (adminPassword != null && !adminPassword.isBlank()) ? adminPassword.trim() : "admin2026";
            this.adminPassword = effectivePassword;
            this.expectedPasswordHashBytes = md.digest(effectivePassword.getBytes(StandardCharsets.UTF_8));
            this.hmacKey = md.digest(("HMAC_SALT_2026:" + effectivePassword).getBytes(StandardCharsets.UTF_8));
            log.info("Пароль администратора успешно инициализирован.");
        } catch (NoSuchAlgorithmException e) {
            log.error("Алгоритм SHA-256 недоступен", e);
        }
    }

    public String login(String password) {
        if (password == null || this.expectedPasswordHashBytes == null) {
            return null;
        }

        // Проверка пароля за постоянное время (Constant-Time Digest Comparison)
        if (verifyPasswordConstantTime(password.trim())) {
            long now = Instant.now().getEpochSecond();
            long ttl = appConfigProperties != null ? appConfigProperties.getSecurity().getTokenTtlSeconds() : 7200;
            long exp = now + ttl;
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

        if (isRevokedConstantTime(token)) {
            return false;
        }

        int dotIdx = token.lastIndexOf('.');
        String payloadPart = token.substring(0, dotIdx);
        String sigPart = token.substring(dotIdx + 1);

        String expectedSig = sign(payloadPart);
        // Защита от Timing Attacks: побайтовое сравнение фиксированных хэшей за постоянное время
        if (!constantTimeEquals(expectedSig, sigPart)) {
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

    private boolean isRevokedConstantTime(String token) {
        boolean revoked = false;
        for (String r : revokedTokens) {
            if (constantTimeEquals(r, token)) {
                revoked = true;
            }
        }
        return revoked;
    }

    /**
     * Сравнение двух строк за строго постоянное время через SHA-256 дайджесты фиксированной длины.
     * Полностью исключает утечку информации по задержкам ответа при подборе токена (Timing Attack).
     */
    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashA = md.digest(a.getBytes(StandardCharsets.UTF_8));
            byte[] hashB = md.digest(b.getBytes(StandardCharsets.UTF_8));
            return MessageDigest.isEqual(hashA, hashB);
        } catch (NoSuchAlgorithmException e) {
            byte[] bytesA = a.getBytes(StandardCharsets.UTF_8);
            byte[] bytesB = b.getBytes(StandardCharsets.UTF_8);
            if (bytesA.length != bytesB.length) return false;
            int result = 0;
            for (int i = 0; i < bytesA.length; i++) {
                result |= bytesA[i] ^ bytesB[i];
            }
            return result == 0;
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

    @Scheduled(fixedRate = 300000)
    public void evictExpiredTokens() {
        int before = revokedTokens.size();
        revokedTokens.removeIf(this::isTokenExpired);
        int after = revokedTokens.size();
        if (before != after) {
            log.debug("Очистка отозванных токенов: удалено {} устаревших записей", before - after);
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

    /**
     * Сравнение хэшей за постоянное время (защита от атак по времени / Timing Attacks).
     */
    private boolean verifyPasswordConstantTime(String inputPassword) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] inputHash = md.digest(inputPassword.getBytes(StandardCharsets.UTF_8));
            return MessageDigest.isEqual(inputHash, this.expectedPasswordHashBytes);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 недоступен", e);
            return false;
        }
    }

    private byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                 + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
    }
}
