package com.example.demo

import com.example.demo.KeycloakIntegrationTest.TestConfig
import com.fasterxml.jackson.annotation.JsonProperty
import dasniko.testcontainers.keycloak.KeycloakContainer
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.boot.testcontainers.properties.TestcontainersPropertySourceAutoConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitEntity
import org.springframework.web.reactive.function.client.awaitExchange
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Duration


@SpringBootTest(classes = [TestConfig::class], webEnvironment = RANDOM_PORT)
@Testcontainers
class KeycloakIntegrationTest {
    companion object {
        private val log = LoggerFactory.getLogger(KeycloakIntegrationTest::class.java)
    }

    @TestConfiguration(proxyBeanMethods = false)
    @ImportAutoConfiguration(TestcontainersPropertySourceAutoConfiguration::class)
    class TestConfig {

        companion object {
            const val POSTGRES_IMAGE: String = "postgres:16"
            const val KEYCLOAK_IMAGE: String = "quay.io/keycloak/keycloak:latest"
            val IMPORT_FILES: Array<String> = arrayOf(
                "/keycloak/demo-realm.json", "/keycloak/demo-users-0.json"
            )
            const val REALM_NAME: String = "demo"
        }

        @Bean
        @ServiceConnection
        fun postgres(): PostgreSQLContainer<*> = PostgreSQLContainer(POSTGRES_IMAGE)

        @Bean
        fun keycloak(registry: DynamicPropertyRegistry): KeycloakContainer {
            val kcContainer = KeycloakContainer(KEYCLOAK_IMAGE)
                .withRealmImportFiles(* IMPORT_FILES)
                .withStartupTimeout(Duration.ofMillis(600_000))
                .withLogConsumer { log.debug("[kc]: ${it.type}: ${it.utf8String}") }

            registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri") {
                kcContainer.authServerUrl + "/realms/" + REALM_NAME
            }

            return kcContainer
        }
    }

    @Autowired
    lateinit var keycloakContainer: KeycloakContainer

    @LocalServerPort
    var port: Int = 0

    @Test
    fun `get users from etip realm`() = runTest {
        val keycloak = keycloakContainer.keycloakAdminClient
        val realmResource = keycloak.realm("demo")

        realmResource.users().list()
            .forEach { log.debug("user, name: ${it.firstName} ${it.lastName}, username: ${it.username}") }
        realmResource.users().searchByEmail("user1@example.com", true).firstOrNull()
            ?.also { log.debug("get uesr1:${it.username},  ${it.realmRoles}") }
    }

    @Test
    fun `access the greeting resource`() = runTest {
        val tokenClient = WebClient.create(keycloakContainer.authServerUrl)

        val formData: MultiValueMap<String, String> = LinkedMultiValueMap()
        formData.add("username", "user1")
        formData.add("password", "password")
        formData.add("grant_type", "password")

        val tokenEntity = tokenClient.post()
            .uri("/realms/demo/protocol/openid-connect/token")
            .headers { it.setBasicAuth("demo-client", "jKpu2nBDeZ05jN2VyfQg6tXh4hWNTgCp") }
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .accept(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromFormData(formData))
            .awaitExchange {
                it.awaitEntity(KeycloakAccessToken::class)
            }

        log.debug("access token is: $tokenEntity")
        val token = tokenEntity.body?.accessToken?: throw IllegalArgumentException("access token isn't set")

        val apiClient = WebClient.create("http://localhost:${port}")
        val currentUserEntity = apiClient.get()
            .uri("/me")
            .headers{ it.setBearerAuth(token)}
            .awaitExchange {
                it.awaitEntity<Map<String, Any>>()
            }
        currentUserEntity.statusCode.value() shouldBe 200
        log.debug("current user: ${currentUserEntity.body}")

        val greetingEntity = apiClient
            .get()
            .uri("/greeting/Hantsy")
            .headers { it.setBearerAuth(token) }
            .awaitExchange {
                it.awaitEntity(String::class)
            }

        greetingEntity.statusCode.value() shouldBe 200
        log.debug("greeting result: ${greetingEntity.body}")
        greetingEntity.body shouldContain "Hantsy"
    }
}


data class KeycloakAccessToken(@JsonProperty("access_token") val accessToken: String)