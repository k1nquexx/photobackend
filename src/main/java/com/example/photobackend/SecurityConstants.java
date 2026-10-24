package com.example.photobackend;

public final class SecurityConstants {

    private SecurityConstants() {}

    public static final String AUTH_HEADER_NAME = "X-Admin-Token";

    // Rate Limiter Defaults
    public static final int LOGIN_MAX_REQUESTS = 5;
    public static final long LOGIN_WINDOW_SECONDS = 60;

    public static final int STATUS_MAX_REQUESTS = 10;
    public static final long STATUS_WINDOW_SECONDS = 60;

    // Cache limits
    public static final int MAX_RATE_LIMITER_ENTRIES = 10_000;
    public static final int MAX_REVOKED_TOKENS = 5_000;
}
