# Chạy BankingApp_Mobile ở môi trường local

Tài liệu này bám theo cấu hình hiện có trong repo. Không cần database dump: tạo năm database rỗng trước, sau đó Hibernate tự tạo/cập nhật bảng vì các service JPA đều dùng `ddl-auto: update`.

## 1. Cấu hình đã kiểm tra

Repo có 7 backend Spring Boot độc lập và 1 Android app.

| Folder | `spring.application.name` | Port | Database | Tài khoản MySQL | `ddl-auto` | Eureka |
| --- | --- | ---: | --- | --- | --- | --- |
| `Service-Registry` | `SERVICE-REGISTRY` | 8761 | Không dùng | Không dùng | Không dùng | Server tại `http://localhost:8761/eureka` |
| `User-Service` | `user-service` | 8082 | `user_service` | `root` / `123456` | `update` | URL mặc định của Eureka client: `http://localhost:8761/eureka/` |
| `Account-Service` | `account-service` | 8081 | `account_service` | `root` / `123456` | `update` | URL mặc định của Eureka client: `http://localhost:8761/eureka/` |
| `Sequence-Generator` | `sequence-generator` | 8083 | `sequence_generator` | `root` / `123456` | `update` | URL mặc định của Eureka client: `http://localhost:8761/eureka/` |
| `Transaction-Service` | `transaction-service` | 8084 | `transaction_service` | `root` / `123456` | `update` | URL mặc định của Eureka client: `http://localhost:8761/eureka/` |
| `Fund-Transfer` | `fund-transfer-service` | 8085 | `fund_transfer_service` | `root` / `123456` | `update` | URL mặc định của Eureka client: `http://localhost:8761/eureka/` |
| `API-Gateway` | `api-gateway` | 8080 | Không dùng | Không dùng | Không dùng | Khai báo rõ `http://localhost:8761/eureka` |

Lưu ý: mật khẩu mặc định thực tế trong tất cả `application.yml` có datasource là `123456`, không phải `12345678`. Riêng `User-Service` cho phép ghi đè bằng các biến `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_DB_NAME`, `MYSQL_USER`, `MYSQL_PASSWORD`.

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
- Keycloak tại `http://localhost:8571` nếu cần đăng ký/duyệt người dùng. `User-Service` gọi trực tiếp Keycloak realm `banking-service`; chỉ có MySQL là chưa đủ cho luồng đăng ký.

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

Lệnh thứ hai dự kiến trả về danh sách rỗng khi database chưa có giao dịch. Nếu Gateway trả `503`, kiểm tra service đích đã đăng ký trên Eureka và tên service có khớp bảng cấu hình ở trên hay chưa.

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
2. Chờ Gradle sync và bảo đảm SDK Platform 35 đã được cài. `local.properties` phải trỏ tới Android SDK của chính máy đang chạy.
3. Khởi động Android Emulator.
4. Chạy cấu hình `app`.

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
