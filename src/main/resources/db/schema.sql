-- Defines the bank ATM domain model described in the current ERD.

-- -------- accounts -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS accounts (
    account_id       VARCHAR(30) PRIMARY KEY,
    account_number   VARCHAR(20) NOT NULL UNIQUE,
    balance          DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    interest_rate    DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    account_type     VARCHAR(20) NOT NULL,
    min_balance      DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    overdraft_limit  DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_accounts_type CHECK (account_type IN ('SAVINGS', 'CURRENT'))
);

-- -------- atm_cards ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS atm_cards (
    card_id            VARCHAR(30) PRIMARY KEY,
    pin_hash           CHAR(64) NOT NULL,
    status             VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    failed_attempts    INT NOT NULL DEFAULT 0,
    linked_account_id  VARCHAR(30) NOT NULL UNIQUE,
    CONSTRAINT chk_atm_cards_status CHECK (status IN ('ACTIVE', 'BLOCKED')),
    CONSTRAINT fk_atm_cards_account FOREIGN KEY (linked_account_id) REFERENCES accounts (account_id) ON DELETE CASCADE,
    INDEX linked_account_index (linked_account_id)
);

-- -------- customers ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS customers (
    customer_id VARCHAR(30) PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    address     VARCHAR(255),
    email       VARCHAR(100) NOT NULL UNIQUE,
    card_id     VARCHAR(30) UNIQUE,
    account_id  VARCHAR(30) UNIQUE,
    CONSTRAINT fk_customers_card FOREIGN KEY (card_id) REFERENCES atm_cards (card_id) ON DELETE CASCADE,
    CONSTRAINT fk_customers_account FOREIGN KEY (account_id) REFERENCES accounts (account_id) ON DELETE CASCADE
);

-- -------- transactions ------------------------------------------------------
CREATE TABLE IF NOT EXISTS transactions (
    transaction_id VARCHAR(40) PRIMARY KEY,
    account_id     VARCHAR(30) NOT NULL,
    type           VARCHAR(20) NOT NULL,
    amount         DECIMAL(15,2) NOT NULL,
    balance_after  DECIMAL(15,2) NOT NULL,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_transactions_type CHECK (type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER_IN', 'TRANSFER_OUT', 'INTEREST')),
    CONSTRAINT fk_transactions_account FOREIGN KEY (account_id) REFERENCES accounts (account_id) ON DELETE CASCADE,
    INDEX created_time_by_account (account_id, created_at)
);

-- -------- scheduled_transfers ----------------------------------------------
CREATE TABLE IF NOT EXISTS scheduled_transfers (
    transfer_id      VARCHAR(40) PRIMARY KEY,
    source_account_id VARCHAR(30) NOT NULL,
    dest_account_id   VARCHAR(30) NOT NULL,
    amount           DECIMAL(15,2) NOT NULL,
    frequency        VARCHAR(20) NOT NULL,
    next_execution   TIMESTAMP NOT NULL,
    status           VARCHAR(20) NOT NULL,
    max_repeats      INT NOT NULL,
    executed_count   INT NOT NULL DEFAULT 0,
    CONSTRAINT chk_scheduled_frequency CHECK (frequency IN ('DAILY', 'WEEKLY', 'MONTHLY')),
    CONSTRAINT chk_scheduled_status CHECK (status IN ('ACTIVE', 'PAUSED', 'COMPLETED', 'FAILED')),
    CONSTRAINT fk_scheduled_source FOREIGN KEY (source_account_id) REFERENCES accounts (account_id),
    CONSTRAINT fk_scheduled_dest FOREIGN KEY (dest_account_id) REFERENCES accounts (account_id)
);

-- -------- atm_machines ------------------------------------------------------
CREATE TABLE IF NOT EXISTS atm_machines (
    atm_id                VARCHAR(30) PRIMARY KEY,
    location              VARCHAR(255) NOT NULL,
    branch_name           VARCHAR(100) NOT NULL,
    total_cash            BIGINT NOT NULL DEFAULT 0,
    denomination_500k     INT NOT NULL DEFAULT 0,
    denomination_200k     INT NOT NULL DEFAULT 0,
    denomination_100k     INT NOT NULL DEFAULT 0,
    denomination_50k      INT NOT NULL DEFAULT 0
);

-- -------- admin_audit_log ---------------------------------------------------
CREATE TABLE IF NOT EXISTS admin_audit_log (
    log_id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_username  VARCHAR(100) NOT NULL,
    action          VARCHAR(255) NOT NULL,
    details         VARCHAR(1000),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
