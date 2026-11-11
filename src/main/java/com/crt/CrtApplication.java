package com.crt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// CRT 上位机入口：网页输入 → AI 判定 → 串口下发 'O'/'E'
@SpringBootApplication
@ConfigurationPropertiesScan
public class CrtApplication {

    public static void main(String[] args) {
        SpringApplication.run(CrtApplication.class, args);
    }
}
