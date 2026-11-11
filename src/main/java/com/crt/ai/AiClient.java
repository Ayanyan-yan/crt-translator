package com.crt.ai;

// 大模型客户端抽象，测试时可替换成假实现
public interface AiClient {

    String chat(String systemPrompt, String userText);
}
