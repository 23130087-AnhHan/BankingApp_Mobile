-- Temporary local databases for BankingApp_Mobile.
-- Tables are intentionally omitted: each JPA service uses ddl-auto: update.

CREATE DATABASE IF NOT EXISTS user_service
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS account_service
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS sequence_generator
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS transaction_service
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS fund_transfer_service
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
