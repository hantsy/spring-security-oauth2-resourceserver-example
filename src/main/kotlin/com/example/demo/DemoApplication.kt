package com.example.demo

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.http.HttpMethod.GET
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.config.web.server.invoke
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers.pathMatchers
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
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
    companion object {
        private val log = LoggerFactory.getLogger(SecurityConfig::class.java)
    }

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
                authorize(pathMatchers(GET, "/greeting/**"), hasRole("DEMO_USER"))
                authorize(anyExchange, permitAll)
            }
        }


//    @Bean
//    fun jwtDecoder(@Value("\${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") jwkSetUri: String): ReactiveJwtDecoder {
//        return NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build()
//    }


    @Bean
    fun jwtAuthenticationConverter(): ReactiveJwtAuthenticationConverter {
        val jwtAuthenticationConverter = ReactiveJwtAuthenticationConverter()
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(customRolesConverter())
        return jwtAuthenticationConverter
    }

    private fun customRolesConverter(): Converter<Jwt, Flux<GrantedAuthority>> = Converter { jwt ->
        (jwt.claims["realm_access"] as Map<String, Any>?)
            ?.let { it["roles"] as Collection<String>? }
            ?.let { roles ->
                Flux.fromIterable(roles).map { SimpleGrantedAuthority("ROLE_$it") }
            }
    }

}