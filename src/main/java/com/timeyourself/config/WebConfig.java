package com.timeyourself.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC configuration that forwards the application root to the bundled start page and serves the HTML
 * pages from the {@code static/html} class path directory at the root path as well.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Forwards requests for the application root to the start page without a redirect, so the browser address
     * stays unchanged and the page can load its relative assets.
     *
     * @param registry registry that receives the view controller mapping
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/html/index.html");
    }

    /**
     * Maps root level HTML requests to the {@code static/html} class path directory, which makes pages such as
     * {@code /myWeek.html} reachable without the directory prefix.
     *
     * @param registry registry that receives the resource handler mapping
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/*.html")
                .addResourceLocations("classpath:/static/html/");
    }
}
