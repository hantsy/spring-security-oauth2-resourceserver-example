package com.example.demo;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.properties.TestcontainersPropertySourceAutoConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.Duration;

@TestConfiguration(proxyBeanMethods = false)
@ImportAutoConfiguration(TestcontainersPropertySourceAutoConfiguration.class)
@Slf4j
class TestcontainersConfiguration {
    public static final String POSTGRES_IMAGE = "postgres:16";
    public static final String KEYCLOAK_IMAGE = "quay.io/keycloak/keycloak:latest";
    public static final String[] IMPORT_FILES = {"/keycloak/demo-realm.json", "/keycloak/demo-users-0.json"};
    public static final String REALM_NAME = "demo";

    @Bean
    @ServiceConnection
    public PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>(POSTGRES_IMAGE);
    }

    @Bean
    public KeycloakContainer keycloak() {
        return new KeycloakContainer(KEYCLOAK_IMAGE)
                .withRealmImportFiles(IMPORT_FILES)
                .withStartupTimeout(Duration.ofMillis(600_000))
                .withLogConsumer(it -> log.debug("[kc]: ({}): {}", it.getType(), it.getUtf8String()));
    }

    @Bean
    DynamicPropertyRegistrar dynamicPropertyRegistrar(KeycloakContainer keycloak) {
       return registry ->  registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> keycloak.getAuthServerUrl() + "/realms/" + REALM_NAME);
    }
}
