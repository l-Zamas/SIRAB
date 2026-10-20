package com.sirab.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

class DevEnvironmentConfigTests {

    @Test
    void developmentProfileLoadsCredentialPropertiesFromLocalEnvFile() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(ProbeConfiguration.class)
                .web(WebApplicationType.NONE)
                .profiles("dev")
                .run()) {
            var environment = context.getEnvironment();
            assertTrue(environment.containsProperty("SIRAB_DEV_CAMPO_PASSWORD"));
            assertTrue(environment.containsProperty("SIRAB_DEV_ADMIN_PASSWORD"));
            assertTrue(environment.getPropertySources().stream()
                    .anyMatch(propertySource -> propertySource.getName().contains(".env")));
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class ProbeConfiguration {
    }
}
