package com.example.demo.config;

import com.example.demo.logging.SpringBootJdbcAppender;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoggingConfig {

    @Bean
    public SpringBootJdbcAppender jdbcAppender() {
        return new SpringBootJdbcAppender();
    }
}