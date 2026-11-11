package com.crt.web;

import jakarta.validation.constraints.Size;

// 判定请求体：{"text":"需求内容"}，长度上限 2000 字
public record JudgeRequest(
        @Size(max = 2000, message = "需求内容过长，最多 2000 字")
        String text) {
}
