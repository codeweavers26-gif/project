package com.project.backend.config;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Encoding;
import io.swagger.v3.oas.models.security.SecurityScheme;
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI brandShopAPI() {
        final String securitySchemeName = "Bearer Authentication";
        return new OpenAPI()
                .info(new Info()
                        .title("R&R API")
                        .version("v1.0")
                        .description("API documentation with JWT authentication"))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }

    // 🔥 THIS FIXES multipart + JSON
    @Bean
    public OpenApiCustomizer multipartJsonCustomizer() {
        return openApi -> openApi.getPaths().values().forEach(pathItem ->
            pathItem.readOperations().forEach(operation -> {
                if (operation.getRequestBody() != null &&
                    operation.getRequestBody().getContent() != null &&
                    operation.getRequestBody().getContent().containsKey("multipart/form-data")) {

                    operation.getRequestBody()
                        .getContent()
                        .get("multipart/form-data")
                        .addEncoding(
                            "product",
                            new Encoding().contentType("application/json")
                        );
                }
            })
        );
    }
}
