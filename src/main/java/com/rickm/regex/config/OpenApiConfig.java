package com.rickm.regex.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for API documentation.
 */
@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Regex Engine API")
                        .version("1.0.0")
                        .description("""
                                Production-grade Regular Expression Engine with Backtracking Algorithm.
                                
                                ## Supported Regex Constructs
                                
                                | Syntax | Description |
                                |--------|-------------|
                                | `\\t` | Tab character |
                                | `\\s` | Whitespace (space, tab, newline, etc.) |
                                | `[abc]` | Character class - matches a, b, or c |
                                | `[^abc]` | Negated class - matches anything except a, b, c |
                                | `[a-z]` | Character range - matches a through z |
                                | `[a-zA-Z]` | Multiple ranges - matches any letter |
                                | `*` | Zero or more (greedy) |
                                | `+` | One or more (greedy) |
                                | `?` | Zero or one (greedy) |
                                | `\\|` | Alternation (OR) |
                                | `\\x` | Escape character x |
                                | `.` | Any printable character (except newline) |
                                | `(expr)` | Grouping |
                                """)
                        .contact(new Contact()
                                .name("API Support")
                                .email("support@example.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local development server")
                ));
    }
}
