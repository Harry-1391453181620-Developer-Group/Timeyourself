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
 * Servlet filter that rejects requests whose {@code Host} header does not name the local machine, protecting
 * the server that only listens on the loopback interface against DNS rebinding attacks performed by malicious
 * web pages opened in the same browser.
 *
 * The guard can be switched off with the {@code timeyourself.security.host-guard} property when the
 * application has to be reached through another host name, for example behind a tunnel.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class LocalHostGuardFilter extends OncePerRequestFilter {

    /**
     * Host names and addresses accepted as loopback; IPv6 entries are stored without surrounding brackets.
     */
    private static final Set<String> LOOPBACK_HOSTS = Set.of(
            "localhost",
            "127.0.0.1",
            "::1",
            "0:0:0:0:0:0:0:1"
    );

    /**
     * Whether the guard is active, configured through the {@code timeyourself.security.host-guard} property.
     */
    private final boolean enabled;

    /**
     * Creates the filter with the activation flag taken from the application configuration.
     *
     * @param enabled whether requests carrying a non-loopback host name are rejected
     */
    public LocalHostGuardFilter(
            @Value("${timeyourself.security.host-guard:true}") boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Passes requests with a loopback host name to the remaining filter chain and answers every other request
     * with a plain {@code 403} response. The rejection is written directly instead of using {@code sendError},
     * because the error dispatch would run content negotiation and could turn the rejection into an HTTP 500
     * for HTML page requests.
     *
     * @param request     current HTTP request whose server name is validated
     * @param response    current HTTP response used for the rejection body
     * @param filterChain remaining filter chain invoked for accepted requests
     * @throws ServletException when the downstream chain throws a servlet exception
     * @throws IOException      when the rejection body cannot be written or the downstream chain fails with an
     *                          I/O error
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        if (!enabled || isLoopback(request.getServerName())) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write("Host not allowed");
    }

    /**
     * Checks whether a server name denotes the local machine. A {@code null} or blank name is treated as
     * loopback, since a request without a host name cannot be produced by a rebinding domain. IPv6 addresses
     * surrounded by brackets are unwrapped before the comparison.
     *
     * @param host server name reported by the request
     * @return {@code true} when the name is {@code localhost}, an IPv4 loopback address or an IPv6 loopback
     *         address
     */
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
