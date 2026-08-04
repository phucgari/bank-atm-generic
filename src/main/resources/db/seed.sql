-- Seed data for the H2 database (MySQL compatibility mode).
--
-- Mirrors the pipe-delimited text files under data/. Run only when the
-- database is empty (see DatabaseInitializer). Values use the same
-- conventions as the text files: uppercase enum tokens, VND amounts (long),
-- "yyyy-MM-dd" dates and "yyyy-MM-dd HH:mm:ss" timestamps.

-- -------- customers ----------------------------------------------------------
-- customers.txt: customerId|customerName|address|email|cardId|accountNumber
INSERT INTO customers (customer_id, customer_name, address, email, card_id, account_number) VALUES
('C001', 'Nguyen Van An',   '12 Nguyen Hue, District 1, HCMC',       'an.nguyen@email.com',    '4921-XXXX-XXXX-1001', 'ACC001'),
('C002', 'Tran Thi Bich',   '45 Le Thanh Ton, District 1, HCMC',     'bich.tran@email.com',    '4921-XXXX-XXXX-1002', 'ACC002'),
('C003', 'Le Minh Cuong',   '88 Pham Ngoc Thach, District 3, HCMC',  'cuong.le@email.com',     '4921-XXXX-XXXX-1003', 'ACC003');

-- -------- accounts -----------------------------------------------------------
-- accounts.txt: accountNumber|type|balance|lastInterestYearMonth
INSERT INTO accounts (account_number, account_type, balance, last_interest_year_month) VALUES
('ACC001', 'SAVINGS', 13000000, ''),
('ACC002', 'SAVINGS',  8500000, ''),
('ACC003', 'CURRENT', 22000000, '');

-- -------- cards --------------------------------------------------------------
-- cards.txt: cardId|pin|accountNumber|status|failedAttempts
INSERT INTO cards (card_id, pin, account_number, status, failed_attempts) VALUES
('4921-XXXX-XXXX-1001', '2468', 'ACC001', 'ACTIVE',  0),
('4921-XXXX-XXXX-1002', '3579', 'ACC002', 'ACTIVE',  0),
('4921-XXXX-XXXX-1003', '1357', 'ACC003', 'ACTIVE',  0);

-- -------- transactions -------------------------------------------------------
-- transactions.txt: transactionId|accountNumber|dateTime|type|amount|balanceAfter|description
INSERT INTO transactions (transaction_id, account_number, date_time, type, amount, balance_after, description) VALUES
('TX001', 'ACC001', '2026-05-10 09:15:00', 'DEPOSIT',       5000000, 15000000, ''),
('TX002', 'ACC001', '2026-05-15 14:22:00', 'WITHDRAWAL',    2000000, 13000000, ''),
('TX003', 'ACC002', '2026-05-12 10:30:00', 'DEPOSIT',       3000000,  8500000, ''),
('TX004', 'ACC003', '2026-05-20 11:00:00', 'DEPOSIT',      10000000, 22000000, ''),
('TX005', 'ACC003', '2026-05-25 16:45:00', 'TRANSFER_OUT',  2000000, 20000000, 'To: ACC001'),
('TX006', 'ACC001', '2026-05-25 16:45:00', 'TRANSFER_IN',   2000000, 15000000, 'From: ACC003');

-- -------- scheduled_transfers -------------------------------------------------
-- scheduled_transfers.txt currently contains no data rows (header only).
-- Example row shown for reference:
-- id|sourceAccount|destAccount|amount|frequency|nextExecDate|status|maxRepeat|repeatCount|endDate

-- -------- atm_info ---------------------------------------------------------------
-- atm.txt: location|branchName|maxCapacity  (maxCapacity is a code constant, not persisted)
INSERT INTO atm_info (id, location, branch_name) VALUES
(1, '123 Le Loi St, District 1', 'VietBank - Ho Chi Minh City');

-- -------- denominations -----------------------------------------------------------
-- denominations.txt: denomination|count  (one per line)
INSERT INTO denominations (denomination, bill_count) VALUES
(500000, 56),
(200000, 50),
(100000, 80),
(50000,  100);

-- -------- admin_log -------------------------------------------------------------
-- admin_log.txt: timestamp|adminUser|action  (append-only)
INSERT INTO admin_log (log_time, admin_user, action) VALUES
('2026-06-07 21:57:52', 'admin', 'Admin login'),
('2026-06-07 21:57:52', 'admin', 'Viewed all accounts'),
('2026-06-07 21:57:52', 'admin', 'Admin logout');
