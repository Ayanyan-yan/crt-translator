package com.crt.web;

import com.crt.ai.AiJudgeService;
import com.crt.ai.JudgeResult;
import com.crt.serial.SerialPortService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

// 对接口：POST /api/judge 判定并下发；GET /api/status 串口状态
@RestController
@RequestMapping("/api")
public class JudgeController {

    private final AiJudgeService aiJudgeService;
    private final SerialPortService serialPortService;

    @Value("${crt.limit.max-text-length:2000}")
    private int maxTextLength;

    public JudgeController(AiJudgeService aiJudgeService, SerialPortService serialPortService) {
        this.aiJudgeService = aiJudgeService;
        this.serialPortService = serialPortService;
    }

    @PostMapping("/judge")
    public JudgeResponse judge(@Valid @RequestBody(required = false) JudgeRequest request) {
        if (request == null || request.text() == null || request.text().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "需求内容不能为空");
        }
        String text = request.text().trim();
        if (text.length() > maxTextLength) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "需求内容过长，最多 " + maxTextLength + " 字");
        }

        JudgeResult result = aiJudgeService.judge(text);
        boolean sent = serialPortService.send(result.verdict());
        return new JudgeResponse(
                result.verdict().name(),
                result.verdict().getCode(),
                result.verdict().getMessage(),
                result.raw(),
                sent);
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of("serialOpen", serialPortService.isOpen());
    }
}
