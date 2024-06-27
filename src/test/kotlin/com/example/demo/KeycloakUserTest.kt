package com.example.demo

import com.example.demo.KeycloakUserTest.TestConfig
import dasniko.testcontainers.keycloak.KeycloakContainer
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.properties.TestcontainersPropertySourceAutoConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Duration

@SpringBootTest(classes = [TestConfig::class])
@Testcontainers
class KeycloakUserTest {
    companion object {
        private val log = LoggerFactory.getLogger(KeycloakUserTest::class.java)
    }

    @TestConfiguration(proxyBeanMethods = false)
    @ImportAutoConfiguration(TestcontainersPropertySourceAutoConfiguration::class)
    class TestConfig {

        companion object {
            const val POSTGRES_IMAGE: String = "postgres:16"
            const val KEYCLOAK_IMAGE: String = "quay.io/keycloak/keycloak:latest"
            const val REALM_FILE: String = "/keycloak/demo-realm.json"
            const val REALM_USERS_FILE: String = "/keycloak/demo-users-0.json"
            const val REALM_NAME: String = "demo"
        }

        @Bean
        @ServiceConnection
        fun postgres(): PostgreSQLContainer<*> = PostgreSQLContainer(POSTGRES_IMAGE)

        @Bean
        fun keycloak(registry: DynamicPropertyRegistry): KeycloakContainer {
            val kcContainer = KeycloakContainer(KEYCLOAK_IMAGE)
                .withRealmImportFiles(REALM_FILE, REALM_USERS_FILE)
                .withStartupTimeout(Duration.ofMillis(500_000))

            registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri") {
                kcContainer.authServerUrl + "/realms/" + REALM_NAME
            }

            return kcContainer
        }
    }

    @Autowired
    lateinit var keycloakContainer: KeycloakContainer

    @Test
    fun `get users from etip realm`() = runTest {
        val keycloak = keycloakContainer.keycloakAdminClient
        val etipRealm = keycloak.realm("demo").toRepresentation()

        etipRealm.users.forEach { log.debug("user: $it") }
        etipRealm.user("user1")?.also { log.debug("get uesr1: $it") }
    }
}
