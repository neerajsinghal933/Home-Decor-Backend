# Inner Nest Decor Backend

Spring Boot REST API for the Inner Nest Decor storefront.

## Stack

- Java 21
- Spring Boot 3.5.16
- Maven
- Spring Web, Spring Data JPA, Spring Security
- Flyway migrations
- MySQL
- Actuator and OpenAPI

## Run Locally

Create MySQL database credentials, then:

```bash
cd backend
cp .env.example .env
mvn spring-boot:run
```

Flyway creates and seeds the local catalog on startup.

Set `APP_GOOGLE_CLIENT_ID` to the Google OAuth web client ID. The API only accepts Google credentials at `POST /api/auth/google`; password login and registration endpoints are intentionally not exposed.

Users are created with the `USER` role. Admin access is granted only by direct SQL:

```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'admin@example.com';
```

## API Docs

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`
