package com.example.photobackend;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class PricingService {

    // Тарифы
    public static final int RATE_PORTRAIT = 1000; // руб/час
    public static final int RATE_EVENT = 2000;    // руб/час

    // Дополнительные услуги
    public static final int ADDON_EXPRESS_PRICE = 1000; // Готовность за 24ч
    public static final int ADDON_STUDIO_PRICE = 500;   // Помощь с арендой студии

    // Единый источник правды для прайс-листа (Single Source of Truth)
    public Map<String, Object> getPricingConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("portraitRate", RATE_PORTRAIT);
        config.put("eventRate", RATE_EVENT);
        config.put("addonExpressPrice", ADDON_EXPRESS_PRICE);
        config.put("addonStudioPrice", ADDON_STUDIO_PRICE);
        return config;
    }

    public int calculatePrice(Integer shootTypeRate, Integer hours, boolean addonExpress, boolean addonStudio, String promoCode) {
        int validHours = (hours == null || hours < 1) ? 2 : Math.min(hours, 24);

        int rate = RATE_PORTRAIT;
        if (shootTypeRate != null && shootTypeRate >= RATE_EVENT) {
            rate = RATE_EVENT;
        }

        int total = rate * validHours;

        if (addonExpress) {
            total += ADDON_EXPRESS_PRICE;
        }
        if (addonStudio) {
            total += ADDON_STUDIO_PRICE;
        }

        if (promoCode != null) {
            String code = promoCode.trim().toUpperCase();
            if ("START10".equals(code)) {
                total = (int) Math.round(total * 0.9);
            } else if ("FOTO500".equals(code)) {
                total = Math.max(0, total - 500);
            }
        }

        return total;
    }
}
