# Android API runtime test checklist

Checklist này dùng cho Android Emulator và backend local. Android phải giữ base URL:

```text
http://10.0.2.2:8080/
```

## 1. Chuẩn bị backend

- [ ] MySQL và Keycloak đang chạy.
- [ ] Mở `http://localhost:8761` và xác nhận có `USER-SERVICE`, `ACCOUNT-SERVICE`, `SEQUENCE-GENERATOR`, `TRANSACTION-SERVICE`, `FUND-TRANSFER-SERVICE`, `API-GATEWAY` ở trạng thái UP.
- [ ] Gọi `http://localhost:8080/actuator/health` và nhận trạng thái UP.
- [ ] API Gateway vẫn chạy port `8080`.

Kết quả mong đợi: Gateway định tuyến được request, không có `503 Service Unavailable`.

## 2. Chạy Android

- [ ] Mở folder `BankingMobileApp` trong Android Studio.
- [ ] Khởi động Android Emulator.
- [ ] Chạy cấu hình `app`.
- [ ] Mở Logcat và lọc theo package `com.example.bankingmobileapp` khi cần debug.

Kết quả mong đợi: Dashboard mở được; session mới hiển thị “Chưa có khách hàng/tài khoản”, không tự gán User ID giả.

## 3. Test Register

- [ ] Mở “Đăng ký hồ sơ khách hàng”.
- [ ] Nhập họ tên, số điện thoại, email chưa dùng và mật khẩu.
- [ ] Nhấn “Tạo hồ sơ khách hàng” một lần.

Kết quả mong đợi:

- Nút bị khóa trong lúc gọi `POST /api/users/register`.
- Thành công: app lưu email, thông báo API chưa trả `userId`, và hiện nút đi tới Tài khoản.
- Email trùng: hiển thị thông báo đã đăng ký.
- Keycloak/User-Service lỗi: hiển thị thông báo backend/Keycloak, không hiện stacktrace và không crash.

## 4. Test Account create/find/activate

- [ ] Lấy `userId` thật đã tạo từ dữ liệu quản trị/backend; không dùng ID đoán.
- [ ] Sang màn hình Tài khoản, nhập `userId` một lần.
- [ ] Nhấn “Mở tài khoản tiết kiệm”.
- [ ] Hoặc dùng “Tìm theo mã khách hàng” nếu tài khoản đã tồn tại.

Kết quả mong đợi:

- `POST /accounts` thành công, sau đó app tự gọi `GET /accounts/{userId}`.
- App lưu `userId`, `accountId`, `accountNumber`, `availableBalance` vào session.
- Màn hình báo “Đã lưu tài khoản mặc định”.
- Tài khoản mới thường ở trạng thái `PENDING` và số dư `0`.

Lưu ý nghiệp vụ hiện tại: backend yêu cầu số dư tối thiểu `1000` trước khi kích hoạt. Nếu kích hoạt ngay, app phải hiển thị lỗi số dư rõ ràng. Sau bước Nạp tiền bên dưới, quay lại và kích hoạt lại; kết quả mong đợi là trạng thái `ACTIVE`.

## 5. Test Dashboard balance

- [ ] Quay lại Dashboard.
- [ ] Nhấn “Làm mới số dư”.

Kết quả mong đợi: app tự gọi `GET /accounts/{userId}`, hiển thị số tài khoản, số dư và trạng thái; người dùng không phải nhập lại ID.

## 6. Test Deposit

- [ ] Mở “Nạp / Rút tiền”.
- [ ] Xác nhận tài khoản nguồn được điền từ session.
- [ ] Nhập số tiền `1000` hoặc lớn hơn và nhấn “Nạp tiền”.

Kết quả mong đợi:

- Gọi `POST /transactions` với `transactionType=DEPOSIT`.
- Sau thành công app gọi `GET /accounts/balance?accountNumber=...` và cập nhật session balance.
- Số tiền rỗng, bằng `0` hoặc âm bị chặn trước khi gọi API.

Sau bước này, quay lại Tài khoản và kích hoạt nếu tài khoản vẫn `PENDING`.

## 7. Test Withdraw

- [ ] Đảm bảo tài khoản đã `ACTIVE`.
- [ ] Nhập số tiền nhỏ hơn hoặc bằng số dư và nhấn “Rút tiền”.
- [ ] Thử thêm một lần với số tiền lớn hơn số dư.

Kết quả mong đợi:

- Giao dịch hợp lệ gọi `POST /transactions` với `transactionType=WITHDRAWAL`, rồi cập nhật balance.
- Vượt số dư hiển thị “Số dư không đủ”.
- Tài khoản chưa kích hoạt hiển thị thông báo trạng thái tài khoản, không crash.

## 8. Test Transfer

Chuẩn bị hai tài khoản thật; tài khoản nguồn phải `ACTIVE` và đủ số dư.

- [ ] Mở “Chuyển tiền”; tài khoản nguồn phải lấy từ session.
- [ ] Nhập tài khoản nhận khác tài khoản nguồn, số tiền và lời nhắn tùy chọn.
- [ ] Nhấn “Xác nhận chuyển tiền”.

Kết quả mong đợi:

- Gọi `POST /fund-transfers` chỉ với `fromAccount`, `toAccount`, `amount`.
- Lời nhắn chỉ xuất hiện trên biên nhận local, không được gửi cho backend.
- Sau thành công app tải lại balance của tài khoản nguồn.
- Hai tài khoản giống nhau hoặc amount không hợp lệ bị chặn trên Android.

## 9. Test History

- [ ] Mở “Lịch sử”.

Kết quả mong đợi:

- App tự dùng `accountNumber` trong session và gọi `GET /transactions?accountId=...`.
- Danh sách hiển thị an toàn cả khi một field trong item bị thiếu.
- Danh sách rỗng hiển thị “Tài khoản chưa có giao dịch nào”.
- Không có account session hiển thị “Chưa có tài khoản để xem lịch sử”.

Tên query là `accountId`, nhưng contract hiện tại của Transaction-Service lưu số tài khoản trong field này; không thay bằng numeric `accountId` của Account-Service.

## 10. Test backend offline

- [ ] Khi Dashboard đang có dữ liệu session, tắt API Gateway hoặc một service nghiệp vụ.
- [ ] Quay lại Dashboard và nhấn làm mới.
- [ ] Thử một giao dịch.

Kết quả mong đợi:

- Dashboard vẫn hiển thị số tài khoản/số dư cache và trạng thái offline nhẹ.
- Màn hình giao dịch hiển thị “Không kết nối được server”.
- Nút thao tác được bật lại sau lỗi.
- App không crash và Logcat có chi tiết lỗi dưới tag Activity tương ứng.
