# Chạy BankingApp_Mobile ở môi trường local

Tài liệu này bám theo cấu hình hiện có trong repo. Không cần database dump: tạo năm database rỗng trước, sau đó Hibernate tự tạo/cập nhật bảng vì các service JPA đều dùng `ddl-auto: update`.

## 1. Cấu hình đã kiểm tra

Repo có 7 backend Spring Boot độc lập và 1 Android app.

| Folder | `spring.application.name` | Port | Database | Tài khoản MySQL | `ddl-auto` | Eureka |
| --- | --- | ---: | --- | --- | --- | --- |
| `Service-Registry` | `SERVICE-REGISTRY` | 8761 | Không dùng | Không dùng | Không dùng | Server tại `http://localhost:8761/eureka` |
| `User-Service` | `user-service` | 8082 | `user_service` | `root` / `12345678` | `update` | URL mặc định của Eureka client: `http://localhost:8761/eureka/` |
| `Account-Service` | `account-service` | 8081 | `account_service` | `root` / `12345678` | `update` | URL mặc định của Eureka client: `http://localhost:8761/eureka/` |
| `Sequence-Generator` | `sequence-generator` | 8083 | `sequence_generator` | `root` / `12345678` | `update` | URL mặc định của Eureka client: `http://localhost:8761/eureka/` |
| `Transaction-Service` | `transaction-service` | 8084 | `transaction_service` | `root` / `12345678` | `update` | URL mặc định của Eureka client: `http://localhost:8761/eureka/` |
| `Fund-Transfer` | `fund-transfer-service` | 8085 | `fund_transfer_service` | `root` / `12345678` | `update` | URL mặc định của Eureka client: `http://localhost:8761/eureka/` |
| `API-Gateway` | `api-gateway` | 8080 | Không dùng | Không dùng | Không dùng | Khai báo rõ `http://localhost:8761/eureka` |

Mật khẩu mặc định hiện là `12345678`. Riêng `User-Service` cho phép ghi đè bằng các biến `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_DB_NAME`, `MYSQL_USER`, `MYSQL_PASSWORD`.

API Gateway đang có các route:

| Path | Service đích |
| --- | --- |
| `/api/users/**` | `user-service` |
| `/api/fund-transfers/**` | `fund-transfer-service` |
| `/accounts/**` | `account-service` |
| `/sequence/**` | `sequence-generator` |
| `/transactions/**` | `transaction-service` |
| `/fund-transfers/**` | `fund-transfer-service` |

Hai route fund transfer dùng cùng `id` trong YAML. Repo trước đây đã chạy và Android dùng `/fund-transfers/**`, nên tài liệu này chỉ ghi nhận, không thay đổi route hay logic.

## 2. Yêu cầu môi trường

- JDK 17. Các `pom.xml` backend và Android đều đặt source/target Java 17.
- Maven 3.8+ cài global và có lệnh `mvn` trong `PATH`.
- MySQL chạy tại `localhost:3306`.
- Android Studio, Android SDK Platform 35 và một Android Emulator.
- Android app dùng Gradle Wrapper có sẵn trong `BankingMobileApp`.
- Keycloak tại `http://localhost:8571` là bắt buộc cho đăng ký, đăng nhập, làm mới token và đăng xuất.

### Cấu hình Keycloak cho xác thực thật

Keycloak là thành phần bắt buộc của bản chính, không phải nhánh demo. `User-Service` luôn tạo và đọc user qua Keycloak; không có cờ tắt hoặc fallback local. Nếu Keycloak chưa chạy tại `http://localhost:8571`, thao tác đăng ký từ Android sẽ lỗi kết nối và không thể hoàn tất.

#### Khởi động Keycloak local tại port 8571

Project này chạy Keycloak bằng ZIP trên Windows, không dùng Docker và không cần `docker-compose-keycloak.yml`. Guide chính nằm ở [`RUN_KEYCLOAK_WINDOWS.md`](./RUN_KEYCLOAK_WINDOWS.md).

```powershell
cd C:\Tools\keycloak

$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

$env:KEYCLOAK_ADMIN = "admin"
$env:KEYCLOAK_ADMIN_PASSWORD = "admin"

.\bin\kc.bat start-dev --http-port=8571
```

Tải Keycloak Server ZIP từ trang chính thức [https://www.keycloak.org/downloads](https://www.keycloak.org/downloads), giải nén và đổi tên folder thành `C:\Tools\keycloak` trước khi chạy lệnh trên. Nếu ZIP đang bị lồng một cấp như `C:\Tools\keycloak\keycloak-26.6.4\bin\kc.bat`, hãy `cd C:\Tools\keycloak\keycloak-26.6.4` hoặc di chuyển nội dung folder version ra trực tiếp `C:\Tools\keycloak`. Giữ terminal này chạy trong lúc sử dụng app. Sau khi admin đầu tiên đã được tạo, các biến bootstrap admin không cần dùng để tạo lại tài khoản ở mỗi lần khởi động.

Lệnh `start-dev` chỉ cấu hình server Keycloak cho môi trường local; luồng đăng ký, credential, token và service account vẫn là Keycloak thật. Khi triển khai production, chạy Keycloak bằng `start`, HTTPS, hostname cố định, database bền vững và secret manager; không cần đổi logic auth của Android hoặc User-Service.

Kiểm tra endpoint realm sau khi hoàn tất setup:

```powershell
Invoke-RestMethod http://localhost:8571/realms/banking-service/.well-known/openid-configuration
```

#### Tạo realm và confidential client

1. Mở `http://localhost:8571` và chọn `Administration Console`.
2. Đăng nhập tài khoản admin vừa tạo.
3. Chọn `Create Realm`, nhập realm name `banking-service`, rồi lưu.
4. Trong realm `banking-service`, mở `Clients` > `Create client`.
5. Chọn `OpenID Connect`, nhập Client ID `banking-service-api-client`, rồi tiếp tục.
6. Cấu hình client:
   - `Client authentication`: **ON**.
   - `Service accounts roles`: **ON** vì User-Service dùng client credentials để gọi Admin API.
   - `Direct access grants`: **ON** vì endpoint login backend đổi email/password lấy token Keycloak.
   - `Authorization`: **OFF**, trừ khi sau này project thật sự dùng Authorization Services.
7. Lưu client. Vào tab `Service account roles` > `Assign role` > lọc theo client.
8. Chọn client roles của `realm-management` và gán `manage-users`. Nếu bản Keycloak không tự kéo các quyền composite cần thiết, gán thêm `view-users` và `query-users`. Không cần gán `realm-admin`.
9. Vào tab `Credentials`, copy `Client Secret`.
10. Dán secret khi script `run-user-service.ps1` hỏi. Không ghi secret vào YAML, script, README, commit hoặc lịch sử lệnh.

Client phải có `Client authentication`, service account và quyền quản lý user; chỉ bật `Credentials` mà không gán role thì token client có thể được cấp nhưng lệnh tạo/tìm user vẫn bị Keycloak từ chối `403`.

Không lưu client secret vào Git. Lấy secret ở Keycloak Admin Console, rồi khai báo trong terminal chạy User-Service:

1. Mở Keycloak Admin Console tại `http://localhost:8571`.
2. Chọn realm `banking-service`.
3. Vào `Clients` và chọn `banking-service-api-client`.
4. Mở tab `Credentials`.
5. Copy `Client Secret`; không dán giá trị này vào file trong repo hoặc commit lên Git.

```powershell
cd C:\Users\Asus\StudioProjects\BankingApp_Mobile

$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

$env:KEYCLOAK_URL = 'http://localhost:8571'
$env:KEYCLOAK_REALM = 'banking-service'
$env:KEYCLOAK_CLIENT_ID = 'banking-service-api-client'
$env:KEYCLOAK_CLIENT_SECRET = 'PASTE_REAL_SECRET_HERE'

.\mvnw.cmd -f .\User-Service\pom.xml spring-boot:run
```

`PASTE_REAL_SECRET_HERE` chỉ là vị trí cần dán secret thật trong terminal, không phải giá trị cấu hình để lưu vào Git. Biến môi trường chỉ có hiệu lực trong phiên PowerShell hiện tại.

Repo cũng có script hỏi secret khi chạy và không lưu secret vào file:

```powershell
cd C:\Users\Asus\StudioProjects\BankingApp_Mobile
.\run-user-service.ps1
```

Script sẽ dừng trước khi chạy Maven nếu secret bị bỏ trống hoặc không kết nối được Keycloak. Khi User-Service kết thúc, script xóa `KEYCLOAK_CLIENT_SECRET` khỏi môi trường của tiến trình script.

Kiểm tra biến đã được set trong terminal chạy thủ công:

```powershell
echo $env:KEYCLOAK_CLIENT_SECRET
```

Lệnh trên sẽ in secret ra màn hình; chỉ dùng để kiểm tra cục bộ và không chia sẻ/chụp lại output. Nếu không set `KEYCLOAK_CLIENT_SECRET`, Spring sẽ báo lỗi placeholder và User-Service không start. Nếu Keycloak chưa chạy, realm/client không tồn tại hoặc secret sai, service có thể start nhưng đăng ký/đăng nhập sẽ lỗi khi gọi Keycloak.

Flow public chỉ gồm đăng ký, đăng nhập, làm mới token và yêu cầu đặt lại mật khẩu. Các API tài khoản, giao dịch, chuyển tiền và dữ liệu user khác đều yêu cầu header `Authorization: Bearer <access_token>`.

Để nút Quên mật khẩu gửi được email, cấu hình SMTP trong `Realm settings > Email` của Keycloak và kiểm tra bằng nút test connection.

#### Kiểm tra đăng ký end-to-end

1. Chạy Keycloak và xác nhận URL discovery của realm trả JSON.
2. Chạy MySQL, Service Registry và các service nghiệp vụ cần thiết.
3. Từ root repo, chạy `./run-user-service.ps1`, nhập client secret thật và chờ `User-Service` đăng ký `UP` trên Eureka.
4. Chạy API Gateway mới nhất và Android app, sau đó xóa app data để tránh session cũ.
5. Trên Android chọn Đăng ký và nhập email chưa tồn tại.
6. Trong Keycloak Admin Console, mở realm `banking-service` > `Users`; email mới phải xuất hiện.
7. Kiểm tra database `user_service`; profile tương ứng phải có `authId` trùng ID của user Keycloak.
8. Android phải nhận response thành công có `userId`; log User-Service không còn `Connect to localhost:8571 failed` hoặc `RESTEASY004655`.

Nếu user có trong Keycloak nhưng không có trong MySQL, kiểm tra lỗi database sau bước gọi Admin API trước khi thử lại. Nếu nhận `401`, kiểm tra client secret; nếu nhận `403`, kiểm tra service-account roles; nếu `Connection refused`, Keycloak chưa lắng nghe ở port 8571.

### Java, Spring Boot và Maven của từng backend

| Folder | Java | Spring Boot parent | Maven Compiler Plugin | Maven Wrapper |
| --- | ---: | ---: | ---: | --- |
| `Service-Registry` | 17 | 2.7.14 | 3.11.0 | Không có |
| `API-Gateway` | 17 | 2.7.14 | 3.11.0 | Không có |
| `User-Service` | 17 | 2.7.14 | 3.11.0 | Không có |
| `Account-Service` | 17 | 2.7.15 | 3.11.0 | Không có |
| `Sequence-Generator` | 17 | 2.7.15 | 3.11.0 | Không có |
| `Transaction-Service` | 17 | 2.7.15 | 3.11.0 | Không có |
| `Fund-Transfer` | 17 | 2.7.14 | 3.11.0 | Không có |

Không service nào có `mvnw`, `mvnw.cmd` hoặc thư mục `.mvn/wrapper`. Các `pom.xml` cũng không khóa Maven core version bằng Maven Enforcer, vì vậy hướng dẫn này dùng Maven cài global; Maven 3.8+ là lựa chọn phù hợp với Spring Boot 2.7.x.

Kiểm tra môi trường trong PowerShell:

```powershell
java -version
mvn -version
where.exe mvn
```

Cả `java -version` và phần Java home trong `mvn -version` nên trỏ tới JDK 17. Nếu đã tải và giải nén Maven nhưng PowerShell chưa nhận lệnh, có thể cấu hình cho phiên terminal hiện tại, ví dụ:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
$env:MAVEN_HOME = 'C:\Tools\apache-maven-3.9.x'
$env:Path = "$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:Path"
mvn -version
```

Thay hai đường dẫn mẫu bằng vị trí cài thực tế. Để dùng lâu dài, thêm `%JAVA_HOME%\bin` và thư mục `bin` của Maven vào biến môi trường `Path` của Windows, sau đó mở terminal mới.

## 3. Tạo database tạm

Từ root repo, chạy một trong các cách sau và nhập mật khẩu MySQL khi được hỏi:

Command Prompt hoặc Git Bash:

```sh
mysql -u root -p < init-temp-databases.sql
```

PowerShell:

```powershell
Get-Content -Raw .\init-temp-databases.sql | mysql -u root -p
```

Nếu `mysql` chưa có trong `PATH`, tìm `mysql.exe` trong thư mục cài MySQL, thường là một trong các vị trí:

```text
C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe
C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe
C:\xampp\mysql\bin\mysql.exe
```

Sau đó chạy bằng đường dẫn đầy đủ:

```powershell
$mysql = 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe'
$sql = (Resolve-Path .\init-temp-databases.sql).Path.Replace('\', '/')
& $mysql -u root -p -e "source $sql"
```

Hoặc thêm thư mục chứa `mysql.exe` vào `PATH` của phiên PowerShell hiện tại:

```powershell
$env:Path = "C:\Program Files\MySQL\MySQL Server 8.0\bin;$env:Path"
mysql --version
```

Muốn cấu hình lâu dài, thêm cùng thư mục `bin` đó vào biến môi trường `Path` của Windows rồi mở terminal mới. Không thêm chính file `mysql.exe` vào `Path`; chỉ thêm thư mục chứa nó.

Có thể kiểm tra kết quả:

```powershell
mysql -u root -p -e "SHOW DATABASES;"
```

Nếu mật khẩu `root` trên máy không phải `123456`, có thể truyền cấu hình lúc chạy từng service thay vì sửa logic:

```powershell
mvn spring-boot:run '-Dspring-boot.run.arguments=--spring.datasource.password=MAT_KHAU_CUA_BAN'
```

## 4. Build và chạy backend

Mở PowerShell tại root `BankingApp_Mobile`. Vì repo không có Maven Wrapper, tất cả lệnh sau dùng Maven global. Có thể build lần lượt cả 7 backend trước khi chạy:

```powershell
$services = @(
    'Service-Registry',
    'User-Service',
    'Sequence-Generator',
    'Account-Service',
    'Transaction-Service',
    'Fund-Transfer',
    'API-Gateway'
)

foreach ($service in $services) {
    mvn -f ".\$service\pom.xml" clean package -DskipTests
    if ($LASTEXITCODE -ne 0) { throw "Build failed: $service" }
}
```

Mỗi service đang chạy phải giữ một cửa sổ PowerShell riêng. Các lệnh dưới đây đều giả sử terminal đang đứng tại root repo.

1. Chạy Service Registry trước và chờ trang `http://localhost:8761` mở được.

   ```powershell
   mvn -f .\Service-Registry\pom.xml spring-boot:run
   ```

2. Chạy các service nghiệp vụ. Database phải được tạo trước bước này.

   ```powershell
   $env:KEYCLOAK_CLIENT_SECRET = 'PASTE_REAL_SECRET_HERE'
   mvn -f .\User-Service\pom.xml spring-boot:run
   ```

   ```powershell
   mvn -f .\Sequence-Generator\pom.xml spring-boot:run
   ```

   ```powershell
   mvn -f .\Account-Service\pom.xml spring-boot:run
   ```

   ```powershell
   mvn -f .\Transaction-Service\pom.xml spring-boot:run
   ```

   ```powershell
   mvn -f .\Fund-Transfer\pom.xml spring-boot:run
   ```

3. Khi các service đã xuất hiện trên Eureka, chạy API Gateway cuối cùng.

   ```powershell
   mvn -f .\API-Gateway\pom.xml spring-boot:run
   ```

4. Xác nhận Gateway ở port 8080:

   ```powershell
   curl.exe http://localhost:8080/actuator/health
   curl.exe "http://localhost:8080/transactions?accountId=test"
   ```

Lệnh thứ hai phải trả `401` khi không có token; đây là hành vi đúng. Nếu Gateway trả `503`, kiểm tra service đích đã đăng ký trên Eureka và tên service có khớp bảng cấu hình ở trên hay chưa.

## 5. Test nhanh bằng Postman

Import collection:

`postman_collection/Banking Core Services.postman_collection.json`

Collection đã đặt `base_url` là `http://localhost:8080` và có request cho user, account, sequence, transaction và fund transfer. Những request đăng ký/duyệt user cần Keycloak realm/client đúng như `User-Service/src/main/resources/application.yml`.

Có thể thử endpoint không cần dữ liệu seed:

```powershell
curl.exe -X POST http://localhost:8080/sequence
```

## 6. Chạy Android

1. Mở riêng folder `BankingMobileApp` bằng Android Studio.
2. Chờ Gradle sync và bảo đảm SDK Platform 35 cùng Build-Tools 35.0.0 đã được cài.
3. Khởi động Android Emulator.
4. Chạy cấu hình `app`.

`BankingMobileApp/local.properties` là cấu hình riêng của từng máy và không còn được Git track, vì vậy pull code sẽ không ghi đè đường dẫn SDK bằng username của máy khác. Sau khi clone/pull trên máy mới, tạo lại file tự động từ SDK mặc định `%LOCALAPPDATA%\Android\Sdk`:

```powershell
cd C:\Users\Asus\StudioProjects\BankingApp_Mobile
powershell -NoProfile -ExecutionPolicy Bypass -File .\setup-android-sdk.ps1
```

Nếu SDK nằm ở vị trí khác:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\setup-android-sdk.ps1 -SdkPath "D:\Android\Sdk"
```

Script kiểm tra SDK Platform 35 và Build-Tools 35.0.0 trước khi tạo `BankingMobileApp/local.properties`. Có thể build lại bằng:

```powershell
cd .\BankingMobileApp
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleDebug
```

`ApiClient.java` đang dùng đúng base URL:

```text
http://10.0.2.2:8080/
```

Không đổi URL này khi dùng Android Emulator vì `10.0.2.2` ánh xạ tới `localhost` của máy chạy backend. Manifest đã có quyền Internet và `android:usesCleartextTraffic="true"` để gọi HTTP local.

Nếu chạy bằng điện thoại thật, đổi `10.0.2.2` thành IP LAN của máy chạy backend, ví dụ `http://192.168.1.10:8080/`; điện thoại và máy phải cùng mạng, đồng thời firewall phải cho phép port 8080.

Có thể build APK debug từ terminal:

```powershell
cd .\BankingMobileApp
.\gradlew.bat assembleDebug
```

## 7. Seed và các điểm cần biết

Không tạo `seed-temp-data.sql` trong bước này. Lý do:

- Hibernate phải chạy ít nhất một lần để tạo schema vật lý theo naming strategy thực tế.
- User hợp lệ còn liên kết với `authId` của Keycloak. Chèn user trực tiếp vào MySQL có thể tạo dữ liệu không nhất quán.
- Các bảng account, transaction và fund transfer có quan hệ nghiệp vụ; seed đoán tay dễ làm giao diện test sai.

Quy trình an toàn là: tạo database rỗng, chạy toàn bộ backend một lần để Hibernate tạo bảng, cấu hình Keycloak nếu cần luồng đăng ký, rồi tạo dữ liệu qua API/Postman.

## 8. Kết quả kiểm tra trên máy hiện tại

- Android đã build thành công bằng `gradlew.bat assembleDebug` sau khi sửa đường dẫn SDK local từ tài khoản máy cũ sang SDK hiện tại.
- Môi trường terminal hiện tại đang dùng Java 25.0.1, trong khi repo đặt Java 17; nên chuyển `JAVA_HOME` và `Path` về JDK 17 trước khi chạy backend để đúng toolchain của project.
- Terminal chưa có `mvn` và `mysql` trong `PATH`. Các vị trí MySQL phổ biến nêu trên cũng chưa có `mysql.exe`, nên chưa thể build/chạy end-to-end backend và thực thi SQL từ terminal này. Đây là thiếu công cụ local, không phải bằng chứng repo bị hỏng.
- Repo không có Maven Wrapper; cần cài Maven global hoặc chủ động bổ sung wrapper trong một task riêng.
- Không thay đổi package Android, port, API path, route, Entity, logic backend hoặc UI.
