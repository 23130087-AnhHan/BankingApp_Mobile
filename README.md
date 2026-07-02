# 🌟 Hệ thống Ngân hàng Trực tuyến NLU Banking 🌟

Dự án này là một ứng dụng ngân hàng số được thiết kế theo kiến trúc **Microservices** hiện đại, kết hợp với ứng dụng di động **Android** dành cho khách hàng. Hệ thống sử dụng các công nghệ Java mới nhất để đảm bảo tính mở rộng, bảo mật và độc lập giữa các dịch vụ.

---

## 🏛️ Kiến trúc & Công nghệ sử dụng

Hệ thống bao gồm **7 dịch vụ backend (Spring Boot)** độc lập và **1 ứng dụng di động (Android - Java)**:

1. **Service Registry (Eureka Server - Port 8761):** Quản lý đăng ký dịch vụ động và khám phá dịch vụ giữa các microservices.
2. **API Gateway (Port 8080):** Cổng kết nối trung tâm, phân tuyến yêu cầu (routing) và bảo mật cuộc gọi API từ ứng dụng di động.
3. **User Service (Port 8082):** Quản lý thông tin người dùng, kết nối trực tiếp với Keycloak để đăng ký, đăng nhập và xác thực OTP.
4. **Account Service (Port 8081):** Quản lý tài khoản ngân hàng của người dùng, truy vấn số dư và trạng thái tài khoản.
5. **Sequence Generator (Port 8083):** Sinh mã số tài khoản và mã định danh tự động duy nhất trên toàn hệ thống.
6. **Transaction Service (Port 8084):** Thực hiện các giao dịch nạp tiền, rút tiền từ tài khoản.
7. **Fund Transfer Service (Port 8085):** Quản lý giao dịch chuyển tiền nội bộ giữa các tài khoản, xác thực OTP trước khi chuyển khoản.
8. **BankingMobileApp:** Ứng dụng Android viết bằng Java (SDK Platform 35) cung cấp giao diện người dùng mượt mà, hỗ trợ tạo tài khoản, chuyển tiền và quét mã QR.

---

## 🚀 Hướng dẫn khởi chạy hệ thống

### 1. Yêu cầu môi trường
Để chạy được hệ thống trên máy tính của bạn, cần chuẩn bị sẵn:
- **Java 17 (JDK 17)** cài đặt và cấu hình đường dẫn `JAVA_HOME`.
- **Docker Desktop** (khuyên dùng để khởi động nhanh MySQL và Keycloak). Nếu không dùng Docker, bạn phải tự cài MySQL 8.0 và Keycloak 22+ local trên cổng `8571`.
- **Android Studio** (để chạy ứng dụng di động).

---

### 2. Bước 1: Khởi động MySQL & Keycloak (Docker)
Để đơn giản hóa quá trình cài đặt cơ sở dữ liệu và máy chủ xác thực, dự án đã cấu hình sẵn tệp `docker-compose.yml` chạy đồng thời MySQL và Keycloak.

Tại thư mục gốc của dự án, mở Terminal (cmd hoặc powershell) và chạy lệnh:
```bash
docker-compose up -d
```
> [!NOTE]
> Lệnh này sẽ tự động tải các ảnh Docker tương ứng, tạo các cơ sở dữ liệu trống cần thiết (`user_service`, `account_service`, v.v.) dựa trên tệp khởi tạo `init-db.sql`.

---

### 3. Bước 2: Cấu hình Keycloak
Sau khi Docker khởi động thành công, Keycloak sẽ chạy tại địa chỉ: `http://localhost:8571`

Hãy cấu hình Keycloak theo các bước sau để hệ thống hoạt động:
1. Truy cập [http://localhost:8571](http://localhost:8571), chọn **Administration Console** và đăng nhập với tài khoản:
   - **Username:** `admin`
   - **Password:** `admin`
2. Tạo Realm mới: Di chuột vào dropdown góc trên bên trái, chọn **Create Realm**, điền tên realm là `banking-service` và nhấn **Create**.
3. Tạo Client cho Backend:
   - Vào mục **Clients** -> Chọn **Create client**.
   - Chọn **OpenID Connect**, nhập Client ID là `banking-service-api-client` và nhấn **Next**.
   - Bật các cấu hình quan trọng sau:
     - **Client authentication:** `ON` (Bắt buộc)
     - **Service accounts roles:** `ON` (Bắt buộc để dịch vụ backend gọi Admin API của Keycloak)
     - **Direct access grants:** `ON` (Bắt buộc để đăng nhập bằng tài khoản/mật khẩu trực tiếp từ Android)
   - Nhấn **Save**.
4. Cấp quyền quản lý cho Service Account:
   - Trong giao diện Client vừa tạo, mở tab **Service account roles** -> Chọn **Assign role**.
   - Chuyển bộ lọc hiển thị sang dạng Client roles (search theo client `realm-management`).
   - Gán các role sau cho Client:
     - `manage-users`
     - `view-users`
     - `query-users`
   - Nhấn **Assign**.
5. Lấy Client Secret:
   - Mở tab **Credentials** của client `banking-service-api-client`.
   - Copy giá trị của **Client Secret** để điền vào script khởi động ở bước sau (Secret mặc định được cấu hình trong code là `TjeQGZma15XGmLGIMI6M84oBU9549Sf9`).

---

### 4. Bước 3: Khởi chạy các Dịch vụ Backend
Thư mục gốc chứa tệp chạy tự động duy nhất `run-services.cmd`. Hãy double-click (hoặc chạy từ cmd) tệp tin này:
```cmd
run-services.cmd
```
**Quy trình chạy của Script:**
- Script sẽ kiểm tra môi trường Java của máy tính.
- Yêu cầu nhập Keycloak Client Secret (Nếu bạn đã cấu hình client secret trùng khớp với mặc định là `TjeQGZma15XGmLGIMI6M84oBU9549Sf9`, chỉ cần nhấn **Enter** để bỏ qua).
- Script sẽ tự động mở 7 cửa sổ dòng lệnh riêng biệt để chạy lần lượt các microservices theo thứ tự tối ưu (Khởi chạy Eureka trước, đợi 15 giây, khởi chạy API Gateway, sau đó khởi chạy tất cả dịch vụ nghiệp vụ khác).

Kiểm tra trạng thái hoạt động của các dịch vụ tại **Eureka Dashboard**: [http://localhost:8761](http://localhost:8761) (Trạng thái của các dịch vụ phải là `UP`).

---

### 5. Bước 4: Khởi chạy Ứng dụng Android
1. Mở **Android Studio**.
2. Chọn **Open** và dẫn tới thư mục `BankingMobileApp` trong dự án.
3. Chờ Gradle tải các thư viện và đồng bộ hóa cấu hình (Sync Project).
4. Kết nối thiết bị thực qua USB (đã bật USB Debugging) hoặc chạy trên Máy ảo Android Emulator.
5. Nhấn **Run** để cài đặt và trải nghiệm ứng dụng.

> [!TIP]
> - Hệ thống sẽ tự động gửi mã OTP xác thực đăng ký tài khoản hoặc giao dịch về log console của `User-Service` hoặc `Fund-Transfer` dịch vụ để bạn lấy mã kiểm tra thử nghiệm mà không cần cấu hình SMTP thật.