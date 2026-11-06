package com.timeyourself.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 明确告诉 WebView：本应用的 HTML/JS/CSS/JSON 都是 UTF-8。
 * 这样即使某个平台没有从 &lt;meta charset&gt; 推断出编码，也不会出现中文乱码。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class Utf8ContentTypeFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        String path = request.getRequestURI();
        if (path.endsWith(".html") || path.equals("/")) {
            response.setContentType("text/html;charset=UTF-8");
        } else if (path.endsWith(".js")) {
            response.setContentType("application/javascript;charset=UTF-8");
        } else if (path.endsWith(".css")) {
            response.setContentType("text/css;charset=UTF-8");
        } else if (path.endsWith(".json")) {
            response.setContentType("application/json;charset=UTF-8");
        } else if (path.endsWith(".ttf")) {
            response.setContentType("font/ttf");
        }

        filterChain.doFilter(request, response);
    }
}
