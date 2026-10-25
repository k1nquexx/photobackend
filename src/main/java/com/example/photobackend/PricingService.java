package com.example.photobackend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Service
public class PricingService {

    public static final BigDecimal RATE_PORTRAIT = PricingConstants.RATE_PORTRAIT;
    public static final BigDecimal RATE_EVENT = PricingConstants.RATE_EVENT;
    public static final BigDecimal ADDON_EXPRESS_PRICE = PricingConstants.ADDON_EXPRESS_PRICE;
    public static final BigDecimal ADDON_STUDIO_PRICE = PricingConstants.ADDON_STUDIO_PRICE;

    private final AppConfigProperties appConfigProperties;

    @Autowired
    public PricingService(AppConfigProperties appConfigProperties) {
        this.appConfigProperties = appConfigProperties;
    }

    public Map<String, Object> getPricingConfig() {
        Map<String, Object> config = new HashMap<>();
        BigDecimal portrait = appConfigProperties != null ? appConfigProperties.getPricing().getRatePortrait() : RATE_PORTRAIT;
        BigDecimal event = appConfigProperties != null ? appConfigProperties.getPricing().getRateEvent() : RATE_EVENT;
        BigDecimal express = appConfigProperties != null ? appConfigProperties.getPricing().getAddonExpress() : ADDON_EXPRESS_PRICE;
        BigDecimal studio = appConfigProperties != null ? appConfigProperties.getPricing().getAddonStudio() : ADDON_STUDIO_PRICE;

        config.put("portraitRate", portrait.setScale(2, PricingConstants.BANKING_ROUNDING));
        config.put("eventRate", event.setScale(2, PricingConstants.BANKING_ROUNDING));
        config.put("addonExpressPrice", express.setScale(2, PricingConstants.BANKING_ROUNDING));
        config.put("addonStudioPrice", studio.setScale(2, PricingConstants.BANKING_ROUNDING));
        return config;
    }

    /**
     * Основной расчет стоимости на BigDecimal с банковским округлением (RoundingMode.HALF_EVEN).
     */
    public BigDecimal calculatePrice(BigDecimal shootTypeRate, Integer hours, boolean addonExpress, boolean addonStudio, String promoCode) {
        int validHours = (hours == null || hours < PricingConstants.MIN_HOURS) 
                ? PricingConstants.DEFAULT_HOURS 
                : Math.min(hours, PricingConstants.MAX_HOURS);

        BigDecimal portraitRate = appConfigProperties != null ? appConfigProperties.getPricing().getRatePortrait() : RATE_PORTRAIT;
        BigDecimal eventRate = appConfigProperties != null ? appConfigProperties.getPricing().getRateEvent() : RATE_EVENT;
        BigDecimal expressPrice = appConfigProperties != null ? appConfigProperties.getPricing().getAddonExpress() : ADDON_EXPRESS_PRICE;
        BigDecimal studioPrice = appConfigProperties != null ? appConfigProperties.getPricing().getAddonStudio() : ADDON_STUDIO_PRICE;

        BigDecimal rate = portraitRate;
        if (shootTypeRate != null && shootTypeRate.compareTo(eventRate) >= 0) {
            rate = eventRate;
        }

        BigDecimal total = rate.multiply(BigDecimal.valueOf(validHours));

        if (addonExpress) {
            total = total.add(expressPrice);
        }
        if (addonStudio) {
            total = total.add(studioPrice);
        }

        if (promoCode != null) {
            String code = promoCode.trim().toUpperCase();
            if (PricingConstants.PROMO_START10.equals(code)) {
                total = total.multiply(PricingConstants.PROMO_START10_DISCOUNT_MULTIPLIER);
            } else if (PricingConstants.PROMO_FOTO500.equals(code)) {
                total = total.subtract(PricingConstants.PROMO_FOTO500_DISCOUNT_RUB).max(BigDecimal.ZERO);
            }
        }

        return total.setScale(2, PricingConstants.BANKING_ROUNDING);
    }

    // Перегруженный метод для совместимости со старыми целочисленными вызовами
    public int calculatePrice(Integer shootTypeRate, Integer hours, boolean addonExpress, boolean addonStudio, String promoCode) {
        BigDecimal rate = shootTypeRate != null ? BigDecimal.valueOf(shootTypeRate) : null;
        BigDecimal result = calculatePrice(rate, hours, addonExpress, addonStudio, promoCode);
        return result.setScale(0, RoundingMode.HALF_EVEN).intValue();
    }
}
