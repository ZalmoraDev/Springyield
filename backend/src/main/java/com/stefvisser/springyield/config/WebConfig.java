package com.stefvisser.springyield.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/// File required for Cross-Origin Sharing (CORS) configuration
/// Needed for Vue.js frontend to access the Spring Boot backend
@Configuration
public class WebConfig {

    @Value("${frontend.ip}")
    private String frontendIp;

    @Value("${frontend.port}")
    private String frontendPort;

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // Allow the frontend URL that's defined in application.properties
        config.addAllowedOrigin("http://" + frontendIp + ":" + frontendPort);
        config.addAllowedOrigin("https://springyield.stefvisser.com"); // Allow the production URL
        config.addAllowedOrigin("https://www.springyield.stefvisser.com"); // Allow the production URL with www
        config.addAllowedOrigin("https://api.springyield.stefvisser.com"); // Allow the production URL with www
        config.addAllowedOrigin("http://localhost:20300"); // Allow local development

        // Allow common HTTP methods
        config.addAllowedMethod("GET");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("DELETE");
        config.addAllowedMethod("PATCH");
        config.addAllowedMethod("OPTIONS");

        // Allow all headers
        config.addAllowedHeader("*");

        // Allow cookies and authentication
        config.setAllowCredentials(true);

        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
