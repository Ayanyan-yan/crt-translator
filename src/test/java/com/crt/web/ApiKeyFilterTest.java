package com.crt.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ApiKeyFilterTest {

    @Test
    void passesWhenKeyNotConfigured() throws Exception {
        ApiKeyFilter filter = new ApiKeyFilter("");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(new MockHttpServletRequest("POST", "/api/judge"),
                new MockHttpServletResponse(), chain);

        assertNotNull(chain.getRequest());
    }

    @Test
    void rejectsWhenHeaderMissing() throws Exception {
        ApiKeyFilter filter = new ApiKeyFilter("secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(new MockHttpServletRequest("POST", "/api/judge"), response, chain);

        assertEquals(401, response.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void rejectsWhenHeaderWrong() throws Exception {
        ApiKeyFilter filter = new ApiKeyFilter("secret");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/judge");
        request.addHeader("X-API-Key", "wrong");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void acceptsCorrectKey() throws Exception {
        ApiKeyFilter filter = new ApiKeyFilter("secret");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/judge");
        request.addHeader("X-API-Key", "secret");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertNotNull(chain.getRequest());
    }
}
