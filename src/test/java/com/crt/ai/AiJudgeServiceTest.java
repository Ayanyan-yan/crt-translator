package com.crt.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiJudgeServiceTest {

    @Test
    @DisplayName("把 AI 返回的 JSON 解析成 OK")
    void judgeParsesOk() {
        AiClient fake = (systemPrompt, userText) -> "{\"verdict\":\"OK\",\"reason\":\"可行\"}";
        AiJudgeService service = new AiJudgeService(fake);

        JudgeResult result = service.judge("给登录页加一个深色模式");

        assertEquals(Verdict.OK, result.verdict());
        assertEquals("{\"verdict\":\"OK\",\"reason\":\"可行\"}", result.raw());
    }

    @Test
    @DisplayName("把 AI 返回的文本解析成 ABSURD")
    void judgeParsesAbsurd() {
        AiJudgeService service = new AiJudgeService((s, u) -> "需求太离谱，一天写完淘宝是不可能的");

        assertEquals(Verdict.ABSURD, service.judge("一天写完淘宝").verdict());
    }

    @Test
    @DisplayName("系统提示词要求输出 JSON，并包含判定标准")
    void systemPromptIsWellFormed() {
        AtomicReference<String> systemPrompt = new AtomicReference<>();
        AiJudgeService service = new AiJudgeService((s, u) -> {
            systemPrompt.set(s);
            return "{\"verdict\":\"OK\"}";
        });

        service.judge("随便一个需求");

        assertTrue(systemPrompt.get().contains("JSON"));
        assertTrue(systemPrompt.get().contains("离谱"));
        assertTrue(systemPrompt.get().contains("ABSURD"));
    }

    @Test
    @DisplayName("用户输入原样传给模型")
    void userTextIsPassedThrough() {
        AtomicReference<String> userText = new AtomicReference<>();
        AiJudgeService service = new AiJudgeService((s, u) -> {
            userText.set(u);
            return "{\"verdict\":\"OK\"}";
        });

        service.judge("把首页改成抖音风格");

        assertEquals("把首页改成抖音风格", userText.get());
    }
}
