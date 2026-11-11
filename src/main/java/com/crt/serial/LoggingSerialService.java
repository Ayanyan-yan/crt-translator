package com.crt.serial;

import com.crt.ai.Verdict;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

// 没接硬件时的模拟串口：只打日志，视为 ACK 成功
@Service
@ConditionalOnProperty(prefix = "crt.serial", name = "enabled", havingValue = "false", matchIfMissing = true)
public class LoggingSerialService implements SerialPortService {

    private static final Logger log = LoggerFactory.getLogger(LoggingSerialService.class);

    @Override
    public SerialResult send(Verdict verdict) {
        log.info("[模拟串口] 发送 '{}' → {}（模拟 ACK）", verdict.getCode(), verdict.getMessage());
        return new SerialResult(true, true, "ACK:" + verdict.getCode());
    }

    @Override
    public boolean isOpen() {
        return true;
    }
}
