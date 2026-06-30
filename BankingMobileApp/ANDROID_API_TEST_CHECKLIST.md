Bạn hãy tiếp tục trong repo `BankingApp_Mobile`, đọc cả Android app và backend services trước khi sửa.

Bối cảnh hiện tại:

* Backend local đã chạy thành công qua API Gateway `http://localhost:8080`.
* Android emulator dùng base URL `http://10.0.2.2:8080/`.
* App đã có `AppSession` lưu `userId`, `userEmail`, `accountId`, `accountNumber`, `accountBalance`.
* UI đã theo phong cách mobile banking.
* Vấn đề hiện tại: app có thể vào Dashboard mà chưa qua đăng nhập rõ ràng.
* Tôi muốn: người dùng đã có tài khoản thì phải đăng nhập trước, đăng nhập thành công mới vào Dashboard.
* Không được tự vào Dashboard chỉ vì có account/session cũ nếu chưa có trạng thái đã đăng nhập hợp lệ.
* Không đổi package name.
* Không đổi base URL.
* Giữ Java/XML native Android.

Mục tiêu chính:
Thiết kế lại flow đăng nhập của Android app dựa trên khả năng thật của backend hiện tại.

Yêu cầu quan trọng nhất:
Trước khi tạo login mới, hãy kiểm tra backend hiện tại có logic đăng nhập/auth dùng được không.

Phần A — Kiểm tra login/auth hiện có trong backend

Đọc các phần sau:

* `User-Service`
* `API-Gateway`
* Keycloak config nếu có
* Security config
* Controller liên quan user/auth
* DTO request/response liên quan register/login/auth
* Retrofit `BankingApi.java` bên Android

Cần xác định rõ:

1. Backend hiện có endpoint login không?
2. Nếu có, endpoint path là gì?
3. Login dùng email/password, username/password, User ID, Keycloak token hay cách khác?
4. Response login có trả token/userId/email/status không?
5. API Gateway hiện có bắt auth/token không hay đang `permitAll`?
6. Android hiện đã có model/request cho login chưa?
7. Register hiện có tạo user trong Keycloak không?
8. User sau register có bị `PENDING` hoặc disabled cho đến khi duyệt không?
9. Với trạng thái backend hiện tại, login thật có thể dùng ổn để demo không?

Sau khi kiểm tra, chọn một trong hai hướng:

Hướng 1 — Dùng login thật nếu backend đã có và dùng được

* Nếu backend có endpoint login rõ ràng và có thể dùng ổn:

  * Tạo/nối `LoginActivity` với endpoint đó.
  * Login bằng đúng field backend yêu cầu, ví dụ email/password hoặc username/password.
  * Nếu response có token thì lưu token vào `AppSession`.
  * Nếu response có userId thì lưu userId.
  * Nếu cần gọi tiếp account API để lấy account, gọi sau login.
  * Chỉ vào Dashboard khi login thành công.
  * Nếu login sai, hiển thị lỗi thân thiện.
  * Nếu user bị PENDING/disabled, hiển thị rõ trạng thái.
* Không tự tạo logic login giả nếu login thật đã dùng được.

Hướng 2 — Tạo login demo nếu backend chưa có login thật hoặc login hiện tại chưa phù hợp
Chỉ dùng hướng này nếu backend không có endpoint login ổn định cho Android hoặc Keycloak flow chưa hoàn chỉnh.

* Tạo login demo bằng `User ID`.
* Giao diện phải ghi rõ: “Phiên bản demo sử dụng User ID để đăng nhập”.
* Người dùng nhập User ID.
* App gọi endpoint đang có để tìm account theo User ID, ví dụ `GET /accounts/{userId}` nếu đúng.
* Nếu tìm được account:

  * lưu `userId`, `accountId`, `accountNumber`, `accountBalance`.
  * set trạng thái đăng nhập trong `AppSession`, ví dụ `isLoggedIn = true`.
  * vào Dashboard.
* Nếu không tìm thấy account:

  * báo “Không tìm thấy tài khoản cho User ID này. Vui lòng mở tài khoản trước.”
  * không vào Dashboard.
* Không hardcode userId.
* Không tự login bằng tài khoản cũ nếu chưa có `isLoggedIn = true`.

Phần B — Tạo/sửa LoginActivity

Tạo hoặc cập nhật:

* `LoginActivity.java`
* `activity_login.xml`

Giao diện:

* Đồng bộ theme mobile banking hiện tại.
* Có tiêu đề “Đăng nhập”.
* Nếu dùng login thật:

  * input theo backend yêu cầu, ví dụ email/password.
* Nếu dùng login demo:

  * input User ID.
  * text phụ: “Phiên bản demo sử dụng User ID để đăng nhập”.
* Có nút “Đăng nhập”.
* Có link/nút “Chưa có hồ sơ? Đăng ký”.
* Có link/nút “Đã có hồ sơ nhưng chưa có tài khoản? Mở tài khoản” nếu hợp lý.

Logic:

* Validate input không rỗng.
* Disable nút khi gọi API.
* Enable lại sau khi API xong.
* Thành công mới vào `MainActivity`.
* Thất bại không vào Dashboard.
* Backend offline hiển thị “Không kết nối được server”.
* Không crash khi response null.

Phần C — AppSession

Cập nhật `AppSession` nếu cần:

* `saveLoginState(Context, boolean)`
* `isLoggedIn(Context)`
* `clearSession(Context)`
* Nếu có token:

  * `saveAuthToken(Context, String)`
  * `getAuthToken(Context)`
  * `clearAuthToken(Context)`

Quy tắc:

* `hasUser()` hoặc `hasAccount()` không đồng nghĩa đã đăng nhập.
* Dashboard chỉ được mở khi:

  * `isLoggedIn() == true`
  * và có thông tin user/account hợp lệ tùy flow.
* Logout phải set `isLoggedIn = false` và xóa session/token nếu có.

Phần D — AndroidManifest

* Đặt `LoginActivity` làm launcher Activity.
* `MainActivity` không còn là launcher.
* Khai báo đầy đủ Activity mới.

Phần E — MainActivity/Dashboard

Trong `MainActivity.onCreate()`:

* Kiểm tra `AppSession.isLoggedIn()`.
* Nếu chưa login:

  * chuyển về `LoginActivity`.
  * gọi `finish()`.
  * không render Dashboard.
* Nếu đã login:

  * render Dashboard như hiện tại.
  * sync account/balance nếu có.

Dashboard:

* Xóa/ẩn khu vực “Dành cho khách hàng mới”.
* Xóa nút “Đăng ký hồ sơ khách hàng” khỏi Dashboard.
* Thêm nút/icon “Đăng xuất”.
* Khi bấm Đăng xuất:

  * gọi `AppSession.clearSession(context)`.
  * mở `LoginActivity`.
  * dùng flags:

    * `Intent.FLAG_ACTIVITY_NEW_TASK`
    * `Intent.FLAG_ACTIVITY_CLEAR_TASK`

Phần F — RegisterActivity

* RegisterActivity chỉ dùng cho người chưa có hồ sơ.
* Không tự vào Dashboard sau khi đăng ký.
* Nếu backend register response trả userId:

  * hiển thị userId cho người dùng.
  * có thể hướng dẫn đăng nhập bằng userId hoặc email/password tùy login flow đã chọn.
* Nếu backend không trả userId:

  * không đoán bừa.
  * hiển thị hướng dẫn rõ.
* Không tạo session giả.
* Không set `isLoggedIn = true` sau register, trừ khi backend login thật trả token/session hợp lệ ngay sau register.

Phần G — AccountActivity

* Nếu user đã login nhưng chưa có account, có thể cho mở tài khoản.
* Sau khi tạo/tìm tài khoản thành công:

  * lưu account vào session.
  * nếu người dùng đã login hợp lệ, có thể vào Dashboard.
  * nếu chưa login, yêu cầu quay lại Login.
* Không tự bỏ qua Login.

Phần H — Bỏ hard-code demo không phù hợp

Tìm và loại bỏ:

* email demo set sẵn trong input
* password demo set sẵn trong input
* userId/accountNumber mặc định
* số tiền mặc định
* dữ liệu giả được set thẳng vào session

Giữ lại hint/placeholder nếu chỉ là hướng dẫn nhập.

Phần I — Error message

* Việt hóa message còn tiếng Anh trong UI helper nếu có.
* Không chỉ hiển thị `HTTP 400/404`.
* Map lỗi cơ bản:

  * 400: “Dữ liệu không hợp lệ”
  * 401/403: “Thông tin đăng nhập không hợp lệ hoặc chưa được cấp quyền”
  * 404: “Không tìm thấy dữ liệu”
  * 409: “Dữ liệu đã tồn tại hoặc xung đột”
  * 500: “Lỗi hệ thống, vui lòng thử lại”
  * network failure: “Không kết nối được server”
* Log chi tiết bằng `Log.e`.

Phần J — Kiểm tra

Sau khi sửa:

* Chạy `assembleDebug`.
* Chạy `lintDebug` bằng JDK 17 nếu có thể.
* Nếu backend login thật được dùng, ghi rõ endpoint login nào đã dùng.
* Nếu login demo được dùng, ghi rõ vì sao không dùng login thật.

Test runtime mong muốn:

1. Clear app data.
2. Mở app.
3. App phải vào LoginActivity, không vào Dashboard.
4. Nhập sai thông tin đăng nhập → không vào Dashboard.
5. Nhập thông tin hợp lệ → vào Dashboard.
6. Dashboard không còn nút đăng ký khách hàng.
7. Bấm Logout → quay lại Login.
8. Sau Logout, bấm Back không quay lại Dashboard.
9. Đóng/mở lại app:

   * nếu vẫn còn `isLoggedIn = true` thì có thể vào Dashboard.
   * nếu đã logout thì vào Login.

Báo cáo cuối cùng cần ghi:

* Backend hiện có login thật hay không.
* Nếu có, login dùng endpoint nào và field nào.
* Nếu không dùng login thật, lý do là gì.
* Flow login mới trong Android.
* File đã tạo.
* File đã sửa.
* Launcher Activity mới.
* Điều kiện để vào Dashboard.
* Logout xử lý thế nào.
* RegisterActivity sau sửa hoạt động thế nào.
* Kết quả `assembleDebug`.
* Kết quả `lintDebug`.
* Giới hạn còn lại, đặc biệt nếu đây vẫn chỉ là login demo, chưa phải authentication bảo mật thật.
