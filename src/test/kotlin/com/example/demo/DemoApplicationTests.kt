package com.example.demo

import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import java.util.function.Consumer

@SpringBootTest
@AutoConfigureWebTestClient
class DemoApplicationTests {

    val token: Consumer<HttpHeaders> = Consumer<HttpHeaders> { http ->
        http.setBearerAuth(
            "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICItM1VDcE1EQ0ZqbG9zSGk5SDRGVUR5MW8tTzh4QUNZblBBN05lV3F4QV8wIn0.eyJleHAiOjE3MTg2MjI4ODIsImlhdCI6MTcxODYyMjU4MiwianRpIjoiY2NlMTc5MmYtMmJkZC00MTExLThjYTItNzhlYWQ1YjhjOGZjIiwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDAwL3JlYWxtcy9kZW1vIiwiYXVkIjpbImQ5YWJjY2Y4LWVhNTYtNDVkMi1hMDkwLTUwYTZhYzE4YjFlOCIsImFjY291bnQiXSwic3ViIjoiODk3ZTFkZmQtMjM3OS00OGU5LTgxZWUtNTY1ZjZjZTVjYWFmIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiZGVtb2FwcCIsImFjciI6IjEiLCJhbGxvd2VkLW9yaWdpbnMiOlsiaHR0cDovL2xvY2FsaG9zdDozMDAwIl0sInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJvZmZsaW5lX2FjY2VzcyIsImRlZmF1bHQtcm9sZXMtZGVtbyIsInVtYV9hdXRob3JpemF0aW9uIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsiZGVtb2FwcCI6eyJyb2xlcyI6WyJ1bWFfcHJvdGVjdGlvbiJdfSwiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsIm1hbmFnZS1hY2NvdW50LWxpbmtzIiwidmlldy1wcm9maWxlIl19fSwic2NvcGUiOiJwcm9maWxlIGVtYWlsIiwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJjbGllbnRIb3N0IjoiMTkyLjE2OC40OC4xIiwicHJlZmVycmVkX3VzZXJuYW1lIjoic2VydmljZS1hY2NvdW50LWRlbW9hcHAiLCJjbGllbnRBZGRyZXNzIjoiMTkyLjE2OC40OC4xIiwiY2xpZW50X2lkIjoiZGVtb2FwcCJ9.Fd-BGQfwLVoKSKASwZODJU0ycgIm4-5ANkdHHt5lgtTonnPh1GmrqkdAF-mdwYqZ9ut5PhA4hDyvL7rSDVNS_pKJqBhu60euoGIn7W86YZY1nqT65EvQcWV9pfq9wYqv_cahLv8Oq4F3sv0jRiFZ5kIJrJpjxC98I3NQcc94Hzy_iekZV2GSKK9Pr-Oj52wqFJXj4aKmPR_EtF0iST0CQhF_1gKDCHAbaQ5osffHL7arsudWxO5LbX8_GKo0gZNW-51dY-Wle0891FE6NuRAocH8IW1XNTBlGAW2SWb0bl772_HLttFgotEDK1DT-Ok0E4a0sJEtkJgNTCAEzkJA3w"
        )
    }

    @Autowired
    lateinit var client: WebTestClient

    @Test
    fun contextLoads() {
        this.client.get().uri("/greeting/Hantsy")
            .headers(token)
            .exchange()
            .expectBody(String::class.java).value { it shouldContain "Say Hello to Hantsy at" }
    }

}
