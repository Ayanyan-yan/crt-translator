package com.crt.ai;

import org.springframework.stereotype.Service;

// 需求判定：拼 Prompt → 调模型 → 解析结论
@Service
public class AiJudgeService {

    private static final String SYSTEM_PROMPT = """
            你是一名资深软件工程评审专家，负责判断"产品经理的需求"是否离谱。
            判定标准：
            1. 违背物理规律、数学不可能 → ABSURD
            2. 明显超出工程极限或时间成本荒谬（如"一天写完淘宝"）→ ABSURD
            3. 需求自相矛盾、无法实现 → ABSURD
            4. 技术可行、工作量合理 → OK

            只输出一行 JSON，不要输出其他内容：
            {"verdict":"OK 或 ABSURD","reason":"一句话理由"}
            """;

    private final AiClient aiClient;

    public AiJudgeService(AiClient aiClient) {
        this.aiClient = aiClient;
    }

    public JudgeResult judge(String requirement) {
        String raw = aiClient.chat(SYSTEM_PROMPT, requirement);
        return new JudgeResult(VerdictParser.parse(raw), raw);
    }
}
