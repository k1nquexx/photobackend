package com.example.photobackend;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class PricingService {

    public static final int RATE_PORTRAIT = PricingConstants.RATE_PORTRAIT;
    public static final int RATE_EVENT = PricingConstants.RATE_EVENT;
    public static final int ADDON_EXPRESS_PRICE = PricingConstants.ADDON_EXPRESS_PRICE;
    public static final int ADDON_STUDIO_PRICE = PricingConstants.ADDON_STUDIO_PRICE;

    // Единый источник правды для прайс-листа (Single Source of Truth)
    public Map<String, Object> getPricingConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("portraitRate", PricingConstants.RATE_PORTRAIT);
        config.put("eventRate", PricingConstants.RATE_EVENT);
        config.put("addonExpressPrice", PricingConstants.ADDON_EXPRESS_PRICE);
        config.put("addonStudioPrice", PricingConstants.ADDON_STUDIO_PRICE);
        return config;
    }

    public int calculatePrice(Integer shootTypeRate, Integer hours, boolean addonExpress, boolean addonStudio, String promoCode) {
        int validHours = (hours == null || hours < PricingConstants.MIN_HOURS) 
                ? PricingConstants.DEFAULT_HOURS 
                : Math.min(hours, PricingConstants.MAX_HOURS);

        int rate = PricingConstants.RATE_PORTRAIT;
        if (shootTypeRate != null && shootTypeRate >= PricingConstants.RATE_EVENT) {
            rate = PricingConstants.RATE_EVENT;
        }

        int total = rate * validHours;

        if (addonExpress) {
            total += PricingConstants.ADDON_EXPRESS_PRICE;
        }
        if (addonStudio) {
            total += PricingConstants.ADDON_STUDIO_PRICE;
        }

        if (promoCode != null) {
            String code = promoCode.trim().toUpperCase();
            if (PricingConstants.PROMO_START10.equals(code)) {
                total = (int) Math.round(total * PricingConstants.PROMO_START10_DISCOUNT_MULTIPLIER);
            } else if (PricingConstants.PROMO_FOTO500.equals(code)) {
                total = Math.max(0, total - PricingConstants.PROMO_FOTO500_DISCOUNT_RUB);
            }
        }

        return total;
    }
}
