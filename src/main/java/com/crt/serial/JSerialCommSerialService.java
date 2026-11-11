package com.crt.serial;

import com.crt.ai.Verdict;
import com.crt.config.CrtProperties;
import com.fazecast.jSerialComm.SerialPort;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

// 真实串口：打开失败自动重试，auto 模式优先选 CH340
@Service
@ConditionalOnProperty(prefix = "crt.serial", name = "enabled", havingValue = "true")
public class JSerialCommSerialService implements SerialPortService {

    private static final Logger log = LoggerFactory.getLogger(JSerialCommSerialService.class);
    private static final int OPEN_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 400L;

    private final CrtProperties properties;
    private SerialPort port;

    public JSerialCommSerialService(CrtProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void open() {
        ensureOpen();
    }

    @PreDestroy
    void close() {
        if (port != null && port.isOpen()) {
            port.closePort();
            log.info("串口已关闭");
        }
    }

    @Override
    public synchronized boolean send(Verdict verdict) {
        if (!ensureOpen()) {
            log.warn("串口不可用，跳过发送：{}", verdict);
            return false;
        }
        byte[] payload = {(byte) verdict.getCode()};
        int written = port.writeBytes(payload, payload.length);
        if (written == payload.length) {
            log.info("串口发送 '{}'（{}）", verdict.getCode(), verdict.getMessage());
            return true;
        }
        log.warn("串口写入不完整（{} / {} 字节）", written, payload.length);
        return false;
    }

    @Override
    public boolean isOpen() {
        return port != null && port.isOpen();
    }

    private synchronized boolean ensureOpen() {
        if (port != null && port.isOpen()) {
            return true;
        }
        for (int attempt = 1; attempt <= OPEN_RETRIES; attempt++) {
            SerialPort candidate = pickPort();
            if (candidate == null) {
                log.warn("没有检测到串口：请插好 USB-TTL（CH340）并确认驱动");
                return false;
            }
            configure(candidate);
            if (candidate.openPort()) {
                port = candidate;
                log.info("串口已打开：{} @ {} bps",
                        candidate.getSystemPortName(), properties.getSerial().getBaudRate());
                return true;
            }
            log.warn("串口打开失败（{}/{}）：{}，稍后重试",
                    attempt, OPEN_RETRIES, candidate.getSystemPortName());
            sleep(RETRY_DELAY_MS);
        }
        port = null;
        return false;
    }

    private SerialPort pickPort() {
        String configured = properties.getSerial().getPort();
        if (configured != null && !configured.isBlank() && !"auto".equalsIgnoreCase(configured)) {
            return SerialPort.getCommPort(configured);
        }
        SerialPort[] available = SerialPort.getCommPorts();
        if (available.length == 0) {
            return null;
        }
        return Arrays.stream(available)
                .sorted(Comparator.comparingInt(JSerialCommSerialService::usbSerialScore).reversed())
                .findFirst()
                .orElse(null);
    }

    // CH340/USB-SERIAL 优先，避免选中蓝牙等虚拟串口
    private static int usbSerialScore(SerialPort p) {
        String desc = (p.getDescriptivePortName() == null ? "" : p.getDescriptivePortName())
                .toUpperCase(Locale.ROOT);
        int score = 0;
        if (desc.contains("CH340") || desc.contains("CH341")) {
            score += 100;
        }
        if (desc.contains("USB-SERIAL") || desc.contains("USB SERIAL")) {
            score += 50;
        }
        if (desc.contains("CP210") || desc.contains("FTDI") || desc.contains("STLINK")) {
            score += 40;
        }
        return score;
    }

    private void configure(SerialPort candidate) {
        candidate.setBaudRate(properties.getSerial().getBaudRate());
        candidate.setNumDataBits(8);
        candidate.setNumStopBits(SerialPort.ONE_STOP_BIT);
        candidate.setParity(SerialPort.NO_PARITY);
        candidate.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 1000, 1000);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
