package com.urbano.monolith.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI urbanoOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Urbano Homes API Document")
                        .description("RESTful Backend API specification for Urbano Homes property technology platform.\n\n" +
                                "### Role-Based Access Control:\n" +
                                "* **SUPER_ADMIN**: Platform oversight, PM Account approvals, Property moderation, & Audit inspection.\n" +
                                "* **PM_ADMIN**: Agency ownership, property & unit management, staff management, lease execution, & financial reporting.\n" +
                                "* **PM_STAFF**: Unit viewings, maintenance ticket resolution, & payment verification.\n" +
                                "* **TENANT**: Self-serve activation, M-Pesa STK Push payments, lease review, & maintenance submissions.\n" +
                                "* **PUBLIC**: Unauthenticated listing search & viewing requests.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Urbano Homes Engineering")
                                .email("engineering@urbano.homes")
                                .url("https://urbano.homes"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://urbano.homes/terms")))
                .servers(List.of(
                        new Server().url("http://localhost:9090").description("Local Development Server"),
                        new Server().url("https://api.urbano.homes").description("Production Gateway Server")
                ))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT access token obtained from POST /api/auth/login or POST /api/auth/tenant/activate.")));
    }
}
