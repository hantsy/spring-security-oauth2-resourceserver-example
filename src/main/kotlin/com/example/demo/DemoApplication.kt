package com.example.demo

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod.GET
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.config.web.server.invoke
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers.pathMatchers
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@SpringBootApplication
class DemoApplication

fun main(args: Array<String>) {
    runApplication<DemoApplication>(*args)
}

@RestController
@RequestMapping("/greeting")
class GreetingController {

    @GetMapping("/{name}")
    fun greeting(@PathVariable name: String): String {
        return "Say Hello to $name at " + LocalDateTime.now()
    }
}

@Configuration
class SecurityConfig {

    @Bean
    fun springWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain =
        http {
            csrf { disable() }
            httpBasic { disable() }
            formLogin { disable() }
            logout { disable() }

            // enable OAuth2 resource server support
            oauth2ResourceServer { jwt { } }
            authorizeExchange {
                authorize(pathMatchers(GET, "/greeting/**"), authenticated)
                authorize(anyExchange, permitAll)
            }
        }

    @Bean
    fun jwtDecoder(@Value("\${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") jwkSetUri: String): ReactiveJwtDecoder {
        return NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build()
    }

}