package com.crt;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// 容器启动测试：关掉串口、清空 Key，避免占用硬件
@SpringBootTest(properties = {
        "crt.serial.enabled=false",
        "crt.ai.api-key="
})
class CrtApplicationTests {

    @Test
    void contextLoads() {
    }
}
