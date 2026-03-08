# API Gateway – Keycloak (JWT) + Eureka + Spring Cloud Gateway

This module provides a centralized API Gateway built using Spring Cloud Gateway (WebFlux) with JWT-based authentication via Keycloak and dynamic service discovery via Eureka.

All incoming requests are routed through this gateway, where authentication is validated before forwarding traffic to backend microservices.


## 📌 Features

* Centralized authentication using Keycloak JWT tokens
* Spring Security WebFlux for reactive, modern security
* Routes protected by JWT
* Eureka Service Discovery (lb://SERVICE-NAME)
* Clean route mapping using StripPrefix filters
* Supports microservices:
  * USER-SERVICE
  * ACCOUNT-SERVICE
* Token Relay to forward access tokens downstream
* Role-based authorization (using Keycloak roles)

## 📁 Project Structure

```text
spring-boot-keycloak-microservices
│── service-registry
|      ├─ src/main/java/com/example/registry
│      |     └── ServiceRegistryApplication.java
|      ├─ src/main/resources
│      |     └── application.yaml
│      └── build.gradle
│── api-gateway
|      ├─ src/main/java/com/example/gateway
│      |     └── ApiGatewayApplication.java
|      ├─ src/main/resources
│      |     └── application.yaml
│      └── build.gradle
│── user-service
|      ├─ src/main/java/com/example/gateway
│      |     ├── UserServiceApplication.java
│      |     ├── controller
│      |     |     └── UserController.java
│      |     ├── entity
│      |     |     └── User.java
│      |     ├── repository
│      |     |     └── UserRepository.java
│      |     └── service
│      |     |     └── UserService.java
|      ├─ src/main/resources
│      |     └── application.yaml
│      └── build.gradle
│── account-service
|      ├─ src/main/java/com/example/gateway
│      |     ├── AccountServiceApplication.java
│      |     ├── controller
│      |     |     └── AccountController.java
│      |     ├── entity
│      |     |     └── Account.java
│      |     ├── repository
│      |     |     └── AccountRepository.java
│      |     └── service
│      |     |     └── AccountService.java
|      ├─ src/main/resources
│      |     └── application.yaml
│      └── build.gradle
│── build.gradle (parent)
│── settings.gradle
└── gradle/wrapper
└── README.md
```

Every module contains:
* controller
* service
* entity / dto
* repository
* resources/application.yml

## 🛠️ Technologies Used

| Layer              | Technology                              | 
| :---               |    :----:                               | 
| Language           | Java 21                                 | 
| Backend Framework  | Spring Boot 3.5.11                      | 
| Cloud Toolkit      | Spring Cloud 2025.0.1                   |
| Service Discovery  | Eureka Server / Client                  |
| Security           | OAuth2 Resource Server & OAuth2 Client  |
| API Gateway        | Spring Cloud Gateway                    |
| IAM Provider       | Keycloak                                |
| Build Tool         | Gradle                                  | 
| Database           | H2 (in-memory)                          |
| ORM                | Spring Data JPA                         |
| Tests              | JUnit 5                                 |

## Explanation
### 1. Service Registry

  Add below dependencies in **build.gradle** file.
  ```build
	implementation 'org.springframework.boot:spring-boot-starter-web'
	implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-server'
  ```
  Add below configuration in the **application.yaml** file.
  ```yaml
  server:
    port: 8761
  spring:
    application:
      name: service-registry 
  eureka:
    client:
      register-with-eureka: false
      fetch-registry: false
  ```
Add **@EnableEurekaServer** annotation on ServiceRegistryApplication class (root level class).
  ```java
  package com.example.registry;
  
  import org.springframework.boot.SpringApplication;
  import org.springframework.boot.autoconfigure.SpringBootApplication;
  import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
  
  @SpringBootApplication
  @EnableEurekaServer
  public class ServiceRegistryApplication {
  
  	public static void main(String[] args) {
  		SpringApplication.run(ServiceRegistryApplication.class, args);
  	}
  
  }
  ```
### 2. API Gateway

  * Add spring cloud dependencies.
    ```build
  	implementation 'org.springframework.cloud:spring-cloud-starter-gateway'
  	implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'
    ```
  * Authentication happens in API Gateway.
  * Authorization must also be configured in each downstream service (user-service, account-service, etc).
  * Gateway does not forward the authentication context automatically unless configured properly.
  * Gateway should work as **OAuth2 Resource Server** so add OAuth2 Rresource Server dependency & keycloak issuer-uri to **integrate Keycloak** for centralized authentication.

      **build.gradle**
      ```build
    	implementation 'org.springframework.boot:spring-boot-starter-oauth2-resource-server'
      ```    
      **application.yaml**
      ```yaml
      spring:
        application:
          name: api-gateway
        security:
          oauth2:
            resourceserver:
              jwt:
                issuer-uri: http://localhost:8100/realms/bank-realm
      ```
  * Gateway must forward the JWT token to downstream services (user-service, account-service). Add Oauth2 Client dependency & TokenRelay filters
  
    **build.gradle**
    ```build
  	implementation 'org.springframework.boot:spring-boot-starter-oauth2-client'
    ```
    **application.yaml**
    ```yaml
    spring:
      cloud:
        gateway:
          routes:
            - id: user-service
              uri: lb://USER-SERVICE
              predicates:
                - Path=/api/users/**
              filters:
                - StripPrefix=1
                - TokenRelay
            - id: account-service
              uri: lb://ACCOUNT-SERVICE
              predicates:
                - Path=/api/accounts/**
              filters:
                - StripPrefix=1
                - TokenRelay
    ```

  * Add SecurityConfig.java file.
    ```java
    package com.example.gateway.config;
    
    import org.springframework.context.annotation.Bean;
    import org.springframework.context.annotation.Configuration;
    import org.springframework.security.config.Customizer;
    import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
    import org.springframework.security.config.web.server.ServerHttpSecurity;
    import org.springframework.security.web.server.SecurityWebFilterChain;
    
    @Configuration
    @EnableWebFluxSecurity
    public class SecurityConfig {
    
        @Bean
        public SecurityWebFilterChain filterChain(ServerHttpSecurity http) {
            http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(auth -> auth
                        .pathMatchers("/actuator/health", "/actuator/info").permitAll()
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                );
            return http.build();
        }
    }
    ```

  * **Key points:**

    * Disables CSRF (not needed for stateless APIs)
    * Allows: /actuator/health, /actuator/info
    * Everything else must include:
        Authorization: Bearer <jwt-token>
    * JWT tokens issued by Keycloak are validated automatically via the configured issuer-uri

### 3. Other Services (user-service, account-service)
  * Add **OAuth2 Resource Server** dependency
    ```gradle
    implementation 'org.springframework.boot:spring-boot-starter-oauth2-resource-server'
    ```
  * Use **@PreAuthorize("hasRole('ADMIN')")** annotation on method level for authorization.
    ```java
    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Account> create(@RequestBody Account account) {
        System.out.println("account: " + account);
        accountService.create(account);
        return ResponseEntity.ok(account);
    }
    ```

## ▶️ How to Run

#### Clone the repository
  ```text
  git clone https://github.com/your-repo-url.git
  cd your-project
  ```
#### Build all services
  ```text
  ./gradlew clean build
  ```
#### Start services in order

1. Start Service Registry (Eureka)
  ```text
   cd service-registry
   ./gradlew bootrun
  ```
   Runs at: **http://localhost:8761**

2. Start Keycloak

   Ensure Keycloak is running at:
   ```text
   http://localhost:8100
   ```

   and the realm:
   ```text
   bank-realm
   ```
   is configured with a client that issues JWT tokens.
   

2. Start API Gateway
  ```text
   cd api-gateway
   ./gradlew bootrun
  ```
   Gateway URL: **http://localhost:8080**

3. Start other microservices

  ```text
    cd user-service
    ./gradlew bootrun
 
    cd account-service
    ./gradlew bootrun
  ```

## 📬 Service Registry URLs
| Service            | URL                     |
| :---               |    :----:               |
| Eureka Dashboard   | http://localhost:8761   |

All microservices automatically register here.

## 🧪 Testing the Gateway

#### Step 1 - Get JWT Token from Keycloak

Use Postman or curl:

```text
POST /realms/bank-realm/protocol/openid-connect/token
```
with form data:
```text
grant_type=password
client_id=<your-client-id>
client_secret=<client-secret>
username=<username>
password=<password>
```

#### Step 2 - Call API Gateway

Use Postman or curl:

```curl
curl -i http://localhost:8080/api/users/me \
  -H "Authorization: Bearer <token>"
```
