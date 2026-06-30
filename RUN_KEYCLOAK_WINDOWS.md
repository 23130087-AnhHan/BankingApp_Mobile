# Run Keycloak On Windows Without Docker

Guide nay dung cho ban chinh cua project. Keycloak la bat buoc: `User-Service` van goi Keycloak that khi dang ky, dang nhap va quan ly user. Khong bypass Keycloak, khong dung `KEYCLOAK_ENABLED=false`, khong dung `demo-secret`, va khong hard-code client secret vao source.

Project khong dung `docker-compose-keycloak.yml` de chay Keycloak. Neu ban thay file nay tu ban cu, hay bo qua hoac xoa no; huong dan chinh la chay Keycloak ZIP tren Windows nhu ben duoi.

## 1. Tai Keycloak ZIP tu trang chinh thuc

1. Mo trang chinh thuc cua Keycloak: [https://www.keycloak.org/downloads](https://www.keycloak.org/downloads).
2. Tai ban **Server ZIP**.
3. Giai nen ZIP vao `C:\Tools`.
4. Doi ten folder vua giai nen thanh `keycloak` de co duong dan:

```text
C:\Tools\keycloak
C:\Tools\keycloak\bin\kc.bat
```

Neu sau khi giai nen ban thay duong dan dang bi long them version, vi du `C:\Tools\keycloak\keycloak-26.6.4\bin\kc.bat`, hay chon mot trong hai cach:

```powershell
cd C:\Tools\keycloak\keycloak-26.6.4
```

Hoac di chuyen toan bo noi dung trong `C:\Tools\keycloak\keycloak-26.6.4` ra truc tiep `C:\Tools\keycloak` de dung dung duong dan trong guide.

## 2. Chay Keycloak bang PowerShell

Mo PowerShell moi va chay:

```powershell
cd C:\Tools\keycloak

$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

$env:KEYCLOAK_ADMIN = "admin"
$env:KEYCLOAK_ADMIN_PASSWORD = "admin"

.\bin\kc.bat start-dev --http-port=8571
```

Giu cua so PowerShell nay dang chay. Sau khi thay Keycloak start xong, mo:

```text
http://localhost:8571
```

Dang nhap Admin Console bang tai khoan local:

```text
username: admin
password: admin
```

## 3. Tao realm

1. Mo `http://localhost:8571`.
2. Chon **Administration Console**.
3. Dang nhap bang user admin.
4. Chon dropdown realm o goc tren trai.
5. Chon **Create realm**.
6. Nhap realm name:

```text
banking-service
```

7. Bam **Create**.

## 4. Tao client cho backend

Trong realm `banking-service`:

1. Vao **Clients**.
2. Chon **Create client**.
3. Client type: **OpenID Connect**.
4. Client ID:

```text
banking-service-api-client
```

5. Bam **Next**.
6. Cau hinh:
   - **Client authentication**: ON.
   - **Service accounts roles**: ON.
   - **Direct access grants**: ON neu backend hoac Android can login username/password.
   - **Standard flow**: ON neu can dang nhap bang browser/redirect flow.
7. Bam **Save**.

## 5. Cap quyen service account

Trong client `banking-service-api-client`:

1. Mo tab **Service account roles**.
2. Chon **Assign role**.
3. Doi filter sang hien thi role theo client neu Keycloak dang chi hien realm roles.
4. Chon client:

```text
realm-management
```

5. Gan cac role:

```text
manage-users
view-users
query-users
```

6. Bam **Assign**.

Neu thieu cac role nay, `User-Service` co the lay duoc token client nhung se bi Keycloak tra `403` khi tao, tim hoac doc user.

## 6. Lay Client Secret

Trong client `banking-service-api-client`:

1. Mo tab **Credentials**.
2. Copy gia tri **Client Secret**.
3. Chi dan secret vao PowerShell khi script hoi.
4. Khong luu secret that vao file, khong commit secret that, khong dan secret vao `application.yml`, README, script hay Postman collection da commit.

`User-Service/src/main/resources/application.yml` phai giu dang:

```yaml
app:
  config:
    keycloak:
      client-secret: ${KEYCLOAK_CLIENT_SECRET}
```

## 7. Chay User-Service

Mo PowerShell moi tai root project:

```powershell
cd C:\Users\YanTynh\StudioProjects\BankingApp_Mobile
.\run-user-service.ps1
```

Script se set cac bien moi truong:

```powershell
$env:KEYCLOAK_URL = "http://localhost:8571"
$env:KEYCLOAK_REALM = "banking-service"
$env:KEYCLOAK_CLIENT_ID = "banking-service-api-client"
```

Script se hoi:

```text
Nhap KEYCLOAK_CLIENT_SECRET that tu Keycloak Admin Console
```

Dan secret that lay trong tab **Credentials**. Script se chay:

```powershell
.\mvnw.cmd -f .\User-Service\pom.xml spring-boot:run
```

Khi `User-Service` dung, script xoa `KEYCLOAK_CLIENT_SECRET` khoi moi truong cua tien trinh script.

## 8. Loi Connection refused localhost:8571

Loi nay nghia la `User-Service` khong ket noi duoc Keycloak tai `http://localhost:8571`.

Kiem tra theo thu tu:

1. Cua so PowerShell chay Keycloak ZIP van dang mo.
2. Keycloak da chay dung lenh `.\bin\kc.bat start-dev --http-port=8571`.
3. Trinh duyet mo duoc `http://localhost:8571`.
4. Realm `banking-service` da duoc tao.
5. Port 8571 dang lang nghe:

```powershell
Test-NetConnection localhost -Port 8571
```

6. Discovery endpoint tra JSON:

```powershell
Invoke-RestMethod http://localhost:8571/realms/banking-service/.well-known/openid-configuration
```

Neu endpoint khong tra JSON, hay tao lai realm dung ten `banking-service` hoac restart Keycloak.

## 9. Loi thieu KEYCLOAK_CLIENT_SECRET

Neu Spring bao thieu placeholder `KEYCLOAK_CLIENT_SECRET`, nghia la User-Service dang chay ma chua co bien moi truong secret.

Cach xu ly:

1. Dung User-Service.
2. Lay secret trong **Clients > banking-service-api-client > Credentials**.
3. Chay lai:

```powershell
.\run-user-service.ps1
```

4. Dan secret khi script hoi, khong bo trong.

Khong sua `application.yml` thanh secret that. Gia tri dung phai la:

```yaml
client-secret: ${KEYCLOAK_CLIENT_SECRET}
```
