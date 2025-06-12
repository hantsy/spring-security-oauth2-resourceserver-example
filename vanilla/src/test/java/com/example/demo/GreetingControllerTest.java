package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@WebFluxTest(controllers = GreetingController.class)
@Import(SecurityConfig.class)
class GreetingControllerTest {

    @Autowired
    private WebTestClient client;

    @MockitoBean
    private ReactiveJwtDecoder jwtDecoder;

    @Test
    void greetingWithoutToken() {
        this.client
                .get()
                .uri("/greeting/Hantsy")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void greetingWithToken() {
        this.client
                .mutateWith(SecurityMockServerConfigurers.mockJwt()
                        .authorities(AuthorityUtils.createAuthorityList("ROLE_USER"))
                        .jwt(jwt -> jwt.subject("test-subject")))
                .get()
                .uri("/greeting/Hantsy")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertThat(body).contains("Say Hello to Hantsy at"));
    }
}