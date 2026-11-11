package com.crt.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Locale;

// 把 AI 返回的 JSON / 文本解析成 Verdict
public final class VerdictParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private VerdictParser() {
    }

    public static Verdict parse(String aiContent) {
        if (aiContent == null) {
            return Verdict.UNKNOWN;
        }
        String text = stripCodeFence(aiContent.trim());
        if (text.isEmpty()) {
            return Verdict.UNKNOWN;
        }
        Verdict fromJson = parseJson(text);
        return fromJson != Verdict.UNKNOWN ? fromJson : parseKeywords(text);
    }

    // 去掉 ```json ... ``` 外壳
    static String stripCodeFence(String text) {
        String t = text.trim();
        if (!t.startsWith("```")) {
            return t;
        }
        int firstNewline = t.indexOf('\n');
        if (firstNewline < 0) {
            return t;
        }
        String body = t.substring(firstNewline + 1);
        int end = body.lastIndexOf("```");
        return (end >= 0 ? body.substring(0, end) : body).trim();
    }

    private static Verdict parseJson(String text) {
        try {
            JsonNode node = MAPPER.readTree(text);
            if (node == null || !node.isObject()) {
                return Verdict.UNKNOWN;
            }
            String word = node.path("verdict").asText("");
            if (word.isBlank()) {
                word = node.path("result").asText("");
            }
            return fromWord(word);
        } catch (Exception e) {
            return Verdict.UNKNOWN;
        }
    }

    // 否定词优先，避免"不合理"被"合理"误判
    private static Verdict parseKeywords(String text) {
        String upper = text.toUpperCase(Locale.ROOT);
        if (text.contains("不合理") || text.contains("离谱") || text.contains("不可能")
                || upper.contains("ABSURD") || upper.contains("REJECT") || upper.contains("ERROR")) {
            return Verdict.ABSURD;
        }
        if (text.contains("合理") || text.contains("可行")
                || upper.contains("OK") || upper.contains("PASS") || upper.contains("APPROVE")) {
            return Verdict.OK;
        }
        return Verdict.UNKNOWN;
    }

    static Verdict fromWord(String word) {
        if (word == null) {
            return Verdict.UNKNOWN;
        }
        String w = word.trim().toUpperCase(Locale.ROOT);
        return switch (w) {
            case "OK", "PASS", "APPROVE", "REASONABLE", "合理", "通过" -> Verdict.OK;
            case "ABSURD", "ERROR", "REJECT", "UNREASONABLE", "离谱", "不合理" -> Verdict.ABSURD;
            default -> Verdict.UNKNOWN;
        };
    }
}
