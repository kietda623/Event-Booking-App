# Event-Booking-App

Backend MVP for an event booking workflow built with Spring Boot, Spring Security, Spring Data JPA, JWT, MySQL, and Java 25.

## Requirements

- JDK 25
- Maven Wrapper, already included as `mvnw.cmd`
- MySQL for local runtime
- H2 is used for tests through `src/test/resources/application.properties`

On Windows PowerShell, use the JDK 25 installed by the modernization task:

```powershell
$env:JAVA_HOME='C:\Users\admin\AppData\Local\jdks\jdk-25.0.2'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
```

## Verify

```powershell
.\mvnw.cmd -q test
```

If Maven reports `release version 25 not supported`, the terminal is still using an older JDK. Re-check `JAVA_HOME` and `.\mvnw.cmd -version`.

## Local Configuration

Runtime database settings live in `src/main/resources/application.properties`.

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/event_booking
spring.datasource.username=root
spring.datasource.password=<your-local-password>
```

Create the `event_booking` database before starting the app. Hibernate is currently configured with `spring.jpa.hibernate.ddl-auto=update`.

## MVP API Flow

1. Register or login with username/password:
   - `POST /api/auth/register`
   - `POST /api/auth/login`
2. Browse events:
   - `GET /api/events`
   - `GET /api/events/{id}`
3. Admin manages events:
   - `POST /api/events`
   - `PUT /api/events/{id}`
   - `DELETE /api/events/{id}`
4. User books and pays:
   - `POST /api/bookings`
   - `GET /api/bookings/my`
   - `POST /api/payments`
   - `GET /api/tickets`
5. User profile/reminders:
   - `PUT /api/users/profile`
   - `PUT /api/users/reminders`

All endpoints except auth and public event reads require `Authorization: Bearer <token>`.

Payment is currently an internal mock flow. It marks the booking as `PAID`, creates a payment record, and generates a ticket code.

## Frontend Workspace

The React frontend lives under `frontend/` and is split into two Vite apps with a shared package:

- `frontend/apps/user` - responsive end-user web app on port `5173`
- `frontend/apps/admin` - admin event management web app on port `5174`
- `frontend/packages/shared` - API client, DTO types, auth helpers, formatters, and shared shadcn-style UI primitives

Install dependencies once from the frontend root:

```powershell
cd frontend
npm install
```

Run the user app:

```powershell
npm run dev:user
```

Run the admin app:

```powershell
npm run dev:admin
```

Both apps proxy `/api` to `http://localhost:8080`, so start the Spring Boot backend first. The admin app expects an account with role `ADMIN`; for local development, enable the existing admin seeder properties if you need a seeded admin user.

Frontend verification:

```powershell
cd frontend
npm run lint
npm run typecheck
npm run build:user
npm run build:admin
```
