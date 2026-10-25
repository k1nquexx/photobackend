package com.example.photobackend;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class PricingConstants {

    private PricingConstants() {}

    // Тарифы (руб/час) в BigDecimal
    public static final BigDecimal RATE_PORTRAIT = new BigDecimal("1000.00");
    public static final BigDecimal RATE_EVENT = new BigDecimal("2000.00");

    // Дополнительные услуги
    public static final BigDecimal ADDON_EXPRESS_PRICE = new BigDecimal("1000.00");
    public static final BigDecimal ADDON_STUDIO_PRICE = new BigDecimal("500.00");

    // Промокоды
    public static final String PROMO_START10 = "START10";
    public static final BigDecimal PROMO_START10_DISCOUNT_MULTIPLIER = new BigDecimal("0.90"); // Скидка 10%

    public static final String PROMO_FOTO500 = "FOTO500";
    public static final BigDecimal PROMO_FOTO500_DISCOUNT_RUB = new BigDecimal("500.00"); // Скидка 500 руб.

    // Ограничения на часы
    public static final int DEFAULT_HOURS = 2;
    public static final int MIN_HOURS = 1;
    public static final int MAX_HOURS = 24;

    // Банковское округление
    public static final RoundingMode BANKING_ROUNDING = RoundingMode.HALF_EVEN;
}
