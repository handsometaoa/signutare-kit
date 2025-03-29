package org.tao.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "signature")
public class SignatureProperties {

    private String requestIdField = "requestId";
    private String timestampField = "timestamp";
    private String signatureField = "signature";
    private Integer expireMinutes = 5;
    private Map<String, String> secretKeys = new HashMap<>();

    public Map<String, String> getSecretKeys() {
        return secretKeys;
    }

    public void setSecretKeys(Map<String, String> secretKeys) {
        this.secretKeys = secretKeys;
    }

    public Integer getExpireMinutes() {
        return expireMinutes;
    }

    public void setExpireMinutes(Integer expireMinutes) {
        this.expireMinutes = expireMinutes;
    }

    public String getRequestIdField() {
        return requestIdField;
    }

    public void setRequestIdField(String requestIdField) {
        this.requestIdField = requestIdField;
    }

    public String getSignatureField() {
        return signatureField;
    }

    public void setSignatureField(String signatureField) {
        this.signatureField = signatureField;
    }

    public String getTimestampField() {
        return timestampField;
    }

    public void setTimestampField(String timestampField) {
        this.timestampField = timestampField;
    }
}
