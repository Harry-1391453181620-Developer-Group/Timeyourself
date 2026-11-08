package com.timeyourself.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

/**
 * 本应用只监听 127.0.0.1，但浏览器仍可能被恶意页面利用 DNS rebinding 访问本机服务。
 * 这里只接受 loopback 的 Host 头，其它 Host 一律 403。
 *
 * 如需通过 SSH 隧道之外的域名/主机名访问，可设置
 * timeyourself.security.host-guard=false（不推荐）。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class LocalHostGuardFilter extends OncePerRequestFilter {

    private static final Set<String> LOOPBACK_HOSTS = Set.of(
            "localhost",
            "127.0.0.1",
            "::1",
            "0:0:0:0:0:0:0:1"
    );

    private final boolean enabled;

    public LocalHostGuardFilter(
            @Value("${timeyourself.security.host-guard:true}") boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        if (!enabled || isLoopback(request.getServerName())) {
            filterChain.doFilter(request, response);
            return;
        }

        // Write the response directly: sendError() would dispatch to /error and
        // can fail content negotiation (HTTP 500) for HTML page requests.
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write("Host not allowed");
    }

    private boolean isLoopback(String host) {
        if (host == null || host.isBlank()) {
            return true;
        }

        String normalized = host.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        return LOOPBACK_HOSTS.contains(normalized);
    }
}
