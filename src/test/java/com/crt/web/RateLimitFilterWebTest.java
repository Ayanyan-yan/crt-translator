package com.crt.web;

import com.crt.ai.AiJudgeService;
import com.crt.ai.JudgeResult;
import com.crt.ai.Verdict;
import com.crt.serial.SerialPortService;
import com.crt.serial.SerialResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 每个 IP 每分钟只允许 1 次时，第 2 次应返回 429
@WebMvcTest(JudgeController.class)
@TestPropertySource(properties = {
        "crt.limit.requests-per-window=1",
        "crt.limit.window-seconds=60"
})
class RateLimitFilterWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiJudgeService aiJudgeService;

    @MockBean
    private SerialPortService serialPortService;

    @Test
    @DisplayName("超过限流阈值 → 429")
    void secondRequestIsLimited() throws Exception {
        when(aiJudgeService.judge(anyString()))
                .thenReturn(new JudgeResult(Verdict.OK, "{\"verdict\":\"OK\"}"));
        when(serialPortService.send(Verdict.OK)).thenReturn(new SerialResult(true, true, "ACK:O"));

        String body = "{\"text\":\"加个深色模式\"}";

        mockMvc.perform(post("/api/judge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/judge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("请求太频繁，请稍后再试"));
    }
}
