package com.example.demo

import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt
import org.springframework.test.web.reactive.server.WebTestClient

@WebFluxTest
@Import(SecurityConfig::class)
class GreetingControllerTest {

    @Autowired
    lateinit var client: WebTestClient

    @MockkBean
    lateinit var jwtDecoder: ReactiveJwtDecoder

    @Test
    fun `greeting without token`() {
        this.client
            .get()
            .uri("/greeting/Hantsy")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `greeting with token`() {
        this.client
            .mutateWith(mockJwt()
                .authorities(AuthorityUtils.createAuthorityList("ROLE_USER"))
                .jwt { it.subject("test-subject") }
            )
            .get()
            .uri("/greeting/Hantsy")
            .exchange()
            .expectBody(String::class.java).value { it shouldContain "Say Hello to Hantsy at" }
    }
}