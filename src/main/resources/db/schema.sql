-- H2 database schema in MySQL compatibility mode (MODE=MySQL).
--
-- The application's repository layer currently persists to the pipe-delimited
-- text files under data/. This schema mirrors the same entity model so the
-- data is available through JDBC (H2) for reporting, the H2 Console, or a
-- future migration of the File*Repository implementations.

-- -------- customers ----------------------------------------------------------
-- Format mirror: customers.txt  ->  customerId|name|address|email|cardId|accountNumber
CREATE TABLE IF NOT EXISTS customers (
    customer_id    VARCHAR(20)  PRIMARY KEY,
    customer_name  VARCHAR(100) NOT NULL,
    address        VARCHAR(255),
    email          VARCHAR(100),
    card_id        VARCHAR(30)  NOT NULL,
    account_number VARCHAR(20)  NOT NULL
);

-- -------- accounts -----------------------------------------------------------
-- Format mirror: accounts.txt  ->  accountNumber|type|balance|lastInterestYearMonth
CREATE TABLE IF NOT EXISTS accounts (
    account_number           VARCHAR(20) PRIMARY KEY,
    account_type             ENUM('SAVINGS', 'CURRENT') NOT NULL,
    balance                  BIGINT      NOT NULL DEFAULT 0,
    last_interest_year_month VARCHAR(7)  NOT NULL DEFAULT ''
);

-- -------- cards ---------------------------------------------------------------
-- Format mirror: cards.txt  ->  cardId|pin|accountNumber|status|failedAttempts
CREATE TABLE IF NOT EXISTS cards (
    card_id         VARCHAR(30) PRIMARY KEY,
    pin             VARCHAR(10) NOT NULL,
    account_number  VARCHAR(20) NOT NULL,
    status          ENUM('ACTIVE', 'BLOCKED') NOT NULL DEFAULT 'ACTIVE',
    failed_attempts INT         NOT NULL DEFAULT 0,
    CONSTRAINT fk_cards_account FOREIGN KEY (account_number) REFERENCES accounts (account_number)
);

-- -------- transactions ---------------------------------------------------------
-- Format mirror: transactions.txt  ->  txId|accountNumber|dateTime|type|amount|balanceAfter|description
CREATE TABLE IF NOT EXISTS transactions (
    transaction_id  VARCHAR(40) PRIMARY KEY,
    account_number  VARCHAR(20) NOT NULL,
    date_time       DATETIME    NOT NULL,
    type            ENUM('DEPOSIT', 'WITHDRAWAL', 'INTEREST', 'TRANSFER_OUT', 'TRANSFER_IN') NOT NULL,
    amount          BIGINT      NOT NULL,
    balance_after   BIGINT      NOT NULL,
    description     VARCHAR(255),
    CONSTRAINT fk_transactions_account FOREIGN KEY (account_number) REFERENCES accounts (account_number)
);

-- -------- scheduled_transfers ---------------------------------------------------
-- Format mirror: scheduled_transfers.txt
--   id|sourceAccount|destAccount|amount|frequency|nextExecutionDate|status|maxRepeat|repeatCount|endDate
CREATE TABLE IF NOT EXISTS scheduled_transfers (
    id                  VARCHAR(40) PRIMARY KEY,
    source_account      VARCHAR(20) NOT NULL,
    dest_account        VARCHAR(20) NOT NULL,
    amount              BIGINT      NOT NULL,
    frequency           ENUM('ONE_TIME', 'DAILY', 'WEEKLY', 'MONTHLY') NOT NULL,
    next_execution_date DATE        NOT NULL,
    status              ENUM('ACTIVE', 'PAUSED', 'COMPLETED', 'FAILED', 'CANCELLED') NOT NULL,
    max_repeat          INT         NOT NULL,
    repeat_count        INT         NOT NULL DEFAULT 0,
    end_date            DATE,
    CONSTRAINT fk_st_source FOREIGN KEY (source_account) REFERENCES accounts (account_number),
    CONSTRAINT fk_st_dest   FOREIGN KEY (dest_account)   REFERENCES accounts (account_number)
);

-- -------- atm_info ----------------------------------------------------------------
-- Format mirror: atm.txt  ->  location|branchName  (single row)
CREATE TABLE IF NOT EXISTS atm_info (
    id          INT PRIMARY KEY,
    location    VARCHAR(100),
    branch_name VARCHAR(100)
);

-- -------- denominations -----------------------------------------------------------
-- Format mirror: denominations.txt  ->  denomination|count  (one per line)
CREATE TABLE IF NOT EXISTS denominations (
    denomination BIGINT PRIMARY KEY,
    bill_count   INT  NOT NULL DEFAULT 0
);

-- -------- admin_log -------------------------------------------------------------
-- Format mirror: admin_log.txt  ->  timestamp|adminUser|action  (append-only)
CREATE TABLE IF NOT EXISTS admin_log (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    log_time   DATETIME     NOT NULL,
    admin_user VARCHAR(50)  NOT NULL,
    action     VARCHAR(255) NOT NULL
);
