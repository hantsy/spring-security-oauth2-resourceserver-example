package com.example.demo;

import com.fasterxml.jackson.annotation.JsonProperty;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = DemoApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@Slf4j
class IntegrationTest {

    @Autowired
    private KeycloakContainer keycloakContainer;

    @LocalServerPort
    private int port;

    @Test
    void getUsersFromDemoRealm() {
        var keycloak = keycloakContainer.getKeycloakAdminClient();
        var realmResource = keycloak.realm(TestcontainersConfiguration.REALM_NAME);

        realmResource.users().list().forEach(user ->
                log.debug("user, name: {} {}, username: {}", user.getFirstName(), user.getLastName(), user.getUsername()));
        realmResource.users().searchByEmail("user1@example.com", true)
                .stream()
                .findFirst()
                .ifPresent(user1 -> log.debug("get user1: {},  {}", user1.getUsername(), user1.getRealmRoles()));
    }

    @Test
    void accessGreetingResource() {
        WebClient authClient = WebClient.create(keycloakContainer.getAuthServerUrl());

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("username", "user1");
        formData.add("password", "password");
        formData.add("grant_type", "password");

        KeycloakAccessToken tokenEntity = authClient.post()
                .uri("/realms/demo/protocol/openid-connect/token")
                .headers(httpHeaders -> httpHeaders.setBasicAuth("demo-client", "jKpu2nBDeZ05jN2VyfQg6tXh4hWNTgCp"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(KeycloakAccessToken.class)
                .block(Duration.ofSeconds(10));

        assertThat(tokenEntity).isNotNull();
        log.debug("access token is: {}", tokenEntity);
        String token = tokenEntity.accessToken();
        assertThat(token).isNotBlank();

        WebClient client = WebClient.create("http://localhost:" + port);
        var currentUserEntity = client.get()
                .uri("/me")
                .headers(h -> h.setBearerAuth(token))
                .retrieve()
                .toEntity(String.class)
                .block(Duration.ofSeconds(10));

        assertThat(currentUserEntity).isNotNull();
        assertThat(currentUserEntity.getStatusCode().value()).isEqualTo(200);
        log.debug("current user: {}", currentUserEntity.getBody());

        var greetingEntity = client.get()
                .uri("/greeting/Hantsy")
                .headers(h -> h.setBearerAuth(token))
                .retrieve()
                .toEntity(String.class)
                .block(Duration.ofSeconds(10));

        assertThat(greetingEntity).isNotNull();
        assertThat(greetingEntity.getStatusCode().value()).isEqualTo(200);
        log.debug("greeting result: {}", greetingEntity.getBody());
        assertThat(greetingEntity.getBody()).contains("Hantsy");
    }

    record KeycloakAccessToken(@JsonProperty("access_token") String accessToken) {
    }
}