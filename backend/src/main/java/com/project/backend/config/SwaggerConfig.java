package com.project.backend.config;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI brandShopAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("BrandShop API Documentation")
                        .description("API documentation for BrandShop project (Auth, Products, Orders, Users, Addresses)")
                        .version("1.0")
                        .contact(new Contact()
                                .name("Your Name")
                                .email("your-email@example.com")
                        )
                );
    }
}
