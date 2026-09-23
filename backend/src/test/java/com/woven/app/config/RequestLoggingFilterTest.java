package com.woven.app.config;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(OutputCaptureExtension.class)
class RequestLoggingFilterTest {
    @Test
    void correlatesResponseWithoutLoggingSecrets(CapturedOutput output) throws Exception {
        var request = new MockHttpServletRequest("POST", "/auth/login");
        request.addHeader("Authorization", "Bearer confidential-token");
        request.addHeader("X-Request-ID", "untrusted-client-value");
        request.setQueryString("password=confidential-query");
        request.setContent("confidential-body".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var response = new MockHttpServletResponse();
        new RequestLoggingFilter().doFilter(request, response, (req, res) -> {
            assertEquals(response.getHeader("X-Request-ID"), MDC.get("requestId"));
            response.setStatus(401);
        });
        assertDoesNotThrow(() -> java.util.UUID.fromString(response.getHeader("X-Request-ID")));
        assertNull(MDC.get("requestId"));
        assertTrue(output.getOut().contains("status=401"));
        assertFalse(output.getAll().contains("confidential"));
        assertFalse(output.getAll().contains("untrusted-client-value"));
    }

    @Test
    void restoresLoggingContextWhenARequestFails(CapturedOutput output) {
        MDC.put("requestId", "outer-context");
        try {
            assertThrows(ServletException.class, () -> new RequestLoggingFilter().doFilter(
                    new MockHttpServletRequest("GET", "/sops"), new MockHttpServletResponse(),
                    (req, res) -> { throw new ServletException("confidential-exception"); }));
            assertEquals("outer-context", MDC.get("requestId"));
            assertTrue(output.getOut().contains("outcome=exception"));
            assertFalse(output.getAll().contains("confidential-exception"));
        } finally {
            MDC.remove("requestId");
        }
    }
}
