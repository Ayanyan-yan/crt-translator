package com.crt.serial;

import com.crt.ai.Verdict;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoggingSerialServiceTest {

    @Test
    @DisplayName("模拟串口：发送 OK 返回成功")
    void sendOk() {
        SerialPortService service = new LoggingSerialService();
        assertTrue(service.send(Verdict.OK));
    }

    @Test
    @DisplayName("模拟串口：发送 ABSURD 返回成功")
    void sendAbsurd() {
        SerialPortService service = new LoggingSerialService();
        assertTrue(service.send(Verdict.ABSURD));
    }

    @Test
    @DisplayName("模拟串口始终视为已连接")
    void alwaysOpen() {
        assertTrue(new LoggingSerialService().isOpen());
    }

    @Test
    @DisplayName("协议字符：OK='O'，ABSURD='E'")
    void protocolCharacters() {
        assertEquals('O', Verdict.OK.getCode());
        assertEquals('E', Verdict.ABSURD.getCode());
    }
}
