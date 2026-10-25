package com.example.photobackend;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfigProperties {

    private List<String> corsAllowedOrigins = List.of("http://localhost:8080", "https://photographer.ru");

    private Pricing pricing = new Pricing();
    private Security security = new Security();

    public static class Pricing {
        private BigDecimal ratePortrait = new BigDecimal("1000.00");
        private BigDecimal rateEvent = new BigDecimal("2000.00");
        private BigDecimal addonExpress = new BigDecimal("1000.00");
        private BigDecimal addonStudio = new BigDecimal("500.00");

        public BigDecimal getRatePortrait() { return ratePortrait; }
        public void setRatePortrait(BigDecimal ratePortrait) { this.ratePortrait = ratePortrait; }

        public BigDecimal getRateEvent() { return rateEvent; }
        public void setRateEvent(BigDecimal rateEvent) { this.rateEvent = rateEvent; }

        public BigDecimal getAddonExpress() { return addonExpress; }
        public void setAddonExpress(BigDecimal addonExpress) { this.addonExpress = addonExpress; }

        public BigDecimal getAddonStudio() { return addonStudio; }
        public void setAddonStudio(BigDecimal addonStudio) { this.addonStudio = addonStudio; }
    }

    public static class Security {
        private long tokenTtlSeconds = 7200;
        private int loginMaxRequests = 5;
        private long loginWindowSeconds = 60;
        private int statusMaxRequests = 10;
        private long statusWindowSeconds = 60;

        public long getTokenTtlSeconds() { return tokenTtlSeconds; }
        public void setTokenTtlSeconds(long tokenTtlSeconds) { this.tokenTtlSeconds = tokenTtlSeconds; }

        public int getLoginMaxRequests() { return loginMaxRequests; }
        public void setLoginMaxRequests(int loginMaxRequests) { this.loginMaxRequests = loginMaxRequests; }

        public long getLoginWindowSeconds() { return loginWindowSeconds; }
        public void setLoginWindowSeconds(long loginWindowSeconds) { this.loginWindowSeconds = loginWindowSeconds; }

        public int getStatusMaxRequests() { return statusMaxRequests; }
        public void setStatusMaxRequests(int statusMaxRequests) { this.statusMaxRequests = statusMaxRequests; }

        public long getStatusWindowSeconds() { return statusWindowSeconds; }
        public void setStatusWindowSeconds(long statusWindowSeconds) { this.statusWindowSeconds = statusWindowSeconds; }
    }

    public List<String> getCorsAllowedOrigins() { return corsAllowedOrigins; }
    public void setCorsAllowedOrigins(List<String> corsAllowedOrigins) { this.corsAllowedOrigins = corsAllowedOrigins; }

    public Pricing getPricing() { return pricing; }
    public void setPricing(Pricing pricing) { this.pricing = pricing; }

    public Security getSecurity() { return security; }
    public void setSecurity(Security security) { this.security = security; }
}
