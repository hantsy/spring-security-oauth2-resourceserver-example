# Securing Restful APIs with Spring Security and Keycloak

The Restful APIs works as OAuth2 *ResourceServer* role.

Firstly prepare a Keycloak server in the development stage. 

## Setup Keycloak Server

Following the official [Running Keycloak in a container](https://www.keycloak.org/server/containers) guide, create a Docker compose file to start up a Keycloak server with the dependent Database. Here we use Postgres as an example.

```yaml
services:

  keycloak:
    build:
      context: ./
      dockerfile: Dockerfile
    ports:
      - "8000:8080"
      - "8443:8443"
    environment:
      - KC_DB=postgres
      - KC_DB_URL=jdbc:postgresql://postgres:5432/keycloakdb
      - KC_DB_USERNAME=keycloak
      - KC_DB_PASSWORD=keycloak
      - KC_HOSTNAME=localhost

      # set admin user to localhost admin UI access
      - KEYCLOAK_ADMIN=admin
      - KEYCLOAK_ADMIN_PASSWORD=admin@123
    depends_on:
      - postgres

  postgres:
    image: postgres
    ports:
      - "5432:5432"
    restart: always
    environment:
      POSTGRES_DB: keycloakdb
      POSTGRES_PASSWORD: keycloak
      POSTGRES_USER: keycloak
    volumes:
      - ./data/postgresql:/var/lib/postgresql
```

We use a multi-stages build in a Dockerfile to customize the Keycloak, eg. you can install a SSL cerificate to enable HTTPS connection, start the application in dev mode.

Execute the following command to start up the Keycloak Server.

```bash
docker compose --file ./keycloak/docker-compose.yml up --build
```

The `--build` here is used to build Docker image from Dockerfile firstly, then start up it.

Open a browser, navigate to http://localhost:8000 or https://localhost:8443, input the admin/password set in the *docker-compose.yaml* in the login page, and enter the Keycloak adminstration console.

1. Create a new Realm *demo*. 
2. Switch to *demo* Realm, create a new client *demo-client*, enable *Client Authentication*, *Authorization* in the *Capablity config* page.
3. Create a new user *demouser*, enable *Email Verified*. Set password for the new created user.

Open http://localhost:8000/realms/demo/.well-known/openid-configuration, you will see all available OIDC configurations for this realm.

> For production deployment, you can choose Kubernetes ready [Keycloak Operator](https://www.keycloak.org/operator/installation).  

## Resource Example Application

Open https://start.spring.io in your browser, choose the following options, and generate a project skeleton.

* Project(Build tools): Gradle - Kotlin
* Language: Kotlin
* Project Metadata/Java: 21
* Dependencies: Spring Reactive Web, OAuth2 Resource Server

In a Spring/Kotlin project, I would like use [Mockk](https://mockk.io) to replace the *Mockito* for mocking dependencies, and use [Kotest assertions](https://kotest.io) fluent APIs for asserting result in Kotlin DSL. Check the updated Gradle build script [here](https://github.com/hantsy/spring-security-oauth2-resourceserver-example/blob/master/build.gradle.kts).

Create a `@RestController` class to produce a simple Restfult API for testing purpose.

```kotlin
@RestController
@RequestMapping("/greeting")
class GreetingController {

    @GetMapping("/{name}")
    fun greeting(@PathVariable name: String): String {
        return "Say Hello to $name at " + LocalDateTime.now()
    }
}
```

Add a `@Configuration` class to setup *Resource Server* and protect the */greeting* endpoint in the `SecurityWebFilterChain` bean.

```kotlin
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
}
```

Open *application.properties*, set a `jwk-set-uri` property for the JWT token verification.

```properties
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8000/realms/demo/protocol/openid-connect/certs
```

Start the application, try to access the `/greeting` endpoint via `curl` command.

```bash
curl http://localhost:8080/greeting/Keycloak -v
> GET /greeting/Keycloak HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/8.7.1
> Accept: */*
>
< HTTP/1.1 401 Unauthorized
< WWW-Authenticate: Bearer
< Cache-Control: no-cache, no-store, max-age=0, must-revalidate
< Pragma: no-cache
< Expires: 0
< X-Content-Type-Options: nosniff
< X-Frame-Options: DENY
< X-XSS-Protection: 0
< Referrer-Policy: no-referrer
< content-length: 0
```

Let's generate an *Access Token* that stands for the user *demouser*, and use it as the security key to access the protected `/greeting` endpoint in the *Resource Server* example application.

```bash 
curl http://localhost:8000/realms/demo/protocol/openid-connect/token  \
-u "demo-client:OQttJYpKRAvcSMOdYkZYv3Y6mZ8EBLMA" \
-d "grant_type=password" \
-d "username=demouser" \
-d "password=password"

{"access_token":"eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICI3akRYcUVZQUwtRDRWS2puUDFzdlFoUFdHME1iWFY1TlFNYWZJclliWDNZIn0.eyJleHAiOjE3MTg3MDQ0MjMsImlhdCI6MTcxODcwNDEyMywianRpIjoiZmJkMTI0OWQtYTlmYS00NDgxLTgxZjgtZDUyMzRmYTg1NzAzIiwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDAwL3JlYWxtcy9kZW1vIiwiYXVkIjoiYWNjb3VudCIsInN1YiI6ImIzNjdhMWU3LWExNzEtNDdmNi05ZDc5LWZkZGE3ZTYwODY4YiIsInR5cCI6IkJlYXJlciIsImF6cCI6ImRlbW8tY2xpZW50Iiwic2lkIjoiMGE4Y2Y4MjAtYjBlMy00ODRjLWEzMWEtOWQ5ZThkMjgzZjU5IiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6WyJodHRwOi8vbG9jYWxob3N0OjMwMDAiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbIm9mZmxpbmVfYWNjZXNzIiwiREVNT19VU0VSIiwiZGVmYXVsdC1yb2xlcy1kZW1vIiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6ImVtYWlsIHByb2ZpbGUiLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwibmFtZSI6ImRlbW8gdXNlciIsInByZWZlcnJlZF91c2VybmFtZSI6ImRlbW91c2VyIiwiZ2l2ZW5fbmFtZSI6ImRlbW8iLCJmYW1pbHlfbmFtZSI6InVzZXIiLCJlbWFpbCI6ImRlbW91c2VyQGV4YW1wbGUuY29tIn0.eivfns04IX9L1o5MtjeOPyKZVhSkKbEPfSYNwMwnw5JQ_y4w_pKTxeqfzS737A1mFGv8sq5kRIkOGJdS8_ONxzvfPV2dGEJbYcuBnp6OWhz5tq6o1cxZ18GleBUzjZG7JPbXJb0i4g79NByjpXn-JdOY3FCApUxwPViO7A1pfVrsexJw_lIL_QAdJZaVHMYQTBcVafjIAxH4AnpeTNyG09LLhIHx6O8AkqSrq1soHseYyQoXvGum1mloHHYKxIr41tGeoqAx6VSp-aOsk52hWoztN_mXUGgPpB-a9fWP-hLb_eWtr1NTbN-XWFxJigkgTp7g6bavYakJyqN0PYFoPA","expires_in":300,"refresh_expires_in":1800,"refresh_token":"eyJhbGciOiJIUzUxMiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICI5OGFiMGRiMi04ZmM2LTRkOTUtYTBiYS04MmUxZGJhOWI0YWIifQ.eyJleHAiOjE3MTg3MDU5MjMsImlhdCI6MTcxODcwNDEyMywianRpIjoiYzUxMDUwNGEtNjVjMS00M2YyLWFjMWEtMmEwYWIwODYwNDc4IiwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDAwL3JlYWxtcy9kZW1vIiwiYXVkIjoiaHR0cDovL2xvY2FsaG9zdDo4MDAwL3JlYWxtcy9kZW1vIiwic3ViIjoiYjM2N2ExZTctYTE3MS00N2Y2LTlkNzktZmRkYTdlNjA4NjhiIiwidHlwIjoiUmVmcmVzaCIsImF6cCI6ImRlbW8tY2xpZW50Iiwic2lkIjoiMGE4Y2Y4MjAtYjBlMy00ODRjLWEzMWEtOWQ5ZThkMjgzZjU5Iiwic2NvcGUiOiJ3ZWItb3JpZ2lucyBhY3IgZW1haWwgcm9sZXMgcHJvZmlsZSBiYXNpYyJ9.nv6oxgYjrMKbCziXWWpyyPgTu23WzoIDeOBLjUiHvbKFE10Kk7x8MFVFNAls8ovY-kVXW8YIq6FhB5qUXSepow","token_type":"Bearer","not-before-policy":0,"session_state":"0a8cf820-b0e3-484c-a31a-9d9e8d283f59","scope":"email profile"}
```

Extract the `access_token` value in the JSON body, add a header `Authorization: Bearer token` to the original `curl` command, and hint the `/greeting` endpoint again.

```bash 
curl http://localhost:8080/greeting/Keycloak -v -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICI3akRYcUVZQUwtRDRWS2puUDFzdlFoUFdHME1iWFY1TlFNYWZJclliWDNZIn0.eyJleHAiOjE3MTg3MDQ0MjMsImlhdCI6MTcxODcwNDEyMywianRpIjoiZmJkMTI0OWQtYTlmYS00NDgxLTgxZjgtZDUyMzRmYTg1NzAzIiwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDAwL3JlYWxtcy9kZW1vIiwiYXVkIjoiYWNjb3VudCIsInN1YiI6ImIzNjdhMWU3LWExNzEtNDdmNi05ZDc5LWZkZGE3ZTYwODY4YiIsInR5cCI6IkJlYXJlciIsImF6cCI6ImRlbW8tY2xpZW50Iiwic2lkIjoiMGE4Y2Y4MjAtYjBlMy00ODRjLWEzMWEtOWQ5ZThkMjgzZjU5IiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6WyJodHRwOi8vbG9jYWxob3N0OjMwMDAiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbIm9mZmxpbmVfYWNjZXNzIiwiREVNT19VU0VSIiwiZGVmYXVsdC1yb2xlcy1kZW1vIiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6ImVtYWlsIHByb2ZpbGUiLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwibmFtZSI6ImRlbW8gdXNlciIsInByZWZlcnJlZF91c2VybmFtZSI6ImRlbW91c2VyIiwiZ2l2ZW5fbmFtZSI6ImRlbW8iLCJmYW1pbHlfbmFtZSI6InVzZXIiLCJlbWFpbCI6ImRlbW91c2VyQGV4YW1wbGUuY29tIn0.eivfns04IX9L1o5MtjeOPyKZVhSkKbEPfSYNwMwnw5JQ_y4w_pKTxeqfzS737A1mFGv8sq5kRIkOGJdS8_ONxzvfPV2dGEJbYcuBnp6OWhz5tq6o1cxZ18GleBUzjZG7JPbXJb0i4g79NByjpXn-JdOY3FCApUxwPViO7A1pfVrsexJw_lIL_QAdJZaVHMYQTBcVafjIAxH4AnpeTNyG09LLhIHx6O8AkqSrq1soHseYyQoXvGum1mloHHYKxIr41tGeoqAx6VSp-aOsk52hWoztN_mXUGgPpB-a9fWP-hLb_eWtr1NTbN-XWFxJigkgTp7g6bavYakJyqN0PYFoPA"

Say Hello to Keycloak at 2024-06-18T17:52:50.122802900
```

