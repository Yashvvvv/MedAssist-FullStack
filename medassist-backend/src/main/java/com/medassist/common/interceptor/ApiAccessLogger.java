package com.medassist.common.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ApiAccessLogger implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(ApiAccessLogger.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        long startTime = System.currentTimeMillis();
        request.setAttribute("startTime", startTime);

        // Add request information to MDC for logging
        String traceId = generateTraceId();
        MDC.put("traceId", traceId);
        MDC.put("method", request.getMethod());
        MDC.put("endpoint", request.getRequestURI());
        MDC.put("userAgent", request.getHeader("User-Agent"));
        MDC.put("clientIp", getClientIpAddress(request));

        // Add user information if authenticated
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            MDC.put("userId", auth.getName());
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                               Object handler, Exception ex) {

        try {
            long startTime = (Long) request.getAttribute("startTime");
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            MDC.put("duration", String.valueOf(duration));
            MDC.put("statusCode", String.valueOf(response.getStatus()));

            // Log API access
            logger.info("API Access: {} {} - Status: {} - Duration: {}ms - User: {} - IP: {}",
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                duration,
                MDC.get("userId"),
                MDC.get("clientIp"));

        } finally {
            // Clean up MDC
            MDC.clear();
        }
    }

    private String generateTraceId() {
        return java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }
}
