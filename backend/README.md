# Stepup Authentication With Spring Boot

## The quickstart

Add a simple Spring boot app with the convenience dependencies.

- The best way to generate a Ready-to-run application in Java is to use,
  the [Spring boot initializr](https://start.spring.io/).

By the way we need to enroll the following dependencies:

- Spring Web
- Spring Security
- OAuth2 Client
- OAuth2 Server
- Lombok
- Testcontainers

The followings for this introduction will use Java 17 and Maven (of course).

### The minimum implementation

The reader should accept that this app doesn't fill the best practice expected for the Production environment.

## Plug the backend to Keycloak

Obviously, we will use Docker and Compose to lead this tutorial.

### Create the Docker Compose stack

- Add the Dockerfile for the backend app.

Nothing to new for this Dockerfile, we just need to separate the build stage from the runtime stage.

```dockerfile
# Build Container
FROM maven:3.8.5-openjdk-17-slim as build

WORKDIR /app/

RUN apt-get update && \
	  apt-get install -y --no-install-recommends

COPY pom.xml .
COPY /src src

RUN mvn clean package -DskipTests

# Run Container
FROM openjdk:17

COPY --from=build /app/target/**.jar app.jar
```

- Add the Docker Compose services

Like the previous Docker configuration the Compose file is a-by-the-book example used to run the services:

```yaml
services:
  keycloak:
    image: quay.io/keycloak/keycloak:21.0
    ports:
      - 9080:8080
    networks:
      - keycloak-net
    volumes:
      - ./keycloak/config:/opt/keycloak/data/import
    environment:
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin
      KC_HOSTNAME: localhost
    entrypoint: [ "/opt/keycloak/bin/kc.sh", "start-dev", "--import-realm" ]
  backend:
    depends_on:
      - keycloak
    build: ./backend
    ports:
      - 8080:8080
    networks:
      - keycloak-net
    entrypoint: [ "java", "-Xms512m", "-Xmx1g", "-jar", "app.jar", "--debug" ]

networks:
  keycloak-net: { }
```

### Create the Keycloak the stepped-up configuration realm

The detailed configuration can be found in this the [keycloak/config/realm.json](../keycloak/config/stepup-realm.json)
file, but we can highlight some points:

- The client `user-client` is mandated for delivering an `access token` to the user,
- The frontend-url property has necessary to be set to http://172.30.0.2:8080 for the issuer claim value.

### Create the OAuth2 security layer

Backend side we can use a simple but an effective configuration where the settings are spread both in
the `application.yml` and `SecurityConfiguration.java` files. No more needed for the time.

The following shows the main content of the files:

- In the `application.yml` file we need to declare to Spring Security where the OIDC configuration can be found.

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://172.30.0.2:8080/realms/stepup
          jwk-set-uri: http://172.30.0.2:8080/realms/stepup/protocol/openid-connect/certs
```

- In the `SecurityConfiguration.java` file we need to describe a basic configuration indicates how to handle the
  request.

```java

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfiguration {

    // ...

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors();
        http.csrf().disable();
        http.authorizeHttpRequests()
                .anyRequest().authenticated()
                .and()
                .oauth2ResourceServer().jwt();

        return http.build();
    }
}
```

### Test the security configuration

At this point we can run the Compose stack:

```shell
❯ docker compose up --build
```

To be note that the client secret was generated during this documentation and located in the `stepup-realm.json` file.

Open a terminal and test the configuration.

- Get a valid access token from Keycloak

```shell
❯ TOKEN=`curl -XPOST 'http://localhost:9080/realms/stepup/protocol/openid-connect/token' \
        --header 'Content-Type: application/x-www-form-urlencoded' \
        --data-urlencode 'client_secret=6AurffbSrQ4yaGOl2TE7nvdveKwM2CB0' \
        --data-urlencode 'client_id=user-client' \
        --data-urlencode 'grant_type=password' \
        --data-urlencode 'username=ulrich' \
        --data-urlencode 'password=ulrich' | jq -r .access_token`
```

- Test an authenticated access

```shell
❯ curl -i "http://localhost:8080/user?email=foo@gmail.com" \
        --header "Authorization: Bearer $TOKEN"
```

- The expected result from the previous request should be:

```shell
HTTP/1.1 200 
Vary: Origin
Vary: Access-Control-Request-Method
Vary: Access-Control-Request-Headers
X-Content-Type-Options: nosniff
X-XSS-Protection: 0
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Pragma: no-cache
Expires: 0
X-Frame-Options: DENY
Content-Type: application/json
Transfer-Encoding: chunked
Date: Mon, 10 Apr 2023 16:35:48 GMT

{"email":"foo@gmail.com"}                                         
```

If we omit the `Authorization` header we should have this kind of response:

```shell
HTTP/1.1 401 
Vary: Origin
Vary: Access-Control-Request-Method
Vary: Access-Control-Request-Headers
Set-Cookie: JSESSIONID=21C630C8B2B38333DC81029DEBC2818E; Path=/; HttpOnly
WWW-Authenticate: Bearer
X-Content-Type-Options: nosniff
X-XSS-Protection: 0
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Pragma: no-cache
Expires: 0
X-Frame-Options: DENY
Content-Length: 0
Date: Mon, 10 Apr 2023 19:54:26 GMT
```

## My step-up implementation for Spring Boot

Obviously, we will use Docker and Compose to led this tutorial.
