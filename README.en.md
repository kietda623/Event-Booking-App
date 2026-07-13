# Event Booking App

[Tiếng Việt](README.md) | [English](README.en.md)

A full-stack event booking platform built with Spring Boot and React + Vite. The app supports event discovery, bookings, payments, tickets, favorites, ticket check-in, and admin operations.

## Key Features

- Register, login, refresh token, and logout.
- Browse events by all, popular, upcoming, and nearby filters.
- View event details, ticket tiers, seat maps, and temporary seat holds.
- Create bookings, pay with Stripe or E2E/staging mock payment, and generate tickets.
- Manage bookings, cancel bookings, view tickets, and check tickets in by code.
- Favorite events, update user profile, configure reminders, and manage push subscriptions.
- Admin event management, ticket tier management, event booking list, and analytics.

## Tech Stack

- **Backend:** Spring Boot 3.5, Spring Security, Spring Data JPA, Flyway, JWT, MySQL, Java 17
- **Frontend:** React 19, Vite, Zustand, TanStack Query, React Router, Stripe Elements
- **Testing:** JUnit + H2, Playwright E2E
- **Infrastructure:** Docker Compose, Nginx, MySQL 8.0
- **CI/CD:** GitHub Actions with backend tests, frontend build, Docker smoke test, and Playwright E2E

---

## Requirements

- JDK 17
- Maven Wrapper included as `mvnw` / `mvnw.cmd`
- Node.js 20+ for frontend and E2E
- MySQL 8.0 for local backend runtime
- Docker Desktop for the Docker Compose staging stack
- H2 is used automatically for backend tests

**Windows PowerShell - set JAVA_HOME:**

```powershell
$env:JAVA_HOME='C:\path\to\jdk-17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
```

Verify:

```powershell
java -version
```

The output should show Java 17.

---

## Local Setup

### 1. Create the database

The local backend defaults to the `event_booking` database:

```sql
CREATE DATABASE event_booking;
```

### 2. Configure backend environment variables

`src/main/resources/application.properties` reads configuration from environment variables. Example for PowerShell:

```powershell
$env:DB_URL='jdbc:mysql://localhost:3306/event_booking'
$env:DB_USERNAME='root'
$env:DB_PASSWORD='your_mysql_password'
$env:JWT_SECRET='replace-with-64-char-secret'
$env:JWT_EXPIRY_MINUTES='1440'
$env:CORS_ALLOWED_ORIGINS='http://localhost:5173'
```

Payment, mail, and push variables default to empty values, so they can be skipped for a basic local run.

### 3. Run the backend

Run from the project root:

```powershell
.\mvnw.cmd spring-boot:run
```

The backend starts at `http://localhost:8080`.

API documentation:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI YAML: `http://localhost:8080/v3/api-docs.yaml`

### 4. Run the frontend

```powershell
cd frontend
npm install
npm run dev
```

The frontend starts at `http://localhost:5173`.

---

## Docker Compose

Docker Compose uses the staging profile and the MySQL database name `eventbooking`.

```powershell
Copy-Item .env.example .env
docker compose up --build
```

Services:

| Service | URL / Port |
|---|---|
| Frontend | `http://localhost` |
| Backend | `http://localhost:8080` |
| MySQL | `3306` inside the Docker network |

Smoke test:

```bash
bash scripts/smoke-test.sh
```

> Do not commit `.env`. It is already listed in `.gitignore`.

---

## Running Tests

### Backend

```powershell
.\mvnw.cmd test
```

### Frontend

```powershell
cd frontend
npm ci
npm run lint
npm run build
```

### Playwright E2E

E2E requires the app to be running. For the Docker staging stack, enable E2E seed data and mock payment:

```powershell
Copy-Item .env.example .env
$env:SPRING_PROFILES_ACTIVE='staging,e2e'
$env:APP_E2E_SEED='true'
$env:VITE_ENABLE_MOCK_PAYMENT='true'
docker compose up -d --build
```

Then run the tests:

```powershell
cd e2e
Copy-Item .env.example .env
npm ci
npx playwright install chromium
npx playwright test
```

The Playwright report is generated in `e2e/playwright-report/`.

---

## API Overview

All responses use a unified envelope:

```json
{ "success": true, "message": "...", "data": {} }
```

```json
{ "success": false, "code": "ERROR_CODE", "message": "...", "errors": [] }
```

| Group | Endpoint | Auth |
|---|---|---|
| Auth | `POST /api/auth/register` | Public |
| Auth | `POST /api/auth/login` | Public |
| Auth | `POST /api/auth/refresh` | Public |
| Auth | `POST /api/auth/logout` | User |
| Events | `GET /api/events?type=popular\|upcoming\|nearby` | Public |
| Events | `GET /api/events/{id}` | Public |
| Events | `GET /api/events/nearby-preview` | Public |
| Events | `POST /api/events` | Admin |
| Events | `PUT /api/events/{id}` | Admin |
| Events | `DELETE /api/events/{id}` | Admin |
| Event tiers | `POST /api/events/{id}/tiers` | Admin |
| Event tiers | `PUT /api/events/{id}/tiers/{tierId}` | Admin |
| Event tiers | `DELETE /api/events/{id}/tiers/{tierId}` | Admin |
| Seats | `GET /api/events/{id}/seats` | Public |
| Seats | `POST /api/events/{id}/seats/hold` | User |
| Seats | `DELETE /api/events/{id}/seats/hold` | User |
| Bookings | `POST /api/bookings` | User |
| Bookings | `GET /api/bookings/my` | User |
| Bookings | `POST /api/bookings/{id}/cancel` | User |
| Bookings | `PUT /api/bookings/{id}/cancel` | User |
| Payments | `POST /api/payments` | User |
| Payments | `GET /api/payments/{id}` | User |
| Payments | `POST /api/payments/webhook` | Stripe |
| Tickets | `GET /api/tickets` | User |
| Tickets | `POST /api/tickets/checkin` | Admin |
| Profile | `GET /api/users/profile` | User |
| Profile | `PUT /api/users/profile` | User |
| Reminders | `PUT /api/users/reminders` | User |
| Favorites | `GET /api/users/favorites` | User |
| Favorites | `GET /api/favorites` | User |
| Favorites | `POST /api/events/{id}/favorite` | User |
| Favorites | `DELETE /api/events/{id}/favorite` | User |
| Favorites | `POST /api/favorites/{eventId}` | User |
| Push | `GET /api/push/vapid-public-key` | Public |
| Push | `POST /api/push/subscribe` | User |
| Push | `DELETE /api/push/subscribe` | User |
| Admin | `GET /api/admin/analytics` | Admin |
| Admin | `GET /api/events/{id}/bookings` | Admin |

Full contract: `http://localhost:8080/v3/api-docs.yaml`

---

## Project Structure

```text
/
├── src/                         # Spring Boot backend source
│   ├── main/
│   │   ├── java/com/eventbooking/
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   ├── dto/
│   │   │   ├── model/
│   │   │   └── config/
│   │   └── resources/
│   │       ├── db/migration/    # Flyway migrations
│   │       ├── application.properties
│   │       ├── application-staging.properties
│   │       └── application-e2e.properties
│   └── test/
├── backend/
│   └── Dockerfile               # Backend Docker image
├── frontend/                    # React + Vite frontend
│   ├── src/
│   │   ├── api/
│   │   ├── components/
│   │   ├── pages/
│   │   ├── routes/
│   │   ├── store/
│   │   └── utils/
│   ├── Dockerfile
│   └── nginx.conf
├── e2e/                         # Playwright tests
│   ├── helpers/
│   ├── tests/
│   └── playwright.config.js
├── scripts/
│   └── smoke-test.sh
├── docker-compose.yml
├── .env.example
├── PRD.md
└── PRD.html
```

---

## CI/CD

GitHub Actions runs on `main` and `duygri` for both push and pull request events.

Pipeline:

1. **Backend tests** - runs `./mvnw test`.
2. **Frontend build** - runs `npm ci` and `npm run build`.
3. **Docker Compose smoke test** - builds containers, starts the stack, and runs `scripts/smoke-test.sh`.
4. **Playwright E2E** - starts the staging stack with the `staging,e2e` profile, seeds test data, and runs Chromium E2E.

The Playwright HTML report is uploaded as an artifact when the E2E job fails.

---

## Security Notes

- Never commit `.env` or real secrets.
- `JWT_SECRET`, Stripe keys, mail keys, and VAPID keys must come from the deployment environment.
- Staging hardcodes `app.admin.seed=false`; E2E seed data only runs when profile `e2e` and `APP_E2E_SEED=true` are enabled.
- Local token storage is acceptable for MVP; production should prefer HttpOnly cookies and a complete refresh-token flow.
