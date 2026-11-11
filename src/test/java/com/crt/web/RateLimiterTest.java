package com.crt.web;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimiterTest {

    @Test
    void allowsUpToLimitThenBlocks() {
        RateLimiter limiter = new RateLimiter(3, 60_000L);

        assertTrue(limiter.tryAcquire("a"));
        assertTrue(limiter.tryAcquire("a"));
        assertTrue(limiter.tryAcquire("a"));
        assertFalse(limiter.tryAcquire("a"));

        // 不同 key 互不影响
        assertTrue(limiter.tryAcquire("b"));
    }

    @Test
    void resetsAfterWindow() throws Exception {
        RateLimiter limiter = new RateLimiter(1, 100L);

        assertTrue(limiter.tryAcquire("a"));
        assertFalse(limiter.tryAcquire("a"));

        Thread.sleep(150L);
        assertTrue(limiter.tryAcquire("a"));
    }

    @Test
    void retryAfterIsPositiveWhenBlocked() {
        RateLimiter limiter = new RateLimiter(1, 60_000L);
        limiter.tryAcquire("a");
        limiter.tryAcquire("a");

        assertTrue(limiter.retryAfterSeconds("a") > 0);
    }
}
