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
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JudgeController.class)
class JudgeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiJudgeService aiJudgeService;

    @MockBean
    private SerialPortService serialPortService;

    @Test
    @DisplayName("合理的需求 → verdict=OK，串口指令 'O'")
    void judgeOk() throws Exception {
        when(aiJudgeService.judge("加个深色模式"))
                .thenReturn(new JudgeResult(Verdict.OK, "{\"verdict\":\"OK\"}"));
        when(serialPortService.send(Verdict.OK)).thenReturn(true);

        mockMvc.perform(post("/api/judge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"加个深色模式\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("OK"))
                .andExpect(jsonPath("$.code").value("O"))
                .andExpect(jsonPath("$.serialSent").value(true));
    }

    @Test
    @DisplayName("离谱的需求 → verdict=ABSURD，串口指令 'E'")
    void judgeAbsurd() throws Exception {
        when(aiJudgeService.judge(anyString()))
                .thenReturn(new JudgeResult(Verdict.ABSURD, "{\"verdict\":\"ABSURD\"}"));
        when(serialPortService.send(Verdict.ABSURD)).thenReturn(true);

        mockMvc.perform(post("/api/judge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"一天写完淘宝\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("ABSURD"))
                .andExpect(jsonPath("$.code").value("E"))
                .andExpect(jsonPath("$.message").value("极度离谱，拒绝接入"));
    }

    @Test
    @DisplayName("空白需求 → 400 + 中文提示")
    void blankTextIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/judge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("需求内容不能为空"));
    }

    @Test
    @DisplayName("AI 调用失败（如未配置 Key）→ 502 + 提示")
    void aiFailureReturnsBadGateway() throws Exception {
        when(aiJudgeService.judge(anyString()))
                .thenThrow(new IllegalStateException("未配置 DEEPSEEK_API_KEY，请先设置环境变量再启动"));

        mockMvc.perform(post("/api/judge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"一天写完淘宝\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("未配置 DEEPSEEK_API_KEY，请先设置环境变量再启动"));
    }

    @Test
    @DisplayName("GET /api/status 返回串口状态")
    void statusReflectsSerial() throws Exception {
        when(serialPortService.isOpen()).thenReturn(true);

        mockMvc.perform(get("/api/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serialOpen").value(true));
    }
}
