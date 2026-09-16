package com.innernest.decor.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
  @Bean
  OpenAPI innerNestOpenApi() {
    return new OpenAPI().info(new Info()
        .title("Inner Nest Decor API")
        .version("0.1.0")
        .description("Spring Boot REST API for catalog, cart, checkout, content, and authentication."));
  }
}
