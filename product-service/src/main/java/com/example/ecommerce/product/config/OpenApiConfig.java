package com.example.ecommerce.product.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger metadata for product-service.
 *
 * Declares a bearer-JWT security scheme so the Swagger UI shows an "Authorize" button — paste
 * a token there and try-it-out calls will carry "Authorization: Bearer ...". The doc pages
 * themselves are whitelisted in SecurityConfig, so the UI is viewable without a token; only
 * the actual API calls need one.
 *
 * UI:   /swagger-ui.html      JSON: /v3/api-docs
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Product Service API", version = "v1",
                description = "Product catalog and stock reservation"),
        security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public class OpenApiConfig {
}
