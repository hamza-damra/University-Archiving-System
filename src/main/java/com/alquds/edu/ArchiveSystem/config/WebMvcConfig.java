package com.alquds.edu.ArchiveSystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Spring MVC Configuration for view resolution.
 * Configures view resolvers to map view names to HTML templates in static
 * resources.
 * 
 * @author Archive System Team
 * @version 1.0
 * @since 2024-11-20
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * Configure view resolver to resolve view names to HTML files in static
     * directory.
     * Maps view names like "deanship/dashboard" to "static/deanship/dashboard.html"
     * 
     * @return ViewResolver configured for static HTML files
     */
    @Bean
    public ViewResolver viewResolver() {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        resolver.setPrefix("/");
        resolver.setSuffix(".html");
        resolver.setOrder(1);
        return resolver;
    }

    /**
     * Configure resource handlers to serve static resources.
     * Ensures static resources are properly served from classpath with correct MIME types.
     * 
     * @param registry ResourceHandlerRegistry to add resource handlers to
     */
    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // Serve static resources from classpath:/static/ with proper caching
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(0); // Disable cache for development
    }

    /**
     * Configure content negotiation to ensure proper MIME types for static resources.
     * 
     * @param configurer ContentNegotiationConfigurer to configure
     */
    @Override
    @SuppressWarnings("deprecation")
    public void configureContentNegotiation(@NonNull org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer configurer) {
        configurer.favorPathExtension(false)
                .favorParameter(false)
                .ignoreAcceptHeader(false)
                .defaultContentType(org.springframework.http.MediaType.APPLICATION_JSON);
    }

    /**
     * Configure view controllers for simple page mappings.
     * This is not used for deanship pages as they require security annotations,
     * but kept for potential future use.
     * 
     * @param registry ViewControllerRegistry to add view controllers to
     */
    @Override
    public void addViewControllers(@NonNull ViewControllerRegistry registry) {
        // View controllers can be added here for simple page mappings
        // Deanship pages use @Controller with @PreAuthorize for security
    }

    /**
     * Add interceptor to set Cache-Control headers for HTML pages.
     * This prevents browsers from caching HTML pages which could cause
     * stale authentication redirects.
     * 
     * @param registry InterceptorRegistry to add interceptors to
     */
    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(new NoCacheHtmlInterceptor())
                .addPathPatterns("/**/*.html", "/", "/index.html")
                .excludePathPatterns("/css/**", "/js/**", "/api/**");
    }

    /**
     * Interceptor that adds no-cache headers to HTML responses.
     * Prevents browser from caching HTML pages and auth-related redirects.
     */
    private static class NoCacheHtmlInterceptor implements HandlerInterceptor {
        @Override
        public boolean preHandle(@NonNull HttpServletRequest request, 
                                 @NonNull HttpServletResponse response, 
                                 @NonNull Object handler) {
            String path = request.getRequestURI();
            // Add no-cache headers for HTML pages
            if (path.endsWith(".html") || path.equals("/")) {
                response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
                response.setHeader("Pragma", "no-cache");
                response.setHeader("Expires", "0");
            }
            return true;
        }
    }
}
