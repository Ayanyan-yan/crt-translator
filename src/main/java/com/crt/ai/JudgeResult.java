package com.crt.ai;

// 一次判定的结果：结论 + AI 原始输出
public record JudgeResult(Verdict verdict, String raw) {
}
