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
 * Servlet filter that declares UTF-8 as the character encoding of every response and assigns explicit content
 * types for the kinds of static assets served by the application. This keeps HTML, JavaScript, CSS and JSON
 * responses correctly decoded inside the embedded WebView even when the platform does not infer the encoding
 * from the document metadata, which would otherwise corrupt non-ASCII text.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class Utf8ContentTypeFilter extends OncePerRequestFilter {

    /**
     * Sets the response character encoding to UTF-8, refines the content type according to the extension of
     * the requested resource and then continues the filter chain. Requests for other resources keep the
     * content type chosen by the handler while still carrying the UTF-8 character encoding.
     *
     * @param request     current HTTP request whose path selects the content type
     * @param response    current HTTP response whose encoding and content type are adjusted
     * @param filterChain remaining filter chain
     * @throws ServletException when the downstream chain throws a servlet exception
     * @throws IOException      when the downstream chain fails with an I/O error
     */
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
