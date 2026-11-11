package com.crt.ai;

import com.crt.config.CrtProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

// DeepSeek（OpenAI 兼容）客户端：POST {baseUrl}/chat/completions
public class DeepSeekClient implements AiClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String CHAT_PATH = "/chat/completions";

    private final CrtProperties properties;
    private final HttpClient httpClient;

    public DeepSeekClient(CrtProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String chat(String systemPrompt, String userText) {
        if (properties.getAi().getApiKey() == null || properties.getAi().getApiKey().isBlank()) {
            throw new IllegalStateException("未配置 DEEPSEEK_API_KEY，请先设置环境变量再启动");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(trimTrailingSlash(properties.getAi().getBaseUrl()) + CHAT_PATH))
                .timeout(Duration.ofSeconds(properties.getAi().getTimeoutSeconds()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + properties.getAi().getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(
                        buildRequestBody(properties.getAi().getModel(), systemPrompt, userText)))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException(
                        "AI 接口返回 HTTP " + response.statusCode() + "：" + response.body());
            }
            return extractContent(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("调用 AI 被中断", e);
        } catch (IOException e) {
            throw new IllegalStateException("调用 AI 失败：" + e.getMessage(), e);
        }
    }

    static String buildRequestBody(String model, String systemPrompt, String userText) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("model", model);
        root.put("temperature", 0.2);
        ArrayNode messages = root.putArray("messages");
        messages.addObject().put("role", "system").put("content", systemPrompt);
        messages.addObject().put("role", "user").put("content", userText);
        try {
            return MAPPER.writeValueAsString(root);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    // 取 choices[0].message.content
    static String extractContent(String responseJson) {
        try {
            JsonNode root = MAPPER.readTree(responseJson);
            return root.path("choices").path(0).path("message").path("content").asText("");
        } catch (IOException e) {
            throw new IllegalStateException("解析 AI 响应失败：" + e.getMessage(), e);
        }
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "https://api.deepseek.com";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
