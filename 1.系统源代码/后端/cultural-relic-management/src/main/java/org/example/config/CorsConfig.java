package org.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                // 允许所有路径
                registry.addMapping("/**")
                        // 允许你的前端地址（5173）访问
                        .allowedOrigins("http://localhost:5173")
                        // 允许的请求方式
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        // 允许所有的请求头
                        .allowedHeaders("*")
                        // 是否允许携带 Cookie
                        .allowCredentials(true);
            }
        };
    }
}