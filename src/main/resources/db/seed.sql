-- Seed data for the database.
--
-- Mirrors the pipe-delimited text files under data/. Run only when the
-- database is empty (see DatabaseInitializer). Values use the same
-- conventions as the text files: uppercase enum tokens, VND amounts (long),
-- "yyyy-MM-dd" dates and "yyyy-MM-dd HH:mm:ss" timestamps.

-- -------- accounts -----------------------------------------------------------
-- accounts.txt: accountNumber|type|balance|lastInterestYearMonth
INSERT INTO accounts (account_id, account_number, account_type, balance, interest_rate) VALUES
('ACC001', 'ACC001', 'SAVINGS', 13000000.00, 0.48),
('ACC002', 'ACC002', 'SAVINGS',  8500000.00, 0.48),
('ACC003', 'ACC003', 'CURRENT', 22000000.00, 0.00);

-- -------- atm_cards ----------------------------------------------------------
-- cards.txt: cardId|pin|accountNumber|status|failedAttempts
INSERT INTO atm_cards (card_id, pin_hash, status, failed_attempts, linked_account_id) VALUES
('4921-XXXX-XXXX-1001', SHA2('2468', 256), 'ACTIVE', 0, 'ACC001'),
('4921-XXXX-XXXX-1002', SHA2('3579', 256), 'ACTIVE', 0, 'ACC002'),
('4921-XXXX-XXXX-1003', SHA2('1357', 256), 'ACTIVE', 0, 'ACC003');

-- -------- customers ----------------------------------------------------------
-- customers.txt: customerId|customerName|address|email|cardId|accountNumber
INSERT INTO customers (customer_id, name, address, email, card_id, account_id) VALUES
('C001', 'Nguyen Van An',   '12 Nguyen Hue, District 1, HCMC',      'an.nguyen@email.com',  '4921-XXXX-XXXX-1001', 'ACC001'),
('C002', 'Tran Thi Bich',   '45 Le Thanh Ton, District 1, HCMC',    'bich.tran@email.com',  '4921-XXXX-XXXX-1002', 'ACC002'),
('C003', 'Le Minh Cuong',   '88 Pham Ngoc Thach, District 3, HCMC', 'cuong.le@email.com',   '4921-XXXX-XXXX-1003', 'ACC003');

-- -------- transactions -------------------------------------------------------
-- transactions.txt: transactionId|accountNumber|dateTime|type|amount|balanceAfter|description
INSERT INTO transactions (transaction_id, account_id, type, amount, balance_after, created_at) VALUES
('TX001', 'ACC001', 'DEPOSIT',       5000000.00, 15000000.00, '2026-05-10 09:15:00'),
('TX002', 'ACC001', 'WITHDRAWAL',    2000000.00, 13000000.00, '2026-05-15 14:22:00'),
('TX003', 'ACC002', 'DEPOSIT',       3000000.00,  8500000.00, '2026-05-12 10:30:00'),
('TX004', 'ACC003', 'DEPOSIT',      10000000.00, 22000000.00, '2026-05-20 11:00:00'),
('TX005', 'ACC003', 'TRANSFER_OUT',  2000000.00, 20000000.00, '2026-05-25 16:45:00'),
('TX006', 'ACC001', 'TRANSFER_IN',   2000000.00, 15000000.00, '2026-05-25 16:45:00');

-- -------- scheduled_transfers -------------------------------------------------
-- scheduled_transfers.txt currently contains no data rows (header only).
-- Example row shown for reference:
-- id|sourceAccount|destAccount|amount|frequency|nextExecDate|status|maxRepeat|repeatCount|endDate

-- -------- atm_machines ---------------------------------------------------------
-- atm.txt: location|branchName|maxCapacity  (maxCapacity is a code constant, not persisted)
-- denominations.txt: denomination|count  (one per line)
INSERT INTO atm_machines (atm_id, location, branch_name, total_cash, denomination_500k, denomination_200k, denomination_100k, denomination_50k) VALUES
('1', '123 Le Loi St, District 1', 'VietBank - Ho Chi Minh City', 51000000, 56, 50, 80, 100);

-- -------- admin_audit_log -----------------------------------------------------
-- admin_log.txt: timestamp|adminUser|action  (append-only).
-- log_id is AUTO_INCREMENT; no seed rows are inserted.
