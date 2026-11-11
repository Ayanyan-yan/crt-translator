package com.crt.web;

// 判定响应：结论、串口字符、提示语、AI 原文、是否已下发
public record JudgeResponse(String verdict, char code, String message, String raw, boolean serialSent) {
}
