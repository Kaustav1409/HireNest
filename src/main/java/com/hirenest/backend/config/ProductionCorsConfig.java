package com.hirenest.backend.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Allows browser calls from Live Server (localhost) and Vercel (*.vercel.app)
 * to a separately hosted Render/Railway backend.
 */
@Configuration
public class ProductionCorsConfig {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ProductionCorsConfig.class);

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CorsFilter productionCorsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        
        List<String> allowedOrigins = List.of(
                "https://hire-nest-virid.vercel.app",
                "http://localhost:3000",
                "http://localhost:5173"
        );
        
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedOriginPatterns(List.of("https://*.vercel.app"));
        config.setAllowedMethods(List.of("*"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        log.info("CORS Configuration initialized. Allowed Origins: {}, Allowed Origin Patterns: {}", allowedOrigins, "https://*.vercel.app");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
