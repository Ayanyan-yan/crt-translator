package com.crt.config;

import com.crt.ai.AiClient;
import com.crt.ai.DeepSeekClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    public AiClient aiClient(CrtProperties properties) {
        return new DeepSeekClient(properties);
    }
}
