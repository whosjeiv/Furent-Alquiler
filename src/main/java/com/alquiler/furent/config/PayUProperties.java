package com.alquiler.furent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "furent.payu")
public class PayUProperties {
    private String apiKey;
    private String merchantId;
    private String accountId;
    private String url;
    private String currency = "COP";
    private String test = "0";

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && merchantId != null && !merchantId.isBlank();
    }

    // Getters and Setters
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getTest() { return test; }
    public void setTest(String test) { this.test = test; }
}
