package com.crt.serial;

import com.crt.ai.Verdict;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoggingSerialServiceTest {

    @Test
    @DisplayName("模拟串口：发送 OK 成功并返回 ACK")
    void sendOk() {
        SerialResult result = new LoggingSerialService().send(Verdict.OK);
        assertTrue(result.sent());
        assertTrue(result.acked());
        assertEquals("ACK:O", result.detail());
    }

    @Test
    @DisplayName("模拟串口：发送 ABSURD 成功并返回 ACK")
    void sendAbsurd() {
        SerialResult result = new LoggingSerialService().send(Verdict.ABSURD);
        assertTrue(result.sent());
        assertTrue(result.acked());
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
