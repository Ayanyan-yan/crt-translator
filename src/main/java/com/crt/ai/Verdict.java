package com.crt.ai;

// 判定结果，code 直接作为串口字符下发（O=合理，E=离谱）
public enum Verdict {

    OK('O', "需求通过"),
    ABSURD('E', "极度离谱，拒绝接入"),
    UNKNOWN('?', "无法判定");

    private final char code;
    private final String message;

    Verdict(char code, String message) {
        this.code = code;
        this.message = message;
    }

    public char getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
