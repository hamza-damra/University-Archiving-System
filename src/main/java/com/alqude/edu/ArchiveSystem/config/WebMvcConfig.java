package com.alqude.edu.ArchiveSystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

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
}
