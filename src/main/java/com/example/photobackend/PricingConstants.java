package com.example.photobackend;

public final class PricingConstants {

    private PricingConstants() {}

    // Тарифы (руб/час)
    public static final int RATE_PORTRAIT = 1000;
    public static final int RATE_EVENT = 2000;

    // Дополнительные услуги
    public static final int ADDON_EXPRESS_PRICE = 1000; // Готовность за 24ч
    public static final int ADDON_STUDIO_PRICE = 500;   // Помощь с арендой студии

    // Промокоды
    public static final String PROMO_START10 = "START10";
    public static final double PROMO_START10_DISCOUNT_MULTIPLIER = 0.9; // Скидка 10%

    public static final String PROMO_FOTO500 = "FOTO500";
    public static final int PROMO_FOTO500_DISCOUNT_RUB = 500; // Скидка 500 руб.

    // Ограничения на часы
    public static final int DEFAULT_HOURS = 2;
    public static final int MIN_HOURS = 1;
    public static final int MAX_HOURS = 24;
}
