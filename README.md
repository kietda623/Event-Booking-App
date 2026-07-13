# Event Booking App

[Tiếng Việt](README.md) | [English](README.en.md)

Nền tảng đặt vé sự kiện full-stack, gồm backend Spring Boot và frontend React + Vite. Ứng dụng hỗ trợ duyệt sự kiện, đặt vé, thanh toán, quản lý vé, yêu thích sự kiện, check-in vé và màn hình quản trị.

## Tính năng chính

- Đăng ký, đăng nhập, refresh token và đăng xuất.
- Duyệt sự kiện theo danh sách, phổ biến, sắp diễn ra và gần vị trí người dùng.
- Xem chi tiết sự kiện, hạng vé, sơ đồ ghế và giữ ghế tạm thời.
- Đặt vé, thanh toán qua Stripe hoặc mock payment cho E2E/staging.
- Quản lý booking, hủy booking, xem vé và check-in bằng mã vé.
- Yêu thích sự kiện, cập nhật hồ sơ, nhắc lịch và push subscription.
- Admin quản lý sự kiện, hạng vé, booking theo sự kiện và xem analytics.

## Tech Stack

- **Backend:** Spring Boot 3.5, Spring Security, Spring Data JPA, Flyway, JWT, MySQL, Java 17
- **Frontend:** React 19, Vite, Zustand, TanStack Query, React Router, Stripe Elements
- **Testing:** JUnit + H2, Playwright E2E
- **Infrastructure:** Docker Compose, Nginx, MySQL 8.0
- **CI/CD:** GitHub Actions với backend tests, frontend build, Docker smoke test và Playwright E2E

---

## Yêu cầu

- JDK 17
- Maven Wrapper đã có sẵn: `mvnw` / `mvnw.cmd`
- Node.js 20+ cho frontend và E2E
- MySQL 8.0 nếu chạy backend local
- Docker Desktop nếu chạy staging stack bằng Docker Compose
- H2 được dùng tự động cho test backend

**Windows PowerShell - cấu hình JAVA_HOME:**

```powershell
$env:JAVA_HOME='C:\path\to\jdk-17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
```

Kiểm tra:

```powershell
java -version
```

Kết quả nên hiển thị Java 17.

---

## Chạy local

### 1. Tạo database

Backend local mặc định dùng database `event_booking`:

```sql
CREATE DATABASE event_booking;
```

### 2. Cấu hình biến môi trường cho backend

`src/main/resources/application.properties` đọc cấu hình từ environment variables. Ví dụ trên PowerShell:

```powershell
$env:DB_URL='jdbc:mysql://localhost:3306/event_booking'
$env:DB_USERNAME='root'
$env:DB_PASSWORD='your_mysql_password'
$env:JWT_SECRET='replace-with-64-char-secret'
$env:JWT_EXPIRY_MINUTES='1440'
$env:CORS_ALLOWED_ORIGINS='http://localhost:5173'
```

Các biến payment, mail và push có giá trị mặc định rỗng nên có thể bỏ qua khi chạy local cơ bản.

### 3. Chạy backend

Chạy từ thư mục root của project:

```powershell
.\mvnw.cmd spring-boot:run
```

Backend chạy tại `http://localhost:8080`.

Tài liệu API:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI YAML: `http://localhost:8080/v3/api-docs.yaml`

### 4. Chạy frontend

```powershell
cd frontend
npm install
npm run dev
```

Frontend chạy tại `http://localhost:5173`.

---

## Chạy bằng Docker Compose

Docker Compose dùng profile staging và MySQL database `eventbooking`.

```powershell
Copy-Item .env.example .env
docker compose up --build
```

Các service:

| Service | URL / Port |
|---|---|
| Frontend | `http://localhost` |
| Backend | `http://localhost:8080` |
| MySQL | `3306` trong Docker network |

Smoke test:

```bash
bash scripts/smoke-test.sh
```

> Không commit `.env`. File này đã nằm trong `.gitignore`.

---

## Chạy test

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

E2E cần ứng dụng đang chạy. Với staging Docker, bật seed và mock payment:

```powershell
Copy-Item .env.example .env
$env:SPRING_PROFILES_ACTIVE='staging,e2e'
$env:APP_E2E_SEED='true'
$env:VITE_ENABLE_MOCK_PAYMENT='true'
docker compose up -d --build
```

Sau đó chạy test:

```powershell
cd e2e
Copy-Item .env.example .env
npm ci
npx playwright install chromium
npx playwright test
```

Playwright report được tạo trong `e2e/playwright-report/`.

---

## API Overview

Response dùng envelope thống nhất:

```json
{ "success": true, "message": "...", "data": {} }
```

```json
{ "success": false, "code": "ERROR_CODE", "message": "...", "errors": [] }
```

| Nhóm | Endpoint | Quyền |
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

## Cấu trúc project

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

GitHub Actions chạy trên `main` và `duygri` cho cả push và pull request.

Pipeline gồm:

1. **Backend tests** - chạy `./mvnw test`.
2. **Frontend build** - chạy `npm ci` và `npm run build`.
3. **Docker Compose smoke test** - build container, start stack và chạy `scripts/smoke-test.sh`.
4. **Playwright E2E** - start staging stack với profile `staging,e2e`, seed dữ liệu test và chạy Chromium E2E.

Playwright HTML report được upload làm artifact khi job E2E fail.

---

## Ghi chú bảo mật

- Không commit `.env` hoặc secret thật.
- `JWT_SECRET`, Stripe key, mail key và VAPID key phải lấy từ môi trường deploy.
- Staging hardcode `app.admin.seed=false`; seed E2E chỉ chạy khi bật profile `e2e` và `APP_E2E_SEED=true`.
- Local token storage phục vụ MVP; khi lên production nên ưu tiên HttpOnly cookie và refresh-token flow đầy đủ.
