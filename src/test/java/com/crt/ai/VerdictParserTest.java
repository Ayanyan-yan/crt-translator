package com.crt.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VerdictParserTest {

    @Test
    @DisplayName("纯 JSON：ABSURD")
    void parsePlainJsonAbsurd() {
        assertEquals(Verdict.ABSURD,
                VerdictParser.parse("{\"verdict\":\"ABSURD\",\"reason\":\"一天写完淘宝\"}"));
    }

    @Test
    @DisplayName("纯 JSON：OK")
    void parsePlainJsonOk() {
        assertEquals(Verdict.OK,
                VerdictParser.parse("{\"verdict\":\"OK\",\"reason\":\"工作量合理\"}"));
    }

    @Test
    @DisplayName("带 ```json 代码块外壳也能解析")
    void parseFencedJson() {
        String content = """
                ```json
                {"verdict":"ABSURD","reason":"违背物理规律"}
                ```
                """;
        assertEquals(Verdict.ABSURD, VerdictParser.parse(content));
    }

    @Test
    @DisplayName("中文关键词：离谱 → ABSURD")
    void parseChineseAbsurdKeyword() {
        assertEquals(Verdict.ABSURD, VerdictParser.parse("这个需求太离谱了，根本做不了"));
    }

    @Test
    @DisplayName("中文关键词：不合理 不能被 合理 误判")
    void chineseNegationWins() {
        assertEquals(Verdict.ABSURD, VerdictParser.parse("这个需求不合理"));
    }

    @Test
    @DisplayName("中文关键词：合理 → OK")
    void parseChineseOkKeyword() {
        assertEquals(Verdict.OK, VerdictParser.parse("合理，可以做"));
    }

    @Test
    @DisplayName("空内容 / null → UNKNOWN")
    void blankOrNullIsUnknown() {
        assertEquals(Verdict.UNKNOWN, VerdictParser.parse(""));
        assertEquals(Verdict.UNKNOWN, VerdictParser.parse("   "));
        assertEquals(Verdict.UNKNOWN, VerdictParser.parse(null));
    }

    @Test
    @DisplayName("JSON 里 verdict 无法识别 → UNKNOWN")
    void unknownWordIsUnknown() {
        assertEquals(Verdict.UNKNOWN, VerdictParser.parse("{\"verdict\":\"WHATEVER\"}"));
    }

    @Test
    @DisplayName("串口指令映射：OK→'O'，ABSURD→'E'")
    void verdictCodes() {
        assertEquals('O', Verdict.OK.getCode());
        assertEquals('E', Verdict.ABSURD.getCode());
    }
}
