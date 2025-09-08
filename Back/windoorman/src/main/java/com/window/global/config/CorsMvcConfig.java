package com.window.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsMvcConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedMethods("GET", "POST", "PATCH", "DELETE")
                .exposedHeaders("Authorization")
                .exposedHeaders("Set-Cookie")
                .allowCredentials(true)
                .allowedOrigins("https://k11b107.p.ssafy.io", "http://localhost:5173");
    }
}
