package com.crt.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// crt.* 配置项
@ConfigurationProperties(prefix = "crt")
public class CrtProperties {

    private final Ai ai = new Ai();
    private final Serial serial = new Serial();

    public Ai getAi() {
        return ai;
    }

    public Serial getSerial() {
        return serial;
    }

    // AI 接口配置（默认 DeepSeek）
    public static class Ai {
        private String baseUrl = "https://api.deepseek.com";
        private String apiKey = "";
        private String model = "deepseek-chat";
        private int timeoutSeconds = 30;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }
    }

    // 串口配置
    public static class Serial {
        private boolean enabled = false;
        private String port = "auto";
        private int baudRate = 9600;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getPort() {
            return port;
        }

        public void setPort(String port) {
            this.port = port;
        }

        public int getBaudRate() {
            return baudRate;
        }

        public void setBaudRate(int baudRate) {
            this.baudRate = baudRate;
        }
    }
}
