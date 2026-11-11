package com.crt.web;

// 判定响应：结论、串口字符、提示语、AI 原文、是否下发、是否收到 ACK
public record JudgeResponse(String verdict, char code, String message, String raw,
                            boolean serialSent, boolean acknowledged) {
}
