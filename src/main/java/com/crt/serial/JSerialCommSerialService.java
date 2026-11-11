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

import java.nio.charset.StandardCharsets;
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
    private static final int ACK_READ_TIMEOUT_MS = 60;
    private static final int ACK_TOTAL_TIMEOUT_MS = 600;

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
    public synchronized SerialResult send(Verdict verdict) {
        if (!ensureOpen()) {
            log.warn("串口不可用，跳过发送：{}", verdict);
            return new SerialResult(false, false, "串口不可用");
        }
        drainInput();

        byte[] payload = {(byte) verdict.getCode()};
        int written = port.writeBytes(payload, payload.length);
        if (written != payload.length) {
            log.warn("串口写入不完整（{} / {} 字节）", written, payload.length);
            return new SerialResult(false, false, "写入不完整");
        }

        String reply = readReply();
        boolean acked = reply.contains("ACK:" + verdict.getCode());
        if (acked) {
            log.info("串口发送 '{}'，收到 ACK：{}", verdict.getCode(), reply.trim());
        } else {
            log.warn("串口发送 '{}'，未收到有效 ACK（收到：{}）", verdict.getCode(), reply.trim());
        }
        return new SerialResult(true, acked, reply.trim());
    }

    // 清掉残留字节，避免上一次的尾巴混进这次应答
    private void drainInput() {
        int available = port.bytesAvailable();
        if (available > 0) {
            byte[] stale = new byte[available];
            port.readBytes(stale, stale.length);
        }
    }

    // 循环读到换行或总超时（readBytes 一有数据就返回，单次读可能拆帧）
    private String readReply() {
        long deadline = System.currentTimeMillis() + ACK_TOTAL_TIMEOUT_MS;
        StringBuilder sb = new StringBuilder();
        byte[] buf = new byte[16];
        while (System.currentTimeMillis() < deadline) {
            int n = port.readBytes(buf, buf.length);
            if (n > 0) {
                sb.append(new String(buf, 0, n, StandardCharsets.UTF_8));
                if (sb.indexOf("\n") >= 0) {
                    break;
                }
            }
        }
        return sb.toString();
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
        // 读超时用于等 ACK；写超时 1s
        candidate.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, ACK_READ_TIMEOUT_MS, 1000);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
