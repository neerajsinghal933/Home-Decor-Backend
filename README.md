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

The default profile is `local`. Create MySQL database credentials, export them if
they differ from the local defaults in `application-local.yml`, then run:

```bash
cd backend
mvn spring-boot:run
```

Flyway creates and seeds the local catalog on startup.

## Build

Java 21 is required.

```bash
cd backend
mvn clean package
```

The executable Spring Boot JAR is generated at `target/app.jar`.

## Run in Production

Configure these environment variables on the EC2 instance without committing
their values to Git:

```text
DB_URL=jdbc:mysql://<RDS-ENDPOINT>:3306/innernest
DB_USERNAME=innernestadmin
DB_PASSWORD=<RDS-MASTER-PASSWORD>
APP_ALLOWED_ORIGINS=<COMMA-SEPARATED-PRODUCTION-ORIGINS>
APP_FRONTEND_URL=https://www.innernestdecor.com
JWT_SECRET=<LONG-RANDOM-PRODUCTION-SECRET>
APP_GOOGLE_CLIENT_ID=<GOOGLE-OAUTH-WEB-CLIENT-ID>
RAZORPAY_KEY_ID=<RAZORPAY-KEY-ID>
RAZORPAY_KEY_SECRET=<RAZORPAY-KEY-SECRET>
RAZORPAY_WEBHOOK_SECRET=<SEPARATE-RAZORPAY-WEBHOOK-SECRET>
APP_S3_BUCKET=inner-nest-images
APP_AWS_REGION=ap-south-1
APP_PUBLIC_BASE_URL=https://api.innernestdecor.com
APP_MAIL_ENABLED=true
APP_MAIL_FROM=innernestofficial@gmail.com
APP_CONTACT_EMAIL=innernestofficial@gmail.com
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=innernestofficial@gmail.com
MAIL_PASSWORD=<GOOGLE-APP-PASSWORD>
MAIL_SMTP_AUTH=true
MAIL_STARTTLS_ENABLE=true
```

Then start the production profile:

```bash
java -jar app.jar --spring.profiles.active=prod
```

The `prod` profile has no datasource fallbacks. Startup fails when any required
database environment variable is missing, and Flyway uses the same production
datasource to apply migrations from `classpath:db/migration` before JPA validates
the schema. Product and profile images use the private S3 bucket through backend
`/uploads/...` URLs. AWS SDK credentials are supplied by the EC2 instance IAM role;
do not configure access-key or secret-key environment variables.

Contact messages are stored in the database and, when `APP_MAIL_ENABLED=true`,
emailed to `APP_CONTACT_EMAIL`. The sender receives a confirmation at the email
address on their authenticated account. New newsletter subscribers receive a
welcome email through the same SMTP configuration. Keep SMTP credentials in the
deployment environment only.

Set `APP_GOOGLE_CLIENT_ID` to the Google OAuth web client ID (or a comma-separated list during a controlled client-ID transition). The API only accepts Google credentials at `POST /api/auth/google`; password login and registration endpoints are intentionally not exposed.

Razorpay API keys are used only by the backend. Configure the Dashboard webhook
URL as `https://api.innernestdecor.com/api/payments/razorpay/webhook`, subscribe
to `payment.captured`, `payment.failed`, `refund.created`, `refund.processed`,
and `refund.failed`, and set its signing secret as
`RAZORPAY_WEBHOOK_SECRET`.

Razorpay checkout creation records a payment attempt and immutable cart snapshot,
not an Inner Nest order. A row is inserted into `orders` only after a valid payment
signature or signed `payment.captured` webhook is verified. Failed attempts retain
the cart and never appear in customer or admin order lists.

Cancellation and return requests are customer-owned and admin-reviewed. Refunds
are initiated only by the backend with the stored Razorpay payment ID, a durable
idempotency key, and the server-calculated refundable amount. Refund creation is
shown as pending until a signed Razorpay refund webhook reconciles the final state.
Transactional order emails use the existing SMTP settings; delivery failure is
recorded independently and never rolls back an order or refund state transition.

Users are created with the `USER` role. Admin access is granted only by direct SQL:

```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'admin@example.com';
```

## API Docs

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`
