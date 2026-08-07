# Bank ATM System Generic Simulation

A console-based Bank ATM System simulation built with Java 17+ (Core only, no frameworks).

## How to Build and Run

### Prerequisites
- JDK 17+
- Apache Maven 3.6+

### Build
```bash
mvn package
```

### Run
```bash
java -jar target/bank-atm-simulation.jar
```
The application reads/writes file-backed data from the configured
external `./data/` directory.

## Pre-seeded Sample Data

| Customer | Account | Type | Balance | Card ID | PIN |
|---|---|---|---|---|---|
| Nguyen Van An | ACC001 | Savings | 15,000,000 VND | 4921-XXXX-XXXX-1001 | 2468 |
| Tran Thi Bich | ACC002 | Savings | 8,500,000 VND | 4921-XXXX-XXXX-1002 | 3579 |
| Le Minh Cuong | ACC003 | Current | 22,000,000 VND | 4921-XXXX-XXXX-1003 | 1357 |

**Admin credentials:** username `admin` / password `Admin@1234`

## Features Implemented

| FR | Description |
|---|---|
| FR-01 | Card Authentication & PIN Verification (3-attempt lockout, BLOCKED status) |
| FR-02 | Cash Withdrawal (denomination algorithm, daily limits, min balance, overdraft) |
| FR-03 | Cash Deposit (confirmation flow, limits) |
| FR-04 | Balance Inquiry & Transaction History (last 10, reverse chronological) |
| FR-05 | PIN Change (current PIN re-verify, weak PIN check, 3 retries) |
| FR-06 | Interest Calculation (Savings compound 0.5%/month, Current simple 0.1%/month) |
| FR-07 | Fund Transfer (atomicity simulation, daily limits, confirmation) |
| FR-08 | ATM Administration (cash replenish, unblock card, view accounts, interest trigger) |
| FR-09 | Cash Denomination & Optimal Dispensing (greedy-first DFS, max 50 bills) |
| FR-10 | Scheduled & Recurring Transfers (ONE_TIME, DAILY, WEEKLY, MONTHLY; pause/resume/cancel) |

## Technology Choices

- **Language:** Java 17 (Core only — `java.util`, `java.io`, `java.text`, `java.time`)
- **Storage:** Switchable external plain text files or MySQL via `storage.mode`
- **Build tool:** Apache Maven

### Data Files

| File | Purpose |
|---|---|
| `data/atm.txt` | ATM location & branch config |
| `data/denominations.txt` | Bill inventory per denomination |
| `data/customers.txt` | Customer master data |
| `data/cards.txt` | ATM card data (PIN, status, failed attempts) |
| `data/accounts.txt` | Account balances and type |
| `data/transactions.txt` | All transaction records (append-only) |
| `data/scheduled_transfers.txt` | Scheduled & recurring transfers |
| `data/admin_log.txt` | Admin audit log (append-only) |

### Storage Configuration

The application uses one `application.properties` file per source set.
Production uses the `prod` profile:

```properties
profile=prod
```

Tests use `src/test/resources/application.properties` with `profile=test` and
isolated file storage.

Set `storage.mode=file` in a profile to use the repositories backed by the files in
`./data/`. The location is configurable with
`file.data-directory`. Tests use `storage.mode=file` through the `test`
profile, so repository-factory tests do not require MySQL.
The profile and database settings can be overridden with system properties,
which take precedence over the file:

```bash
java -Dprofile=prod \
     -Ddatabase.url=jdbc:mysql://db-host:3306/appdb \
     -Ddatabase.user=appuser \
     -Ddatabase.password=apppass \
     -jar target/bank-atm-simulation.jar
```

## Architecture

```
com.training.atm
├── App.java                         — Composition root; wires all components (no IoC framework)
├── config/
│   └── TransactionLimits.java       — All business limit constants in one place
├── model/
│   ├── enums/                       — AccountType, CardStatus, TransactionType,
│   │                                  TransferFrequency, TransferStatus
│   ├── strategy/                    — Interest algorithm plug-ins (Strategy pattern)
│   │   ├── InterestStrategy.java
│   │   ├── CompoundInterestStrategy.java
│   │   └── ZeroOnNegativeInterestStrategy.java
│   ├── state/                       — Card & transfer lifecycle state machines (State pattern)
│   │   ├── CardState.java / ActiveCardState.java / BlockedCardState.java
│   │   └── TransferLifecycleState.java / ActiveTransferState.java / PausedTransferState.java
│   │       CompletedTransferState.java / FailedTransferState.java / CancelledTransferState.java
│   ├── Account.java                 — Abstract; delegates interest to InterestStrategy
│   ├── SavingsAccount.java          — 0.5%/month compound, 50,000 VND min balance
│   ├── CurrentAccount.java          — 0.1%/month simple, −1,000,000 VND overdraft
│   ├── ATMCard.java                 — Delegates status behaviour to CardState
│   ├── BankCustomer.java
│   ├── Transaction.java             — Immutable transaction record
│   └── ScheduledTransfer.java       — Delegates lifecycle to TransferLifecycleState
├── dto/                             — Result objects (WithdrawalResult, DepositResult, TransferResult)
│                                      all implement OperationResult
├── command/                         — Command pattern
│   ├── OperationResult.java         — Common interface on all result DTOs
│   ├── TransactionCommand.java      — Generic command interface
│   ├── WithdrawCommand.java
│   ├── DepositCommand.java
│   └── TransferCommand.java
├── validation/                      — Generic entity validation
│   ├── ValidationRule.java          — @FunctionalInterface for one rule
│   ├── ValidationResult.java        — Validation outcome and error message
│   ├── EntityValidator.java         — Generic rule collection and runner
│   ├── withdrawal/                  — WithdrawalContext + 5 validation rules
│   ├── deposit/                     — DepositContext + 2 validation rules
│   └── transfer/                    — TransferContext + 6 validation rules
├── repository/                      — Repository interfaces (DIP)
│   └── file/                        — Text-file implementations
├── service/
│   ├── ATM.java / DisplayScreen.java / CardScanner.java
│   ├── CashDispenser.java / ReceiptPrinter.java
│   ├── WithdrawalService.java / DepositService.java
│   ├── TransferService.java / InterestService.java
│   └── impl/
│       ├── WithdrawalServiceImpl.java  — Generic validation + withdrawal logic
│       ├── DepositServiceImpl.java     — Generic validation + deposit logic
│       ├── TransferServiceImpl.java    — Generic validation + Command scheduler
│       └── InterestServiceImpl.java
├── session/
│   ├── CustomerSessionDeps.java     — Parameter-object record for CustomerSession
│   ├── AdminSessionDeps.java        — Parameter-object record for AdminSession
│   ├── CustomerSession.java         — Customer console flow (uses Command + State)
│   └── AdminSession.java            — Admin console flow (uses State)
└── util/
    ├── DenominationDispenser.java   — Greedy-first DFS optimal bill algorithm
    ├── DateUtil.java
    ├── ValidationUtil.java
    └── FormatUtil.java
```

---

## Design Patterns

The project applies several GoF patterns to concrete features. Each pattern targets a specific design problem; the sections below explain the problem, the solution, and exactly which files are involved.

---

### Observer — Generic Domain Event Bus

The `event/` package provides type-safe event publishing. `EventListener<E>`
can only receive its declared `DomainEvent` subtype, while `EventBus` stores
listeners by event class and dispatches events without unchecked casts at
call sites.

```java
EventBus eventBus = new EventBus();
eventBus.subscribe(WithdrawalEvent.class,
        event -> audit(event.getTransaction().getTransactionId()));
eventBus.publish(new WithdrawalEvent(transaction));
```

Available domain events include `WithdrawalEvent`, `TransferEvent`, and
`CardBlockedEvent`. Adding a new event requires only a new `DomainEvent`
subclass and typed subscriptions.

---

### 1. Strategy — Pluggable Interest Algorithms (FR-06)

#### Problem
`SavingsAccount` and `CurrentAccount` used to override the abstract method `calculateInterest()` directly. This baked the algorithm into the class hierarchy: changing the formula for one account type — or introducing a new one (e.g., tiered or promotional rates) — required modifying a model class, violating the Open/Closed Principle.

#### Solution
Extract the algorithm into a separate `InterestStrategy` interface. Each `Account` receives a strategy at construction time and delegates `calculateInterest()` and `getInterestRate()` to it. The account subclass only defines account-type-specific constraints (withdrawal floor, overdraft limit, display label).

```
model/strategy/
├── InterestStrategy.java              ← interface: calculate(balance), getMonthlyRate()
├── CompoundInterestStrategy.java      ← Math.round(balance * rate)  — used by SavingsAccount
└── ZeroOnNegativeInterestStrategy.java← returns 0 when balance ≤ 0  — used by CurrentAccount
```

#### How it works

```java
// Account.java — delegates to the injected strategy
public long   calculateInterest() { return interestStrategy.calculate(balance); }
public double getInterestRate()   { return interestStrategy.getMonthlyRate();   }

// SavingsAccount.java — 3-param convenience constructor (default algorithm)
public SavingsAccount(String accountNumber, long balance, String lastInterestYearMonth) {
    this(accountNumber, balance, lastInterestYearMonth,
         new CompoundInterestStrategy(MONTHLY_RATE));
}

// FileAccountRepository.java — 4-param constructor makes the injection explicit
case SAVINGS -> new SavingsAccount(p[0], balance, p[3],
                   new CompoundInterestStrategy(SavingsAccount.MONTHLY_RATE));
case CURRENT -> new CurrentAccount(p[0], balance, p[3],
                   new ZeroOnNegativeInterestStrategy(CurrentAccount.MONTHLY_RATE));
```

**Benefit:** To add a "double-rate promotional savings account", create one new `PromotionalInterestStrategy` class and pass it at construction time — zero changes to `Account`, `SavingsAccount`, or `InterestServiceImpl`.

---

### 2. State — Card Lockout & Transfer Lifecycle (FR-01, FR-10)

#### Problem
Two entities had lifecycle states that were managed with raw enum comparisons scattered across multiple classes:

- **`ATMCard`:** `if (card.getStatus() == CardStatus.BLOCKED)` appeared in `CardScanner`, `CustomerSession`, and `AdminSession`. Blocking a card required two separate calls: `card.setStatus(BLOCKED)` and `card.setFailedAttempts(0)`. Missing either call left the card in an inconsistent state.
- **`ScheduledTransfer`:** `if (st.getStatus() == TransferStatus.ACTIVE) … else if (st.getStatus() == TransferStatus.PAUSED) …` chains appeared in both `CustomerSession` and `TransferServiceImpl`. Legal transitions were not enforced — any caller could call `setStatus()` with any value at any time.

#### Solution
Replace the raw enum field with a `CardState` / `TransferLifecycleState` interface. Each concrete state class encapsulates which operations are legal and what the next state is. The model objects (`ATMCard`, `ScheduledTransfer`) expose behaviour-describing methods (`isBlocked()`, `canPause()`, `canResume()`) instead of data-exposing getters.

```
model/state/
├── CardState.java                 ← canAcceptPin(), onPinSuccess(), onPinFailure(), onAdminUnblock(), isBlocked()
├── ActiveCardState.java           ← canAcceptPin=true;  blocks after N failures
├── BlockedCardState.java          ← canAcceptPin=false; onAdminUnblock → ActiveCardState
│
├── TransferLifecycleState.java    ← canExecute/Pause/Resume/Cancel(), onExecuteSuccess/Failure(), onPause/Resume/Cancel()
├── ActiveTransferState.java       ← canExecute/Pause/Cancel=true;  onExecuteSuccess(last) → Completed if last
├── PausedTransferState.java       ← canResume/Cancel=true
├── CompletedTransferState.java    ← terminal — all can*() = false
├── FailedTransferState.java       ← terminal
└── CancelledTransferState.java    ← terminal
```

#### How it works — ATMCard (FR-01)

```java
// CardScanner.java — reads behaviour, never compares enums
public Optional<ATMCard> acceptCard(String cardId) {
    if (card.isBlocked()) { /* show error */ return Optional.empty(); }  // ← State delegation
    ...
}

public boolean validatePin(ATMCard card, String enteredPin) {
    if (card.getPin().equals(enteredPin)) {
        card.recordPinSuccess();   // ← resets counter + stays ActiveCardState
        return true;
    }
    card.recordPinFailure(MAX_PIN_ATTEMPTS);  // ← increments counter; BlockedCardState if limit reached
    ...
}

// AdminSession.java — single call replaces two-line sequence
card.adminUnblock();   // ← resets failedAttempts + transitions to ActiveCardState
```

#### How it works — ScheduledTransfer (FR-10)

```java
// CustomerSession.java — queries capability, never compares TransferStatus
if (st.canPause()) {
    st.pause();           // ← ActiveTransferState → PausedTransferState
} else if (st.canResume()) {
    st.resume();          // ← PausedTransferState → ActiveTransferState
}

if (!st.canCancel()) {    // replaces: status == COMPLETED || status == FAILED
    screen.println("Cannot cancel — status is: " + st.getStatus());
    return;
}
st.cancel();              // ← any non-terminal state → CancelledTransferState

// TransferServiceImpl.java — scheduler uses state methods, not setStatus()
st.executeFail();                        // → FailedTransferState
st.executeSuccess(isLastExecution);      // → CompletedTransferState if last, else stays Active
st.complete();                           // → CompletedTransferState (end-date / repeat-count reached)
```

The `CardStatus` / `TransferStatus` enums are kept **only** for file serialisation. On load, the repository calls `ATMCard.stateFrom(status)` / `ScheduledTransfer.stateFrom(status)` to reconstruct the correct state object.

**Benefit:** Adding a new `FROZEN` card state requires only one new `FrozenCardState` class and one new `case` in `stateFrom()`. No changes to `CardScanner`, `CustomerSession`, or `AdminSession`.

---

### 3. Command — Encapsulated ATM Operations (FR-02, FR-03, FR-07, FR-10)

#### Problem
The scheduler loop in `TransferServiceImpl.processScheduledTransfers()` called `transfer()` directly and then built an ad-hoc log message inline. The session methods (`doWithdrawal`, `doDeposit`, `doTransfer`) called service methods directly with no standard way to get a self-describing label for receipts or logs. Operations could not be treated uniformly.

#### Solution
Wrap each operation in a `TransactionCommand<R extends OperationResult>` object. The command carries its own parameters and knows how to describe itself. The caller holds the command, executes it, and uses its metadata without coupling to the service interface.

```
command/
├── OperationResult.java        ← interface: isSuccess(), getMessage()
├── TransactionCommand.java     ← interface: execute(), getType(), describe()
├── WithdrawCommand.java        ← holds Account + amount + WithdrawalService
├── DepositCommand.java         ← holds Account + amount + DepositService
└── TransferCommand.java        ← holds source Account + destAccountNumber + amount + TransferService
```

All result DTOs (`WithdrawalResult`, `DepositResult`, `TransferResult`) implement `OperationResult`, so the scheduler can handle any command result uniformly.

#### How it works

```java
// CustomerSession.java — withdrawal (FR-02)
WithdrawCommand cmd = new WithdrawCommand(account, amount, withdrawalService);
WithdrawalResult result = cmd.execute();
// cmd.getType() → "WITHDRAWAL" — used as receipt label, no hard-coded string here
receiptPrinter.printReceipt(..., cmd.getType(), ...);

// CustomerSession.java — transfer (FR-07)
TransferCommand cmd = new TransferCommand(account, destAccNum, amount, transferService);
TransferResult result = cmd.execute();

// TransferServiceImpl.java — scheduled transfer scheduler (FR-10)
TransferCommand cmd = new TransferCommand(srcOpt.get(), st.getDestAccount(), st.getAmount(), this);
TransferResult result = cmd.execute();           // ← uniform execution
System.out.println("[SCHEDULER] " + cmd.describe() + " → OK");
// cmd.describe() → "TRANSFER 500,000 VND from ACC001 to ACC002"
```

**Benefit:** The scheduler loop is now closed to modification — adding a "scheduled deposit" feature means creating a `ScheduledDepositCommand` and wiring it in, without touching the loop body.

---

### 4. Generic Entity Validation (FR-02, FR-03, FR-07)

#### Problem
Each service implementation contained a long sequence of `if (!condition) return failure(message)` guard clauses before the actual business logic. These sequences:
- Mixed validation concerns with execution logic in the same method
- Duplicated structural boilerplate across three service classes
- Required modifying the service method body to add, remove, or reorder a rule

#### Solution
Extract each rule into a single-responsibility `ValidationRule<T>` — a `@FunctionalInterface` that returns a `ValidationResult`. Rules are registered with a generic `EntityValidator<T>`, which evaluates every rule and returns all failures. The validator is assembled **once** at field initialisation and is shared across all calls because rules are stateless. A `Context` record carries all pre-fetched data so no rule needs to call a repository itself.

```
validation/
├── ValidationRule.java                    ← @FunctionalInterface for one rule
├── ValidationResult.java                  ← valid/invalid result and message
├── EntityValidator.java                   ← generic rule collection and runner
├── withdrawal/
│   ├── WithdrawalContext.java             ← record(account, amount, dailyTotal, atmAvailableCash)
│   ├── DenominationWithdrawalValidator    ← amount is a positive multiple of 50,000 VND
│   ├── SingleWithdrawalLimitValidator     ← amount ≤ MAX_WITHDRAWAL_SINGLE
│   ├── DailyWithdrawalLimitValidator      ← dailyTotal + amount ≤ MAX_WITHDRAWAL_DAILY
│   ├── AccountBalanceValidator            ← account.verifyWithdrawAmount(amount)
│   └── AtmCashValidator                   ← atmAvailableCash ≥ amount
├── deposit/
│   ├── DepositContext.java                ← record(account, amount)
│   ├── DenominationDepositValidator
│   └── SingleDepositLimitValidator
└── transfer/
    ├── TransferContext.java               ← record(source, destAccountNumber, destAccount, amount, dailyTotal)
    ├── SameAccountTransferValidator
    ├── PositiveAmountTransferValidator
    ├── SingleTransferLimitValidator
    ├── DestinationExistsValidator         ← checks ctx.destAccount() != null
    ├── DailyTransferLimitValidator
    └── BalanceTransferValidator
```

#### How it works

```java
// ValidationRule.java — one reusable validation rule
@FunctionalInterface
public interface ValidationRule<T> {
    ValidationResult validate(T entity);
}

// WithdrawalServiceImpl.java — rules assembled once at class level
private final EntityValidator<WithdrawalContext> validator =
    new EntityValidator<WithdrawalContext>()
    .addRule(new DenominationWithdrawalValidator())
    .addRule(new SingleWithdrawalLimitValidator())
    .addRule(new DailyWithdrawalLimitValidator())
    .addRule(new AccountBalanceValidator())
    .addRule(new AtmCashValidator());

// withdraw() — build context, run rules, proceed on pass
public WithdrawalResult withdraw(Account account, long amount) {
    long dailyTotal = txRepo.sumByAccountNumberTypeAndDate(...);
    WithdrawalContext ctx = new WithdrawalContext(account, amount, dailyTotal, cashDispenser.getAvailableCash());

    List<ValidationResult> errors = validator.validate(ctx);
    if (!errors.isEmpty()) return WithdrawalResult.failure(errors.get(0).getErrorMessage());

    // ... dispense cash, update balance, persist transaction
}
```

The transfer chain demonstrates a further benefit: `DestinationExistsValidator` checks `ctx.destAccount() != null`, and because the destination account was pre-fetched into the context, the validated post-validation code can use `ctx.destAccount()` directly — **no second repository lookup**.

**Benefit:** To add a "maximum single deposit ≤ 200,000,000 VND" rule, create one new rule class and register it with `.addRule()`. The service method body and all other rules remain untouched. Unlike the previous short-circuit chain, the generic validator can collect multiple failures; services currently preserve the existing user experience by reporting the first failure.

---

## SOLID Principles Applied

| Principle | Where |
|---|---|
| **SRP** | Each service impl handles one operation type; each validator handles one rule; sessions own only the console flow |
| **OCP** | New account type → new `Account` subclass + new `InterestStrategy`; new validation rule → new validator class; new card state → new `CardState` class |
| **LSP** | `SavingsAccount` and `CurrentAccount` are interchangeable as `Account` everywhere; all `CardState` implementations satisfy the `CardState` contract |
| **ISP** | Repository interfaces are split by entity (`AccountRepository`, `CardRepository`, …); result DTOs expose only what their caller needs |
| **DIP** | `CustomerSession` and `AdminSession` depend on service/repository interfaces; `App.java` is the sole place where concrete implementations are instantiated |

## Assumptions & Trade-offs

- **PIN storage:** Stored in plain text in the data file. A real system would hash with bcrypt/PBKDF2.
- **Denomination algorithm:** Greedy-first depth-first search across 4 denominations with a 50-bill cap. Optimal for the given denomination set.
- **Atomicity for transfers:** Simulated — both accounts are updated in memory before persisting. No true rollback if the process crashes mid-write, which is acceptable for a simulation.
- **Daily limits:** Computed by summing same-day transactions from the in-memory transaction list on each operation. They do not persist as a separate counter but are derived from the transaction log.
- **Interest deduplication:** Prevented by storing the last calculated `YYYY-MM` period on each account.
- **Deposit denominations:** Deposited cash is added to the 50,000 VND denomination bucket for simplicity; real ATMs physically sort deposited bills.
- **Scheduled transfer startup check:** Executed once when the application starts, simulating a daily scheduler trigger.
