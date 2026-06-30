<h1 align="center">🌟 Spring-Boot-Microservices-Banking-Application 🌟</h1>
<h2>📋 Table of Contents</h2>

- [🔍 About](#-about)
- [🏛️ Architecture](#-architecture)
- [🚀 Microservices](#-microservices)
- [🚀 Getting Started](#-getting-started)
- [📖 Documentation](#-documentation)
- [⌚ Future Enhancement](#-future-enhancement)
- [🤝 Contribution](#-contribution)
- [📞 Contact Information](#-contact-information)

## 🔍 About
<p>
    The Banking Application is built using a microservices architecture, incorporating the Spring Boot framework along with other Spring technologies such as Spring Data JPA, Spring Cloud, and Spring Security, alongside tools like Maven for dependency management. These technologies play a crucial role in establishing essential components like Service Registry, API Gateway, and more.<br><br>
    Moreover, they enable us to develop independent microservices such as the user service for user management, the account service for account generation and other related functionalities, the fund transfer service for various transfer operations, and the transaction service for viewing transactions and facilitating withdrawals and deposits. These technologies not only streamline development but also enhance scalability and maintainability, ensuring a robust and efficient banking system.
</p>

## 🏛️ Architecture

- **Service Registry:** The microservices uses the discovery service for service registration and service discovery, this helps the microservices to discovery and communicate with other services, without needing to hardcode the endpoints while communicating with other microservices.

- **API Gateway:** This microservices uses the API gateway to centralize the API endpoint, where all the endpoints have common entry point to all the endpoints. The API Gateway also facilitates the Security inclusion where the Authorization and Authentication for the Application.

- **Database per Microservice:** Each of the microservice have there own dedicated database. Here for this application for all the microservices we are incorparating the MySQL database. This helps us to isolate each of the services from each other which facilitates each services to have their own data schemas and scale each of the database when required.


<h2>🚀 Microservices</h2>

- **🌐 Service Registry (Eureka):** Acts as a service discovery server, allowing microservices to find and communicate with each other dynamically.

- **🛡️ API Gateway:** The central entry point for all client requests. It handles routing to appropriate microservices and manages security (authentication and authorization).

- **👤 User Service:** The user microservice provides functionalities for user management. This includes user registration, updating user details, viewing user information, and accessing all accounts associated with the user. Additionally, this microservice handles user authentication and authorization processes.

- **💼 Account Service:** The account microservice manages account-related APIs. It enables users to modify account details, view all accounts linked to the user profile, access transaction histories for each account, and supports the account closure process.

- **💸 Fund Transfer Service:** The fund transfer microservice facilitates various fund transfer-related functionalities. Users can initiate fund transfers between different accounts, access detailed fund transfer records, and view specific details of any fund transfer transaction.

- **💳 Transactions Service:** The transaction service offers a range of transaction-related services. Users can view transactions based on specific accounts or transaction reference IDs, as well as make deposits or withdrawals from their accounts.

- **🔢 Sequence Generator Service:** Responsible for generating unique sequences for various entities across the banking application.

<h2>🚀 Getting Started</h2>

To get started, follow these steps to run the application on your local system:

### 1. Prerequisites
- **Java 17** installed.
- **Maven** installed.
- **WampServer** (đã chạy MySQL trên port 3306).
- **Keycloak** (bạn cần tải và chạy thủ công hoặc dùng Docker nếu muốn).

### 2. Khởi tạo Database (WampServer)
Bạn hãy mở **phpMyAdmin** hoặc dùng tool SQL (như Navicat, DBeaver) và tạo các database sau:
- `user_service`
- `account_service`
- `transaction_service`
- `fund_transfer_service`
- `sequence_generator`

*Lưu ý: Mặc định code đang để user là `root` và password là `123456`. Nếu Wamp của bạn để trống password, hãy cập nhật trong các file `application.yml` của từng service.*

### 3. Chạy các Microservices
Bạn có thể dùng file script PowerShell tôi đã tạo để chạy tất cả service theo đúng thứ tự:

```powershell
./run-services.ps1
```

Script này sẽ:
1. Chạy **Service Registry** (Cổng 8761) và đợi nó sẵn sàng.
2. Chạy **API Gateway** (Cổng 8080).
3. Chạy tất cả các service còn lại trong các cửa sổ riêng biệt.

<h3>💡 PowerShell Shortcut</h3>

If you are on Windows, you can use this PowerShell "trick" to start all microservices at once in separate windows. Run this command in the project root:

```powershell
Get-ChildItem -Filter pom.xml -Recurse | ForEach-Object { Start-Process powershell -ArgumentList "cd $($_.DirectoryName); mvn spring-boot:run" }
```

Alternatively, you can run the provided `run-services.ps1` script (if available) which handles the startup order (starting Service Registry first).

<h2>📖 Documentation</h2>
<h3>📂 Microservices Documentation</h3>

For detailed information about each microservice, refer to their respective README files:

- [👤 User Service](./User-Service/README.md)
- [💼 Account Service](./Account-Service/README.md)
- [💸 Fund Transfer Service](./Fund-Transfer/README.md)
- [💳 Transactions Service](./Transaction-Service/README.md)
- [🔢 Sequence Generator Service](./Sequence-Generator/README.md)
- [🌐 Service Registry](./Service-Registry/README.md)
- [🛡️ API Gateway](./API-Gateway/README.md)

<h3>📖 API Documentation</h3>

For a detailed guide on API endpoints and usage instructions, explore our comprehensive [API Documentation](https://app.theneo.io/student/spring-boot-microservices-banking-application). This centralized resource offers a holistic view of the entire banking application, making it easier to understand and interact with various services.

<h3>📚 Java Documentation (JavaDocs)</h3>

Explore the linked [Java Documentation](https://kartik1502.github.io/Spring-Boot-Microservices-Banking-Application/) to delve into detailed information about classes, methods, and variables across all microservices. These resources are designed to empower developers by providing clear insights into the codebase and facilitating seamless development and maintenance tasks.

## ⌚ Future Enhancement

As part of our ongoing commitment to improving the banking application, we are planning several enhancements to enrich user experience and expand functionality:

- Implementing a robust notification system will keep users informed about important account activities, such as transaction updates, account statements, and security alerts. Integration with email and SMS will ensure timely and relevant communication.
- Adding deposit and investment functionalities will enable users to manage their savings and investments directly through the banking application. Features such as fixed deposits, recurring deposits, and investment portfolio tracking will empower users to make informed financial decisions.
- and more....

<h2>🤝 Contribution</h2>

Contributions to this project are welcome! Feel free to open issues, submit pull requests, or provide feedback to enhance the functionality and usability of this banking application. Follow the basic PR specification while creating a PR.

Let's build a robust and efficient banking system together using Spring Boot microservices!

Happy Banking! 🏦💰

<h2>📞 Contact Information</h2>

If you have any questions, feedback, or need assistance with this project, please feel free to reach out to me:

[![WhatsApp](https://img.shields.io/badge/WhatsApp-25D366?style=for-the-badge&logo=whatsapp&logoColor=white)](https://wa.me/6361921186)
[![GMAIL](https://img.shields.io/badge/Gmail-D14836?style=for-the-badge&logo=gmail&logoColor=white)](mailto:kartikkulkarni1411@gmail.com)

We appreciate your interest in our project and look forward to hearing from you. Happy coding!