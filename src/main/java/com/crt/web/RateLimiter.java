package com.crt.web;

import java.util.concurrent.ConcurrentHashMap;

// 固定窗口限流：同一 key 在 windowMillis 内最多 limit 次
public class RateLimiter {

    private final int limit;
    private final long windowMillis;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimiter(int limit, long windowMillis) {
        this.limit = Math.max(1, limit);
        this.windowMillis = Math.max(1L, windowMillis);
    }

    public boolean tryAcquire(String key) {
        long now = System.currentTimeMillis();
        Window window = windows.compute(key, (k, old) -> {
            if (old == null || now - old.start >= windowMillis) {
                return new Window(now);
            }
            old.count++;
            return old;
        });
        return window.count <= limit;
    }

    // 距离窗口重置还有几秒（用于 Retry-After）
    public long retryAfterSeconds(String key) {
        Window window = windows.get(key);
        if (window == null) {
            return 0L;
        }
        long remain = windowMillis - (System.currentTimeMillis() - window.start);
        return remain <= 0 ? 0L : (remain + 999L) / 1000L;
    }

    private static final class Window {
        private final long start;
        private int count = 1;

        private Window(long start) {
            this.start = start;
        }
    }
}
