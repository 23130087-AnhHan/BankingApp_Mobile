# Prompt Setup Nhanh Cho Nhanh UI

Dung prompt nay khi mot may moi pull nhanh `UI` ve nhung chua biet can cau hinh gi de dang ky/dang nhap duoc.

```text
Ban la coding assistant trong repo BankingApp_Mobile tren nhanh UI.

Hay kiem tra va huong dan setup moi truong local de chay Android app voi backend that, khong demo:

1. Xac nhan dang o nhanh UI va pull moi nhat.
2. Kiem tra JDK 17, JAVA_HOME, Maven wrapper.
3. Kiem tra Android SDK/local.properties cho dung may hien tai.
4. Kiem tra MySQL va tao cac database neu thieu:
   - user_service
   - account_service
   - sequence_generator
   - transaction_service
   - fund_transfer_service
5. Huong dan chay Keycloak khong Docker bang ZIP tren Windows:
   - Tai Keycloak Server ZIP tu https://www.keycloak.org/downloads
   - Giai nen vao C:\Tools\keycloak
   - Neu bi long folder version nhu C:\Tools\keycloak\keycloak-26.6.4 thi cd vao folder do
   - Chay:
     cd C:\Tools\keycloak
     $env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
     $env:Path = "$env:JAVA_HOME\bin;$env:Path"
     $env:KEYCLOAK_ADMIN = "admin"
     $env:KEYCLOAK_ADMIN_PASSWORD = "admin"
     .\bin\kc.bat start-dev --http-port=8571
6. Trong Keycloak tao realm:
   - banking-service
7. Tao client:
   - Client type: OpenID Connect
   - Client ID: banking-service-api-client
   - Client authentication: ON
   - Service accounts roles: ON
   - Direct access grants: ON neu backend/Android can login username/password
   - Standard flow: ON neu can
8. Gan service-account roles tu client realm-management:
   - manage-users
   - view-users
   - query-users
9. Lay Client Secret trong tab Credentials, khong luu vao code, khong commit.
10. Xac nhan User-Service application.yml van dung:
    client-secret: ${KEYCLOAK_CLIENT_SECRET}
11. Chay User-Service bang:
    .\run-user-service.ps1
    Script phai hoi KEYCLOAK_CLIENT_SECRET bang Read-Host.
12. Chay cac service con lai:
    .\mvnw.cmd -f .\Service-Registry\pom.xml spring-boot:run
    .\mvnw.cmd -f .\Account-Service\pom.xml spring-boot:run
    .\mvnw.cmd -f .\Sequence-Generator\pom.xml spring-boot:run
    .\mvnw.cmd -f .\Transaction-Service\pom.xml spring-boot:run
    .\mvnw.cmd -f .\Fund-Transfer\pom.xml spring-boot:run
    .\mvnw.cmd -f .\API-Gateway\pom.xml spring-boot:run
13. Mo Eureka http://localhost:8761 va dam bao cac service UP.
14. Voi Android Emulator chay cung may backend, ApiClient.BASE_URL phai la:
    http://10.0.2.2:8080/
15. Neu muon dung backend cua may khac trong cung LAN, chi doi BASE_URL sang:
    http://<IP-may-chay-backend>:8080/
    va phai test duoc tu may Android/dev:
    http://<IP-may-chay-backend>:8080/api/users
16. Neu gap loi:
    - Connection refused localhost:8571: Keycloak chua chay, sai realm, sai port.
    - Thieu KEYCLOAK_CLIENT_SECRET: chay lai run-user-service.ps1 va nhap secret.
    - 401: sai client secret hoac token.
    - 403: thieu service-account roles manage-users/view-users/query-users.
    - Android timeout: sai BASE_URL, firewall, khac mang, hoac Wi-Fi chan client-to-client.

Khong duoc bypass Keycloak, khong dung KEYCLOAK_ENABLED=false, khong dung demo-secret, khong hard-code client secret vao source.
Neu can sua file, chi sua file lien quan, khong commit file build, .gradle, local.properties hay secret.
```

