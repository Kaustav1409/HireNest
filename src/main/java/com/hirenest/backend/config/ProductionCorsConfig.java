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
        
        config.setAllowedOriginPatterns(List.of(
                "https://*.vercel.app",
                "https://*.onrender.com",
                "http://localhost:*"
        ));
        config.setAllowedMethods(List.of("*"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        log.info("CORS Configuration initialized. Allowed Origin Patterns: {}", "https://*.vercel.app, https://*.onrender.com, http://localhost:*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
