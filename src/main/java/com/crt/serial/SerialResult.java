package com.crt.serial;

// 一次下发的执行结果：是否写出、是否收到下位机 ACK、原始应答
public record SerialResult(boolean sent, boolean acked, String detail) {
}
