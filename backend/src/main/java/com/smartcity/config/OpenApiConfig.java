package com.smartcity.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI civicPulseOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CivicPulse Smart City Command Centre REST API")
                        .description("Production-grade unified Smart City Command Centre REST API. " +
                                "Covers Incident Management, Priority Rule Engine, SLA & Escalation Engine, " +
                                "Department Routing, Team Dispatch, and Municipal Response Actions.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("CivicPulse Command Operations")
                                .email("ops@civicpulse.gov"))
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("/").description("Current Server Context")
                ))
                .tags(List.of(
                        new Tag().name("Incidents").description("Incident lifecycle reporting, filtering, updates, status transitions, and audit trail"),
                        new Tag().name("Priority").description("Multi-factor Priority Rule Engine evaluation and tier scoring"),
                        new Tag().name("SLA").description("SLA target calculation, deadline tracking, active monitoring, and breach escalation"),
                        new Tag().name("Departments").description("Municipal department and operational team queries"),
                        new Tag().name("Assignments").description("Configurable department routing, team dispatch, and auto-assignment engine"),
                        new Tag().name("Response Actions").description("Recommended incident response plans, action approvals, and execution workflow"),
                        new Tag().name("Authentication").description("Operator credentials, role verification, and JWT session handling"),
                        new Tag().name("IoT Ingestion").description("MQTT sensor telemetry ingestion, status tracking, and event logs"),
                        new Tag().name("IoT Simulator").description("Sensor simulation and test scenario triggering without physical hardware")
                ))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth", new SecurityScheme()
                                .name("BearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter JWT Bearer token for authorized municipal operations")));
    }
}
