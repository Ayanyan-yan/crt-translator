package com.crt.ai;

import com.crt.config.CrtProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 用本地 HttpServer 模拟 DeepSeek 接口，验证请求体与响应解析（不联网）
class DeepSeekClientTest {

    @Test
    @DisplayName("发送请求并提取 choices[0].message.content")
    void sendsRequestAndExtractsContent() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String body = "{\"choices\":[{\"message\":{\"content\":\"{\\\"verdict\\\":\\\"OK\\\"}\"}}]}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.start();

        try {
            CrtProperties properties = new CrtProperties();
            properties.getAi().setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
            properties.getAi().setApiKey("test-key");
            properties.getAi().setModel("deepseek-chat");

            DeepSeekClient client = new DeepSeekClient(properties);
            String content = client.chat("你是评审专家", "一天写完淘宝");

            assertEquals("{\"verdict\":\"OK\"}", content);
            assertTrue(requestBody.get().contains("\"model\":\"deepseek-chat\""));
            assertTrue(requestBody.get().contains("你是评审专家"));
            assertTrue(requestBody.get().contains("一天写完淘宝"));
            assertTrue(requestBody.get().contains("\"role\":\"system\""));
            assertTrue(requestBody.get().contains("\"role\":\"user\""));
        } finally {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("AI 返回非 200 时抛出异常")
    void non200Throws() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", exchange -> {
            byte[] bytes = "{\"error\":\"invalid api key\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(401, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.start();

        try {
            CrtProperties properties = new CrtProperties();
            properties.getAi().setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
            properties.getAi().setApiKey("bad-key");

            DeepSeekClient client = new DeepSeekClient(properties);
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> client.chat("s", "u"));
            assertTrue(ex.getMessage().contains("401"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("响应缺少 choices 时返回空串而不是崩溃")
    void missingChoicesReturnsEmpty() {
        assertEquals("", DeepSeekClient.extractContent("{}"));
    }
}
