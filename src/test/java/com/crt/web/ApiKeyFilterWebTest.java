package com.crt.web;

import com.crt.ai.AiJudgeService;
import com.crt.ai.JudgeResult;
import com.crt.ai.Verdict;
import com.crt.serial.SerialPortService;
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

// 配置了 api-key 时，/api/** 必须带 X-API-Key
@WebMvcTest(JudgeController.class)
@TestPropertySource(properties = "crt.security.api-key=secret")
class ApiKeyFilterWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiJudgeService aiJudgeService;

    @MockBean
    private SerialPortService serialPortService;

    @Test
    @DisplayName("不带 X-API-Key → 401")
    void missingKeyReturns401() throws Exception {
        mockMvc.perform(post("/api/judge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"加个深色模式\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("缺少或错误的 X-API-Key"));
    }

    @Test
    @DisplayName("带正确 X-API-Key → 正常判定")
    void correctKeyPasses() throws Exception {
        when(aiJudgeService.judge(anyString()))
                .thenReturn(new JudgeResult(Verdict.OK, "{\"verdict\":\"OK\"}"));
        when(serialPortService.send(Verdict.OK)).thenReturn(true);

        mockMvc.perform(post("/api/judge")
                        .header("X-API-Key", "secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"加个深色模式\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("OK"));
    }
}
