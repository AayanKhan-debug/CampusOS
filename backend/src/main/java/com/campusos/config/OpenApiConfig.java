package com.campusos.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * OpenApiConfig — configures the Swagger UI documentation.
 *
 * Once the app runs, visit http://localhost:8080/swagger-ui.html to see and
 * test all API endpoints in an interactive UI.
 *
 * The @SecurityScheme annotation adds a global "Authorize" button in Swagger UI
 * where you can paste your JWT token and test protected endpoints directly.
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "CampusOS API",
        version = "1.0",
        description = "Student-focused campus platform API. " +
                      "Use the Authorize button to add your JWT token for protected endpoints.",
        contact = @Contact(name = "CampusOS Team")
    )
)
@SecurityScheme(
    name = "Bearer Authentication",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    in = SecuritySchemeIn.HEADER,
    description = "Paste your JWT token here (without the 'Bearer ' prefix)"
)
public class OpenApiConfig {
    // Configuration is done entirely through annotations above
}
