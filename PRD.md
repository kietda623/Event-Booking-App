# PRD - Event Booking App

## 1. Tổng quan sản phẩm

Event Booking App là nền tảng giúp người dùng tìm kiếm sự kiện, xem thông tin chi tiết, đặt vé, thanh toán và quản lý vé đã mua. Hệ thống cũng hỗ trợ admin quản lý danh sách sự kiện và cung cấp các chức năng tài khoản như hồ sơ cá nhân, nhắc lịch sự kiện và xác thực bằng JWT.

Tài liệu này được xây dựng dựa trên:

- File API Spec: `Event Booking App_API Specs_V1.0.pdf`.
- Codebase hiện tại tại `C:\Event_Booking_App\Event-Booking-App`.
- Hiện trạng backend Spring Boot trong repo.

### Mục tiêu

- Cung cấp tài liệu sản phẩm thống nhất cho backend, frontend và các bước phát triển tiếp theo.
- Mô tả rõ các chức năng cốt lõi của ứng dụng đặt vé sự kiện.
- Bổ sung định hướng frontend React để triển khai giao diện người dùng.
- Ghi nhận các khoảng cách giữa API spec mục tiêu và codebase hiện tại.
- Gợi ý các chức năng mở rộng có thể thêm sau MVP.

### Phạm vi MVP

MVP tập trung vào các luồng chính:

- Đăng ký và đăng nhập người dùng.
- Xem danh sách và chi tiết sự kiện.
- Đặt vé cho sự kiện.
- Thanh toán booking.
- Xem vé/booking của người dùng.
- Cập nhật hồ sơ và cài đặt nhắc nhở.
- Admin tạo, sửa, xoá sự kiện.

## 2. Người dùng và vai trò

### Khách chưa đăng nhập

- Xem danh sách sự kiện công khai.
- Xem chi tiết sự kiện.
- Đăng ký tài khoản.
- Đăng nhập.

### Người dùng đã đăng nhập

- Xem danh sách và chi tiết sự kiện.
- Đặt vé sự kiện.
- Thanh toán booking.
- Xem danh sách vé/booking của mình.
- Cập nhật hồ sơ cá nhân.
- Bật/tắt nhắc nhở sự kiện.

### Admin

- Quản lý sự kiện.
- Tạo sự kiện mới.
- Cập nhật thông tin sự kiện.
- Xoá sự kiện.
- Theo dõi dữ liệu sự kiện và booking trong các giai đoạn mở rộng.

## 3. Luồng nghiệp vụ chính

### Đăng ký

Người dùng tạo tài khoản bằng họ tên, email và mật khẩu. Hệ thống cần validate email, kiểm tra email không trùng, kiểm tra mật khẩu đạt yêu cầu tối thiểu, sau đó lưu mật khẩu dạng BCrypt.

Target API:

- `POST /api/auth/register`

Request mục tiêu:

```json
{
  "fullName": "Jane Doe",
  "email": "jane@example.com",
  "password": "password123"
}
```

Response thành công trả về thông tin người dùng đã tạo.

### Đăng nhập

Người dùng đăng nhập bằng email và mật khẩu. Hệ thống trả về JWT access token và thời điểm hết hạn.

Target API:

- `POST /api/auth/login`

Request mục tiêu:

```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

Response thành công trả về `accessToken` và `expire`.

### Xem danh sách sự kiện

Người dùng xem danh sách sự kiện theo các kiểu lọc:

- `popular`: sự kiện phổ biến dựa trên số lượng booking.
- `upcoming`: sự kiện sắp diễn ra.
- `nearby`: sự kiện gần vị trí người dùng.

Target API:

- `GET /api/events`

Query params:

- `type`: `popular`, `upcoming`, `nearby`.
- `search`: từ khoá tìm kiếm.
- `page`: trang hiện tại, mặc định 1.
- `size`: số lượng item mỗi trang, mặc định 10.

### Xem chi tiết sự kiện

Người dùng xem thông tin chi tiết của một sự kiện, gồm tên, thời gian, địa điểm, giá, mô tả và ảnh.

Target API:

- `GET /api/events/{id}`

### Quản lý sự kiện cho admin

Admin có quyền tạo, cập nhật và xoá sự kiện. Các endpoint này yêu cầu JWT hợp lệ và role `ADMIN`.

Target API:

- `POST /api/events`
- `PUT /api/events/{id}`
- `DELETE /api/events/{id}`

Thông tin sự kiện mục tiêu:

```json
{
  "title": "Art Exhibition",
  "dateTime": "2025-05-12T10:00:00",
  "location": "Modern Art Gallery, New York",
  "price": 25.0,
  "description": "Lorem ipsum dolor sit amet...",
  "imageUrl": "https://cdn.example.com/events/art_exhibition.jpg"
}
```

### Đặt vé

Người dùng đăng nhập chọn sự kiện và số lượng vé. Hệ thống kiểm tra sự kiện tồn tại, số lượng hợp lệ, tính tổng tiền và tạo booking trạng thái `PENDING`.

Target API:

- `POST /api/bookings`

Request:

```json
{
  "eventId": 101,
  "quantity": 2
}
```

### Xem vé của tôi

Người dùng xem danh sách vé/booking đã mua của mình.

Target API:

- `GET /api/tickets`

Dữ liệu trả về gồm mã vé, thông tin sự kiện, số lượng và trạng thái thanh toán.

### Thanh toán

Người dùng thanh toán cho một booking chưa thanh toán. Hệ thống validate booking, thông tin thẻ và cập nhật trạng thái thanh toán.

Target API:

- `POST /api/payments`

Request mục tiêu:

```json
{
  "bookingId": 501,
  "cardNumber": "1234 5678 9012 3456",
  "expiry": "12/25",
  "cvv": "123"
}
```

### Cài đặt nhắc nhở

Người dùng bật hoặc tắt nhắc nhở sự kiện.

Target API:

- `PUT /api/users/reminders`

Request:

```json
{
  "eventReminder": true
}
```

### Hồ sơ cá nhân

Người dùng cập nhật họ tên và avatar.

Target API:

- `PUT /api/users/profile`

Request:

```json
{
  "fullName": "Jane Doe",
  "avatar": "https://cdn.example.com/avatars/user123.jpg"
}
```

## 4. API và chuẩn response

### Response thành công

Các API target nên dùng cấu trúc response thống nhất:

```json
{
  "success": true,
  "message": "Operation successful",
  "data": {}
}
```

### Response validation lỗi

```json
{
  "success": false,
  "message": "Validation failed",
  "errors": [
    {
      "field": "email",
      "message": "Invalid email format"
    }
  ]
}
```

### Response bảo mật

Các endpoint ngoài `/api/auth/login` và `/api/auth/register` yêu cầu header:

```http
Authorization: Bearer <token>
```

Các lỗi bảo mật cần chuẩn hoá:

- `401 Unauthorized`: chưa đăng nhập, token thiếu, token sai hoặc token hết hạn.
- `403 Forbidden`: người dùng không đủ quyền.

## 5. Data model mục tiêu

### users

Lưu thông tin tài khoản người dùng.

Các trường chính:

- `id`
- `full_name`
- `email`
- `password`
- `avatar`
- `role`
- `created_at`

### events

Lưu thông tin sự kiện.

Các trường chính:

- `id`
- `title`
- `date_time`
- `location`
- `latitude`
- `longitude`
- `price`
- `description`
- `image_url`
- `created_at`
- `updated_at`

### bookings

Lưu giao dịch đặt vé.

Các trường chính:

- `id`
- `user_id`
- `event_id`
- `quantity`
- `total_price`
- `status`
- `created_at`

Trạng thái mục tiêu:

- `PENDING`
- `PAID`
- `CANCELLED`

### payments

Lưu thông tin thanh toán.

Các trường chính:

- `id`
- `booking_id`
- `amount`
- `status`
- `created_at`

Trạng thái mục tiêu:

- `PAID`
- `FAILED`

### reminders

Lưu cài đặt nhắc nhở của người dùng.

Các trường chính:

- `id`
- `user_id`
- `event_reminder`
- `created_at`

### tickets

Lưu vé được sinh ra từ booking đã thanh toán.

Các trường chính:

- `id`
- `ticket_code`
- `booking_id`

## 6. Hiện trạng codebase backend

Codebase hiện tại là dự án Spring Boot dùng Java 21, Maven, Spring Web, Spring Data JPA, Spring Security, JWT, Validation, Lombok và MySQL.

Các package chính:

- `controller`: `AuthController`, `EventController`, `BookingController`, `PaymentController`, `UserController`.
- `entity`: `User`, `Role`, `Event`, `Booking`, `Payment`, `Ticket`, `Reminder`.
- `dto`: auth, event, booking, user response DTO.
- `service`: auth, event, booking, payment.
- `repository`: user, role, event, booking, payment, ticket.
- `security`: JWT service và authentication filter.
- `exception`: exception handler và business exception.

### Chênh lệch so với API Spec V1.0

- API spec dùng `email` để đăng nhập, trong khi code hiện tại đang có DTO đăng nhập bằng `username`.
- API spec có `fullName` và `avatar`, trong khi entity `User` hiện có `username`, `password`, `email`, `roles`.
- API spec dùng role dạng enum trong bảng `users`, còn code hiện tại dùng `roles` và `user_roles`.
- API spec có CRUD sự kiện cho admin, nhưng controller hiện tại mới có `GET /api/events` và `GET /api/events/{id}`.
- API spec có `GET /api/tickets`, nhưng code hiện tại chưa có `TicketController`.
- API spec có `PUT /api/users/reminders` và `PUT /api/users/profile`, nhưng `UserController` hiện đang rỗng.
- API spec có payment body gồm `bookingId`, `cardNumber`, `expiry`, `cvv`, nhưng code hiện tại đang dùng `bookingId` dạng request param.
- Entity `Event` hiện chưa có `imageUrl`, `latitude`, `longitude`, `createdAt`, `updatedAt`.
- Entity `Booking` hiện chưa có `totalPrice` và `status`.
- Entity `Reminder` hiện đang gắn với `Event`, trong khi spec target gắn reminder settings với `User`.
- Service/repository implementation hiện tại còn ở mức skeleton ở nhiều file.
- `AuthController` hiện thiếu annotation controller/request mapping cần thiết.
- `SecurityConfig` hiện có lỗi cú pháp và thiếu annotation cấu hình cần thiết.

### Verification snapshot

Tại thời điểm lập PRD:

- Repo đã được đọc ở `C:\Event_Booking_App\Event-Booking-App`.
- PDF API Spec V1.0 đã được trích nội dung.
- Lệnh `.\mvnw.cmd -q test` hiện fail ở lỗi compile trong `SecurityConfig.java`.
- PRD này không sửa code backend, chỉ ghi nhận hiện trạng để định hướng phát triển.

## 7. Frontend React

Frontend nên được xây bằng React để cung cấp trải nghiệm đặt vé end-to-end cho người dùng và màn hình quản trị cho admin.

### Stack đề xuất

- React + Vite.
- React Router cho routing.
- Axios hoặc Fetch wrapper cho API client.
- Context API hoặc Zustand cho auth state.
- React Hook Form hoặc form state nhẹ cho form validation.
- Tailwind CSS, CSS Modules hoặc SCSS tuỳ định hướng UI.
- LocalStorage hoặc session storage để lưu JWT access token trong MVP.

### Cấu trúc route đề xuất

- `/`: trang danh sách sự kiện.
- `/login`: đăng nhập.
- `/register`: đăng ký.
- `/events/:id`: chi tiết sự kiện.
- `/bookings/checkout`: xác nhận đặt vé.
- `/payments/:bookingId`: thanh toán.
- `/tickets`: vé của tôi.
- `/profile`: hồ sơ cá nhân.
- `/admin/events`: quản lý sự kiện.
- `/admin/events/new`: tạo sự kiện.
- `/admin/events/:id/edit`: chỉnh sửa sự kiện.

### Màn hình Login/Register

Chức năng:

- Nhập email/username theo contract backend được chọn.
- Nhập mật khẩu.
- Hiển thị lỗi validation theo response API.
- Lưu token sau đăng nhập thành công.
- Điều hướng về trang chủ hoặc trang trước đó.

### Màn hình Home/Event Listing

Chức năng:

- Hiển thị danh sách sự kiện.
- Tìm kiếm theo từ khoá.
- Lọc theo `popular`, `upcoming`, `nearby`.
- Pagination.
- Empty state khi không có sự kiện.
- Loading và error state.

### Màn hình Event Detail

Chức năng:

- Hiển thị ảnh, tiêu đề, thời gian, địa điểm, giá, mô tả.
- Cho phép chọn số lượng vé.
- CTA đặt vé.
- Nếu chưa đăng nhập, điều hướng sang login.

### Màn hình Booking Confirmation

Chức năng:

- Hiển thị thông tin sự kiện.
- Hiển thị số lượng vé và tổng tiền.
- Tạo booking qua `POST /api/bookings`.
- Điều hướng sang thanh toán sau khi booking thành công.

### Màn hình Payment

Chức năng:

- Nhập thông tin thẻ trong MVP.
- Validate format cơ bản ở frontend.
- Gửi thanh toán qua `POST /api/payments`.
- Hiển thị kết quả thanh toán.
- Điều hướng sang vé của tôi sau khi thanh toán thành công.

### Màn hình My Tickets/My Bookings

Chức năng:

- Hiển thị danh sách vé/booking của người dùng.
- Hiển thị trạng thái `PENDING`, `PAID`, `CANCELLED`.
- Với booking `PENDING`, cho phép tiếp tục thanh toán nếu backend hỗ trợ.

### Màn hình Profile Settings

Chức năng:

- Cập nhật họ tên.
- Cập nhật avatar URL.
- Bật/tắt nhắc nhở sự kiện.
- Hiển thị thông báo lưu thành công hoặc lỗi validation.

### Màn hình Admin Event Management

Chức năng:

- Danh sách sự kiện.
- Tạo sự kiện mới.
- Chỉnh sửa sự kiện.
- Xoá sự kiện.
- Bảo vệ route theo role `ADMIN`.
- Hiển thị lỗi `403 Forbidden` nếu user không đủ quyền.

### Frontend API contract

Frontend cần có API client thống nhất:

- Tự động gắn `Authorization: Bearer <token>` cho protected endpoints.
- Tự động logout hoặc điều hướng login khi nhận `401`.
- Hiển thị thông báo không đủ quyền khi nhận `403`.
- Parse lỗi validation từ `errors`.
- Chuẩn hoá loading/error/success state cho các màn hình chính.

## 8. Gợi ý chức năng mở rộng

### Wishlist/Favorite events

Cho phép người dùng lưu sự kiện yêu thích để xem lại và đặt vé sau.

### Cancel booking và refund flow

Cho phép người dùng huỷ booking theo chính sách thời gian. Nếu đã thanh toán, hệ thống có thể tạo refund request.

### QR code ticket check-in

Sinh QR code cho vé đã thanh toán. Admin hoặc nhân viên sự kiện quét QR để check-in.

### Email/push reminders

Gửi email hoặc push notification trước giờ sự kiện dựa trên cài đặt reminder.

### Review và rating

Người dùng đánh giá sự kiện sau khi tham gia. Rating có thể dùng để cải thiện ranking sự kiện.

### Admin analytics

Bổ sung dashboard cho admin:

- Tổng doanh thu.
- Tổng vé đã bán.
- Sự kiện phổ biến.
- Tỷ lệ booking thanh toán thành công.
- Doanh thu theo thời gian.

### Seat map hoặc ticket tier

Hỗ trợ sơ đồ ghế hoặc nhiều loại vé như Standard, VIP, Early Bird.

### Upload ảnh

Cho phép upload ảnh sự kiện và avatar thay vì chỉ nhập URL.

### Nearby events bằng geolocation

Sử dụng latitude/longitude của sự kiện và vị trí người dùng để tính khoảng cách.

### Payment gateway thật

Tích hợp Stripe, VNPay hoặc MoMo sandbox để thay thế payment giả lập.

### Search nâng cao

Cho phép lọc theo khoảng giá, ngày, thành phố, category hoặc trạng thái còn vé.

### Event category

Phân loại sự kiện như âm nhạc, thể thao, triển lãm, hội thảo, giáo dục.

## 9. Tiêu chí nghiệm thu

PRD được xem là hoàn tất khi:

- File `PRD.md` tồn tại ở root repo.
- Nội dung viết bằng tiếng Việt.
- Có mô tả mục tiêu sản phẩm, người dùng, luồng nghiệp vụ, API, data model.
- Có phần frontend React với stack, route và màn hình chính.
- Có danh sách chức năng mở rộng.
- Có ghi nhận chênh lệch giữa API spec và codebase hiện tại.
- Có ghi nhận hiện trạng build/test backend.

## 10. Ưu tiên phát triển tiếp theo

### Phase 1 - Sửa backend để chạy được

- Sửa `SecurityConfig`.
- Bổ sung annotation cho controller/config/service/repository còn thiếu.
- Hoàn thiện repository interface theo Spring Data JPA.
- Hoàn thiện service implementation cho auth, event, booking, payment.
- Chuẩn hoá exception handler và response format.

### Phase 2 - Đồng bộ backend với API spec

- Chuyển login/register sang contract email/fullName nếu chọn theo spec.
- Bổ sung CRUD sự kiện cho admin.
- Bổ sung tickets endpoint.
- Bổ sung profile và reminders endpoint.
- Bổ sung payment request body và payment status.
- Bổ sung các field còn thiếu trong entity/schema.

### Phase 3 - Xây frontend React MVP

- Scaffold React + Vite.
- Tạo routing và layout.
- Tạo auth flow.
- Tạo event listing/detail.
- Tạo booking/payment flow.
- Tạo tickets/profile screens.
- Tạo admin event management.

### Phase 4 - Mở rộng sản phẩm

- QR ticket.
- Notification/reminder thật.
- Analytics dashboard.
- Upload ảnh.
- Payment gateway thật.
- Nearby events bằng vị trí GPS.
