# Cháº¡y BankingApp_Mobile á»Ÿ mÃ´i trÆ°á»ng local

TÃ i liá»‡u nÃ y bÃ¡m theo cáº¥u hÃ¬nh hiá»‡n cÃ³ trong repo. KhÃ´ng cáº§n database dump: táº¡o nÄƒm database rá»—ng trÆ°á»›c, sau Ä‘Ã³ Hibernate tá»± táº¡o/cáº­p nháº­t báº£ng vÃ¬ cÃ¡c service JPA Ä‘á»u dÃ¹ng `ddl-auto: update`.

## 1. Cáº¥u hÃ¬nh Ä‘Ã£ kiá»ƒm tra

Repo cÃ³ 7 backend Spring Boot Ä‘á»™c láº­p vÃ  1 Android app.

| Folder | `spring.application.name` | Port | Database | TÃ i khoáº£n MySQL | `ddl-auto` | Eureka |
| --- | --- | ---: | --- | --- | --- | --- |
| `Service-Registry` | `SERVICE-REGISTRY` | 8761 | KhÃ´ng dÃ¹ng | KhÃ´ng dÃ¹ng | KhÃ´ng dÃ¹ng | Server táº¡i `http://localhost:8761/eureka` |
| `User-Service` | `user-service` | 8082 | `user_service` | `root` / `123456` | `update` | URL máº·c Ä‘á»‹nh cá»§a Eureka client: `http://localhost:8761/eureka/` |
| `Account-Service` | `account-service` | 8081 | `account_service` | `root` / `123456` | `update` | URL máº·c Ä‘á»‹nh cá»§a Eureka client: `http://localhost:8761/eureka/` |
| `Sequence-Generator` | `sequence-generator` | 8083 | `sequence_generator` | `root` / `123456` | `update` | URL máº·c Ä‘á»‹nh cá»§a Eureka client: `http://localhost:8761/eureka/` |
| `Transaction-Service` | `transaction-service` | 8084 | `transaction_service` | `root` / `123456` | `update` | URL máº·c Ä‘á»‹nh cá»§a Eureka client: `http://localhost:8761/eureka/` |
| `Fund-Transfer` | `fund-transfer-service` | 8085 | `fund_transfer_service` | `root` / `123456` | `update` | URL máº·c Ä‘á»‹nh cá»§a Eureka client: `http://localhost:8761/eureka/` |
| `API-Gateway` | `api-gateway` | 8080 | KhÃ´ng dÃ¹ng | KhÃ´ng dÃ¹ng | KhÃ´ng dÃ¹ng | Khai bÃ¡o rÃµ `http://localhost:8761/eureka` |

Máº­t kháº©u máº·c Ä‘á»‹nh hiá»‡n lÃ  `123456`. RiÃªng `User-Service` cho phÃ©p ghi Ä‘Ã¨ báº±ng cÃ¡c biáº¿n `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_DB_NAME`, `MYSQL_USER`, `MYSQL_PASSWORD`.

API Gateway Ä‘ang cÃ³ cÃ¡c route:

| Path | Service Ä‘Ã­ch |
| --- | --- |
| `/api/users/**` | `user-service` |
| `/api/fund-transfers/**` | `fund-transfer-service` |
| `/accounts/**` | `account-service` |
| `/sequence/**` | `sequence-generator` |
| `/transactions/**` | `transaction-service` |
| `/fund-transfers/**` | `fund-transfer-service` |

Hai route fund transfer dÃ¹ng cÃ¹ng `id` trong YAML. Repo trÆ°á»›c Ä‘Ã¢y Ä‘Ã£ cháº¡y vÃ  Android dÃ¹ng `/fund-transfers/**`, nÃªn tÃ i liá»‡u nÃ y chá»‰ ghi nháº­n, khÃ´ng thay Ä‘á»•i route hay logic.

## 2. YÃªu cáº§u mÃ´i trÆ°á»ng

- JDK 17. CÃ¡c `pom.xml` backend vÃ  Android Ä‘á»u Ä‘áº·t source/target Java 17.
- Maven 3.8+ cÃ i global vÃ  cÃ³ lá»‡nh `mvn` trong `PATH`.
- MySQL cháº¡y táº¡i `localhost:3306`.
- Android Studio, Android SDK Platform 35 vÃ  má»™t Android Emulator.
- Android app dÃ¹ng Gradle Wrapper cÃ³ sáºµn trong `BankingMobileApp`.
- Keycloak táº¡i `http://localhost:8571` lÃ  báº¯t buá»™c cho Ä‘Äƒng kÃ½, Ä‘Äƒng nháº­p, lÃ m má»›i token vÃ  Ä‘Äƒng xuáº¥t.

### Cáº¥u hÃ¬nh Keycloak cho xÃ¡c thá»±c tháº­t

Keycloak lÃ  thÃ nh pháº§n báº¯t buá»™c cá»§a báº£n chÃ­nh, khÃ´ng pháº£i nhÃ¡nh demo. `User-Service` luÃ´n táº¡o vÃ  Ä‘á»c user qua Keycloak; khÃ´ng cÃ³ cá» táº¯t hoáº·c fallback local. Náº¿u Keycloak chÆ°a cháº¡y táº¡i `http://localhost:8571`, thao tÃ¡c Ä‘Äƒng kÃ½ tá»« Android sáº½ lá»—i káº¿t ná»‘i vÃ  khÃ´ng thá»ƒ hoÃ n táº¥t.

#### Khá»Ÿi Ä‘á»™ng Keycloak local táº¡i port 8571

Project nÃ y cháº¡y Keycloak báº±ng ZIP trÃªn Windows, khÃ´ng dÃ¹ng Docker vÃ  khÃ´ng cáº§n `docker-compose-keycloak.yml`. Guide chÃ­nh náº±m á»Ÿ [`RUN_KEYCLOAK_WINDOWS.md`](./RUN_KEYCLOAK_WINDOWS.md).

```powershell
cd C:\Tools\keycloak

$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

$env:KEYCLOAK_ADMIN = "admin"
$env:KEYCLOAK_ADMIN_PASSWORD = "admin"

.\bin\kc.bat start-dev --http-port=8571
```

Táº£i Keycloak Server ZIP tá»« trang chÃ­nh thá»©c [https://www.keycloak.org/downloads](https://www.keycloak.org/downloads), giáº£i nÃ©n vÃ  Ä‘á»•i tÃªn folder thÃ nh `C:\Tools\keycloak` trÆ°á»›c khi cháº¡y lá»‡nh trÃªn. Náº¿u ZIP Ä‘ang bá»‹ lá»“ng má»™t cáº¥p nhÆ° `C:\Tools\keycloak\keycloak-26.6.4\bin\kc.bat`, hÃ£y `cd C:\Tools\keycloak\keycloak-26.6.4` hoáº·c di chuyá»ƒn ná»™i dung folder version ra trá»±c tiáº¿p `C:\Tools\keycloak`. Giá»¯ terminal nÃ y cháº¡y trong lÃºc sá»­ dá»¥ng app. Sau khi admin Ä‘áº§u tiÃªn Ä‘Ã£ Ä‘Æ°á»£c táº¡o, cÃ¡c biáº¿n bootstrap admin khÃ´ng cáº§n dÃ¹ng Ä‘á»ƒ táº¡o láº¡i tÃ i khoáº£n á»Ÿ má»—i láº§n khá»Ÿi Ä‘á»™ng.

Lá»‡nh `start-dev` chá»‰ cáº¥u hÃ¬nh server Keycloak cho mÃ´i trÆ°á»ng local; luá»“ng Ä‘Äƒng kÃ½, credential, token vÃ  service account váº«n lÃ  Keycloak tháº­t. Khi triá»ƒn khai production, cháº¡y Keycloak báº±ng `start`, HTTPS, hostname cá»‘ Ä‘á»‹nh, database bá»n vá»¯ng vÃ  secret manager; khÃ´ng cáº§n Ä‘á»•i logic auth cá»§a Android hoáº·c User-Service.

Kiá»ƒm tra endpoint realm sau khi hoÃ n táº¥t setup:

```powershell
Invoke-RestMethod http://localhost:8571/realms/banking-service/.well-known/openid-configuration
```

#### Táº¡o realm vÃ  confidential client

1. Má»Ÿ `http://localhost:8571` vÃ  chá»n `Administration Console`.
2. ÄÄƒng nháº­p tÃ i khoáº£n admin vá»«a táº¡o.
3. Chá»n `Create Realm`, nháº­p realm name `banking-service`, rá»“i lÆ°u.
4. Trong realm `banking-service`, má»Ÿ `Clients` > `Create client`.
5. Chá»n `OpenID Connect`, nháº­p Client ID `banking-service-api-client`, rá»“i tiáº¿p tá»¥c.
6. Cáº¥u hÃ¬nh client:
   - `Client authentication`: **ON**.
   - `Service accounts roles`: **ON** vÃ¬ User-Service dÃ¹ng client credentials Ä‘á»ƒ gá»i Admin API.
   - `Direct access grants`: **ON** vÃ¬ endpoint login backend Ä‘á»•i email/password láº¥y token Keycloak.
   - `Authorization`: **OFF**, trá»« khi sau nÃ y project tháº­t sá»± dÃ¹ng Authorization Services.
7. LÆ°u client. VÃ o tab `Service account roles` > `Assign role` > lá»c theo client.
8. Chá»n client roles cá»§a `realm-management` vÃ  gÃ¡n `manage-users`. Náº¿u báº£n Keycloak khÃ´ng tá»± kÃ©o cÃ¡c quyá»n composite cáº§n thiáº¿t, gÃ¡n thÃªm `view-users` vÃ  `query-users`. KhÃ´ng cáº§n gÃ¡n `realm-admin`.
9. VÃ o tab `Credentials`, copy `Client Secret`.
10. DÃ¡n secret khi script `run-user-service.ps1` há»i. KhÃ´ng ghi secret vÃ o YAML, script, README, commit hoáº·c lá»‹ch sá»­ lá»‡nh.

Client pháº£i cÃ³ `Client authentication`, service account vÃ  quyá»n quáº£n lÃ½ user; chá»‰ báº­t `Credentials` mÃ  khÃ´ng gÃ¡n role thÃ¬ token client cÃ³ thá»ƒ Ä‘Æ°á»£c cáº¥p nhÆ°ng lá»‡nh táº¡o/tÃ¬m user váº«n bá»‹ Keycloak tá»« chá»‘i `403`.

KhÃ´ng lÆ°u client secret vÃ o Git. Láº¥y secret á»Ÿ Keycloak Admin Console, rá»“i khai bÃ¡o trong terminal cháº¡y User-Service:

1. Má»Ÿ Keycloak Admin Console táº¡i `http://localhost:8571`.
2. Chá»n realm `banking-service`.
3. VÃ o `Clients` vÃ  chá»n `banking-service-api-client`.
4. Má»Ÿ tab `Credentials`.
5. Copy `Client Secret`; khÃ´ng dÃ¡n giÃ¡ trá»‹ nÃ y vÃ o file trong repo hoáº·c commit lÃªn Git.

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

`PASTE_REAL_SECRET_HERE` chá»‰ lÃ  vá»‹ trÃ­ cáº§n dÃ¡n secret tháº­t trong terminal, khÃ´ng pháº£i giÃ¡ trá»‹ cáº¥u hÃ¬nh Ä‘á»ƒ lÆ°u vÃ o Git. Biáº¿n mÃ´i trÆ°á»ng chá»‰ cÃ³ hiá»‡u lá»±c trong phiÃªn PowerShell hiá»‡n táº¡i.

Repo cÅ©ng cÃ³ script há»i secret khi cháº¡y vÃ  khÃ´ng lÆ°u secret vÃ o file:

```powershell
cd C:\Users\Asus\StudioProjects\BankingApp_Mobile
.\run-user-service.ps1
```

Script sáº½ dá»«ng trÆ°á»›c khi cháº¡y Maven náº¿u secret bá»‹ bá» trá»‘ng hoáº·c khÃ´ng káº¿t ná»‘i Ä‘Æ°á»£c Keycloak. Khi User-Service káº¿t thÃºc, script xÃ³a `KEYCLOAK_CLIENT_SECRET` khá»i mÃ´i trÆ°á»ng cá»§a tiáº¿n trÃ¬nh script.

Kiá»ƒm tra biáº¿n Ä‘Ã£ Ä‘Æ°á»£c set trong terminal cháº¡y thá»§ cÃ´ng:

```powershell
echo $env:KEYCLOAK_CLIENT_SECRET
```

Lá»‡nh trÃªn sáº½ in secret ra mÃ n hÃ¬nh; chá»‰ dÃ¹ng Ä‘á»ƒ kiá»ƒm tra cá»¥c bá»™ vÃ  khÃ´ng chia sáº»/chá»¥p láº¡i output. Náº¿u khÃ´ng set `KEYCLOAK_CLIENT_SECRET`, Spring sáº½ bÃ¡o lá»—i placeholder vÃ  User-Service khÃ´ng start. Náº¿u Keycloak chÆ°a cháº¡y, realm/client khÃ´ng tá»“n táº¡i hoáº·c secret sai, service cÃ³ thá»ƒ start nhÆ°ng Ä‘Äƒng kÃ½/Ä‘Äƒng nháº­p sáº½ lá»—i khi gá»i Keycloak.

Flow public chá»‰ gá»“m Ä‘Äƒng kÃ½, Ä‘Äƒng nháº­p, lÃ m má»›i token vÃ  yÃªu cáº§u Ä‘áº·t láº¡i máº­t kháº©u. CÃ¡c API tÃ i khoáº£n, giao dá»‹ch, chuyá»ƒn tiá»n vÃ  dá»¯ liá»‡u user khÃ¡c Ä‘á»u yÃªu cáº§u header `Authorization: Bearer <access_token>`.

Äá»ƒ nÃºt QuÃªn máº­t kháº©u gá»­i Ä‘Æ°á»£c email, cáº¥u hÃ¬nh SMTP trong `Realm settings > Email` cá»§a Keycloak vÃ  kiá»ƒm tra báº±ng nÃºt test connection.

#### Kiá»ƒm tra Ä‘Äƒng kÃ½ end-to-end

1. Cháº¡y Keycloak vÃ  xÃ¡c nháº­n URL discovery cá»§a realm tráº£ JSON.
2. Cháº¡y MySQL, Service Registry vÃ  cÃ¡c service nghiá»‡p vá»¥ cáº§n thiáº¿t.
3. Tá»« root repo, cháº¡y `./run-user-service.ps1`, nháº­p client secret tháº­t vÃ  chá» `User-Service` Ä‘Äƒng kÃ½ `UP` trÃªn Eureka.
4. Cháº¡y API Gateway má»›i nháº¥t vÃ  Android app, sau Ä‘Ã³ xÃ³a app data Ä‘á»ƒ trÃ¡nh session cÅ©.
5. TrÃªn Android chá»n ÄÄƒng kÃ½ vÃ  nháº­p email chÆ°a tá»“n táº¡i.
6. Trong Keycloak Admin Console, má»Ÿ realm `banking-service` > `Users`; email má»›i pháº£i xuáº¥t hiá»‡n.
7. Kiá»ƒm tra database `user_service`; profile tÆ°Æ¡ng á»©ng pháº£i cÃ³ `authId` trÃ¹ng ID cá»§a user Keycloak.
8. Android pháº£i nháº­n response thÃ nh cÃ´ng cÃ³ `userId`; log User-Service khÃ´ng cÃ²n `Connect to localhost:8571 failed` hoáº·c `RESTEASY004655`.

Náº¿u user cÃ³ trong Keycloak nhÆ°ng khÃ´ng cÃ³ trong MySQL, kiá»ƒm tra lá»—i database sau bÆ°á»›c gá»i Admin API trÆ°á»›c khi thá»­ láº¡i. Náº¿u nháº­n `401`, kiá»ƒm tra client secret; náº¿u nháº­n `403`, kiá»ƒm tra service-account roles; náº¿u `Connection refused`, Keycloak chÆ°a láº¯ng nghe á»Ÿ port 8571.

### Java, Spring Boot vÃ  Maven cá»§a tá»«ng backend

| Folder | Java | Spring Boot parent | Maven Compiler Plugin | Maven Wrapper |
| --- | ---: | ---: | ---: | --- |
| `Service-Registry` | 17 | 2.7.14 | 3.11.0 | KhÃ´ng cÃ³ |
| `API-Gateway` | 17 | 2.7.14 | 3.11.0 | KhÃ´ng cÃ³ |
| `User-Service` | 17 | 2.7.14 | 3.11.0 | KhÃ´ng cÃ³ |
| `Account-Service` | 17 | 2.7.15 | 3.11.0 | KhÃ´ng cÃ³ |
| `Sequence-Generator` | 17 | 2.7.15 | 3.11.0 | KhÃ´ng cÃ³ |
| `Transaction-Service` | 17 | 2.7.15 | 3.11.0 | KhÃ´ng cÃ³ |
| `Fund-Transfer` | 17 | 2.7.14 | 3.11.0 | KhÃ´ng cÃ³ |

KhÃ´ng service nÃ o cÃ³ `mvnw`, `mvnw.cmd` hoáº·c thÆ° má»¥c `.mvn/wrapper`. CÃ¡c `pom.xml` cÅ©ng khÃ´ng khÃ³a Maven core version báº±ng Maven Enforcer, vÃ¬ váº­y hÆ°á»›ng dáº«n nÃ y dÃ¹ng Maven cÃ i global; Maven 3.8+ lÃ  lá»±a chá»n phÃ¹ há»£p vá»›i Spring Boot 2.7.x.

Kiá»ƒm tra mÃ´i trÆ°á»ng trong PowerShell:

```powershell
java -version
mvn -version
where.exe mvn
```

Cáº£ `java -version` vÃ  pháº§n Java home trong `mvn -version` nÃªn trá» tá»›i JDK 17. Náº¿u Ä‘Ã£ táº£i vÃ  giáº£i nÃ©n Maven nhÆ°ng PowerShell chÆ°a nháº­n lá»‡nh, cÃ³ thá»ƒ cáº¥u hÃ¬nh cho phiÃªn terminal hiá»‡n táº¡i, vÃ­ dá»¥:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
$env:MAVEN_HOME = 'C:\Tools\apache-maven-3.9.x'
$env:Path = "$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:Path"
mvn -version
```

Thay hai Ä‘Æ°á»ng dáº«n máº«u báº±ng vá»‹ trÃ­ cÃ i thá»±c táº¿. Äá»ƒ dÃ¹ng lÃ¢u dÃ i, thÃªm `%JAVA_HOME%\bin` vÃ  thÆ° má»¥c `bin` cá»§a Maven vÃ o biáº¿n mÃ´i trÆ°á»ng `Path` cá»§a Windows, sau Ä‘Ã³ má»Ÿ terminal má»›i.

## 3. Táº¡o database táº¡m

Tá»« root repo, cháº¡y má»™t trong cÃ¡c cÃ¡ch sau vÃ  nháº­p máº­t kháº©u MySQL khi Ä‘Æ°á»£c há»i:

Command Prompt hoáº·c Git Bash:

```sh
mysql -u root -p < init-temp-databases.sql
```

PowerShell:

```powershell
Get-Content -Raw .\init-temp-databases.sql | mysql -u root -p
```

Náº¿u `mysql` chÆ°a cÃ³ trong `PATH`, tÃ¬m `mysql.exe` trong thÆ° má»¥c cÃ i MySQL, thÆ°á»ng lÃ  má»™t trong cÃ¡c vá»‹ trÃ­:

```text
C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe
C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe
C:\xampp\mysql\bin\mysql.exe
```

Sau Ä‘Ã³ cháº¡y báº±ng Ä‘Æ°á»ng dáº«n Ä‘áº§y Ä‘á»§:

```powershell
$mysql = 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe'
$sql = (Resolve-Path .\init-temp-databases.sql).Path.Replace('\', '/')
& $mysql -u root -p -e "source $sql"
```

Hoáº·c thÃªm thÆ° má»¥c chá»©a `mysql.exe` vÃ o `PATH` cá»§a phiÃªn PowerShell hiá»‡n táº¡i:

```powershell
$env:Path = "C:\Program Files\MySQL\MySQL Server 8.0\bin;$env:Path"
mysql --version
```

Muá»‘n cáº¥u hÃ¬nh lÃ¢u dÃ i, thÃªm cÃ¹ng thÆ° má»¥c `bin` Ä‘Ã³ vÃ o biáº¿n mÃ´i trÆ°á»ng `Path` cá»§a Windows rá»“i má»Ÿ terminal má»›i. KhÃ´ng thÃªm chÃ­nh file `mysql.exe` vÃ o `Path`; chá»‰ thÃªm thÆ° má»¥c chá»©a nÃ³.

CÃ³ thá»ƒ kiá»ƒm tra káº¿t quáº£:

```powershell
mysql -u root -p -e "SHOW DATABASES;"
```

Náº¿u máº­t kháº©u `root` trÃªn mÃ¡y khÃ´ng pháº£i `123456`, cÃ³ thá»ƒ truyá»n cáº¥u hÃ¬nh lÃºc cháº¡y tá»«ng service thay vÃ¬ sá»­a logic:

```powershell
mvn spring-boot:run '-Dspring-boot.run.arguments=--spring.datasource.password=MAT_KHAU_CUA_BAN'
```

## 4. Build vÃ  cháº¡y backend

Má»Ÿ PowerShell táº¡i root `BankingApp_Mobile`. VÃ¬ repo khÃ´ng cÃ³ Maven Wrapper, táº¥t cáº£ lá»‡nh sau dÃ¹ng Maven global. CÃ³ thá»ƒ build láº§n lÆ°á»£t cáº£ 7 backend trÆ°á»›c khi cháº¡y:

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

Má»—i service Ä‘ang cháº¡y pháº£i giá»¯ má»™t cá»­a sá»• PowerShell riÃªng. CÃ¡c lá»‡nh dÆ°á»›i Ä‘Ã¢y Ä‘á»u giáº£ sá»­ terminal Ä‘ang Ä‘á»©ng táº¡i root repo.

1. Cháº¡y Service Registry trÆ°á»›c vÃ  chá» trang `http://localhost:8761` má»Ÿ Ä‘Æ°á»£c.

   ```powershell
   mvn -f .\Service-Registry\pom.xml spring-boot:run
   ```

2. Cháº¡y cÃ¡c service nghiá»‡p vá»¥. Database pháº£i Ä‘Æ°á»£c táº¡o trÆ°á»›c bÆ°á»›c nÃ y.

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

3. Khi cÃ¡c service Ä‘Ã£ xuáº¥t hiá»‡n trÃªn Eureka, cháº¡y API Gateway cuá»‘i cÃ¹ng.

   ```powershell
   mvn -f .\API-Gateway\pom.xml spring-boot:run
   ```

4. XÃ¡c nháº­n Gateway á»Ÿ port 8080:

   ```powershell
   curl.exe http://localhost:8080/actuator/health
   curl.exe "http://localhost:8080/transactions?accountId=test"
   ```

Lá»‡nh thá»© hai pháº£i tráº£ `401` khi khÃ´ng cÃ³ token; Ä‘Ã¢y lÃ  hÃ nh vi Ä‘Ãºng. Náº¿u Gateway tráº£ `503`, kiá»ƒm tra service Ä‘Ã­ch Ä‘Ã£ Ä‘Äƒng kÃ½ trÃªn Eureka vÃ  tÃªn service cÃ³ khá»›p báº£ng cáº¥u hÃ¬nh á»Ÿ trÃªn hay chÆ°a.

## 5. Test nhanh báº±ng Postman

Import collection:

`postman_collection/Banking Core Services.postman_collection.json`

Collection Ä‘Ã£ Ä‘áº·t `base_url` lÃ  `http://localhost:8080` vÃ  cÃ³ request cho user, account, sequence, transaction vÃ  fund transfer. Nhá»¯ng request Ä‘Äƒng kÃ½/duyá»‡t user cáº§n Keycloak realm/client Ä‘Ãºng nhÆ° `User-Service/src/main/resources/application.yml`.

CÃ³ thá»ƒ thá»­ endpoint khÃ´ng cáº§n dá»¯ liá»‡u seed:

```powershell
curl.exe -X POST http://localhost:8080/sequence
```

## 6. Cháº¡y Android

1. Má»Ÿ riÃªng folder `BankingMobileApp` báº±ng Android Studio.
2. Chá» Gradle sync vÃ  báº£o Ä‘áº£m SDK Platform 35 cÃ¹ng Build-Tools 35.0.0 Ä‘Ã£ Ä‘Æ°á»£c cÃ i.
3. Khá»Ÿi Ä‘á»™ng Android Emulator.
4. Cháº¡y cáº¥u hÃ¬nh `app`.

`BankingMobileApp/local.properties` lÃ  cáº¥u hÃ¬nh riÃªng cá»§a tá»«ng mÃ¡y vÃ  khÃ´ng cÃ²n Ä‘Æ°á»£c Git track, vÃ¬ váº­y pull code sáº½ khÃ´ng ghi Ä‘Ã¨ Ä‘Æ°á»ng dáº«n SDK báº±ng username cá»§a mÃ¡y khÃ¡c. Sau khi clone/pull trÃªn mÃ¡y má»›i, táº¡o láº¡i file tá»± Ä‘á»™ng tá»« SDK máº·c Ä‘á»‹nh `%LOCALAPPDATA%\Android\Sdk`:

```powershell
cd C:\Users\Asus\StudioProjects\BankingApp_Mobile
powershell -NoProfile -ExecutionPolicy Bypass -File .\setup-android-sdk.ps1
```

Náº¿u SDK náº±m á»Ÿ vá»‹ trÃ­ khÃ¡c:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\setup-android-sdk.ps1 -SdkPath "D:\Android\Sdk"
```

Script kiá»ƒm tra SDK Platform 35 vÃ  Build-Tools 35.0.0 trÆ°á»›c khi táº¡o `BankingMobileApp/local.properties`. CÃ³ thá»ƒ build láº¡i báº±ng:

```powershell
cd .\BankingMobileApp
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleDebug
```

`ApiClient.java` Ä‘ang dÃ¹ng Ä‘Ãºng base URL:

```text
http://10.0.2.2:8080/
```

KhÃ´ng Ä‘á»•i URL nÃ y khi dÃ¹ng Android Emulator vÃ¬ `10.0.2.2` Ã¡nh xáº¡ tá»›i `localhost` cá»§a mÃ¡y cháº¡y backend. Manifest Ä‘Ã£ cÃ³ quyá»n Internet vÃ  `android:usesCleartextTraffic="true"` Ä‘á»ƒ gá»i HTTP local.

Náº¿u cháº¡y báº±ng Ä‘iá»‡n thoáº¡i tháº­t, Ä‘á»•i `10.0.2.2` thÃ nh IP LAN cá»§a mÃ¡y cháº¡y backend, vÃ­ dá»¥ `http://192.168.1.10:8080/`; Ä‘iá»‡n thoáº¡i vÃ  mÃ¡y pháº£i cÃ¹ng máº¡ng, Ä‘á»“ng thá»i firewall pháº£i cho phÃ©p port 8080.

CÃ³ thá»ƒ build APK debug tá»« terminal:

```powershell
cd .\BankingMobileApp
.\gradlew.bat assembleDebug
```

## 7. Seed vÃ  cÃ¡c Ä‘iá»ƒm cáº§n biáº¿t

KhÃ´ng táº¡o `seed-temp-data.sql` trong bÆ°á»›c nÃ y. LÃ½ do:

- Hibernate pháº£i cháº¡y Ã­t nháº¥t má»™t láº§n Ä‘á»ƒ táº¡o schema váº­t lÃ½ theo naming strategy thá»±c táº¿.
- User há»£p lá»‡ cÃ²n liÃªn káº¿t vá»›i `authId` cá»§a Keycloak. ChÃ¨n user trá»±c tiáº¿p vÃ o MySQL cÃ³ thá»ƒ táº¡o dá»¯ liá»‡u khÃ´ng nháº¥t quÃ¡n.
- CÃ¡c báº£ng account, transaction vÃ  fund transfer cÃ³ quan há»‡ nghiá»‡p vá»¥; seed Ä‘oÃ¡n tay dá»… lÃ m giao diá»‡n test sai.

Quy trÃ¬nh an toÃ n lÃ : táº¡o database rá»—ng, cháº¡y toÃ n bá»™ backend má»™t láº§n Ä‘á»ƒ Hibernate táº¡o báº£ng, cáº¥u hÃ¬nh Keycloak náº¿u cáº§n luá»“ng Ä‘Äƒng kÃ½, rá»“i táº¡o dá»¯ liá»‡u qua API/Postman.

## 8. Káº¿t quáº£ kiá»ƒm tra trÃªn mÃ¡y hiá»‡n táº¡i

- Android Ä‘Ã£ build thÃ nh cÃ´ng báº±ng `gradlew.bat assembleDebug` sau khi sá»­a Ä‘Æ°á»ng dáº«n SDK local tá»« tÃ i khoáº£n mÃ¡y cÅ© sang SDK hiá»‡n táº¡i.
- MÃ´i trÆ°á»ng terminal hiá»‡n táº¡i Ä‘ang dÃ¹ng Java 25.0.1, trong khi repo Ä‘áº·t Java 17; nÃªn chuyá»ƒn `JAVA_HOME` vÃ  `Path` vá» JDK 17 trÆ°á»›c khi cháº¡y backend Ä‘á»ƒ Ä‘Ãºng toolchain cá»§a project.
- Terminal chÆ°a cÃ³ `mvn` vÃ  `mysql` trong `PATH`. CÃ¡c vá»‹ trÃ­ MySQL phá»• biáº¿n nÃªu trÃªn cÅ©ng chÆ°a cÃ³ `mysql.exe`, nÃªn chÆ°a thá»ƒ build/cháº¡y end-to-end backend vÃ  thá»±c thi SQL tá»« terminal nÃ y. ÄÃ¢y lÃ  thiáº¿u cÃ´ng cá»¥ local, khÃ´ng pháº£i báº±ng chá»©ng repo bá»‹ há»ng.
- Repo khÃ´ng cÃ³ Maven Wrapper; cáº§n cÃ i Maven global hoáº·c chá»§ Ä‘á»™ng bá»• sung wrapper trong má»™t task riÃªng.
- KhÃ´ng thay Ä‘á»•i package Android, port, API path, route, Entity, logic backend hoáº·c UI.
