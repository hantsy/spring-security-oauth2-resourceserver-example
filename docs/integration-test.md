# Integration Test with Keycloak

To test the protected resources in an OAuth2 ResourceServer, Spring Security Test module provides utilities to mock JWT token when making authorized request on the resources.

The following is an example to test our */greeting* endpoint.

```kotlin
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
```

With `mutateWith` method, you can assemble a mocked JWT token with authorties before making the request.

In the middle of our development progress, we declared a `ReactiveJwtDecoder` bean to add our custom auditence validator to check the audience claim. 

If the `ReactiveJwtDecoder` bean is built with the `issuerUri` property, it will resovle the OIDC Configuration at the bean initializaiton stage. 
When the url of the `issuerUri` value is not available, for example, we use a local Keycloak, but forgot to start up it at the moment, then it will fail the above controller tests before running the tests, add a `@MockkBean ...jwtDecoder` to overcome the barirrer.




