package com.training.atm.model;

import com.training.atm.model.enums.TransferFrequency;
import com.training.atm.model.enums.TransferStatus;
import com.training.atm.model.state.*;

/**
 * Represents a recurring or one-time scheduled fund transfer.
 *
 * <h3>State pattern</h3>
 * Lifecycle transitions ({@code pause}, {@code resume}, {@code cancel},
 * {@code complete}, {@code fail}) are delegated to a
 * {@link TransferLifecycleState} object.  The caller only needs to know
 * whether an operation is legal ({@code canPause()}, {@code canResume()},
 * etc.) and then invoke the corresponding action method — the state object
 * decides the next state.  This eliminates the
 * {@code if (status == ACTIVE) … else if (status == PAUSED) …} chains that
 * previously appeared in {@link com.training.atm.session.CustomerSession} and
 * {@link com.training.atm.service.impl.TransferServiceImpl}.
 */
public class ScheduledTransfer {
    private final String                 id;
    private final String                 sourceAccount;
    private final String                 destAccount;
    private final long                   amount;
    private final TransferFrequency      frequency;
    private       String                 nextExecutionDate;  // "yyyy-MM-dd"
    private       TransferLifecycleState lifecycleState;
    private final int                    maxRepeat;    // -1 = use endDate
    private       int                    repeatCount;
    private       String                 endDate;      // "yyyy-MM-dd" or ""

    public ScheduledTransfer(String id, String sourceAccount, String destAccount, long amount,
                              TransferFrequency frequency, String nextExecutionDate,
                              TransferLifecycleState lifecycleState,
                              int maxRepeat, int repeatCount, String endDate) {
        this.id                = id;
        this.sourceAccount     = sourceAccount;
        this.destAccount       = destAccount;
        this.amount            = amount;
        this.frequency         = frequency;
        this.nextExecutionDate = nextExecutionDate;
        this.lifecycleState    = lifecycleState;
        this.maxRepeat         = maxRepeat;
        this.repeatCount       = repeatCount;
        this.endDate           = (endDate != null) ? endDate : "";
    }

    // -----------------------------------------------------------------------
    // State-delegating lifecycle queries
    // -----------------------------------------------------------------------
    public boolean canExecute() { return lifecycleState.canExecute(); }
    public boolean canPause()   { return lifecycleState.canPause();   }
    public boolean canResume()  { return lifecycleState.canResume();  }
    public boolean canCancel()  { return lifecycleState.canCancel();  }

    // -----------------------------------------------------------------------
    // State-delegating lifecycle transitions
    // -----------------------------------------------------------------------

    /** Called by the scheduler after a successful execution. */
    public void executeSuccess(boolean isLastExecution) {
        lifecycleState = lifecycleState.onExecuteSuccess(isLastExecution);
    }

    /** Called by the scheduler when execution fails. */
    public void executeFail() {
        lifecycleState = lifecycleState.onExecuteFailure();
    }

    public void pause()    { lifecycleState = lifecycleState.onPause();   }
    public void resume()   { lifecycleState = lifecycleState.onResume();  }
    public void cancel()   { lifecycleState = lifecycleState.onCancel();  }
    public void complete() { lifecycleState = new CompletedTransferState(); }

    // -----------------------------------------------------------------------
    // Serialisation bridge
    // -----------------------------------------------------------------------

    /** Returns the serialisable status for file storage. */
    public TransferStatus getStatus() { return lifecycleState.toStatus(); }

    /**
     * Reconstructs the lifecycle state from a persisted {@link TransferStatus}.
     * Called only by the repository layer during file load.
     */
    public static TransferLifecycleState stateFrom(TransferStatus status) {
        return switch (status) {
            case ACTIVE    -> new ActiveTransferState();
            case PAUSED    -> new PausedTransferState();
            case COMPLETED -> new CompletedTransferState();
            case FAILED    -> new FailedTransferState();
            case CANCELLED -> new CancelledTransferState();
        };
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------
    public String            getId()                  { return id; }
    public String            getSourceAccount()       { return sourceAccount; }
    public String            getDestAccount()         { return destAccount; }
    public long              getAmount()              { return amount; }
    public TransferFrequency getFrequency()           { return frequency; }
    public String            getNextExecutionDate()   { return nextExecutionDate; }
    public int               getMaxRepeat()           { return maxRepeat; }
    public int               getRepeatCount()         { return repeatCount; }
    public String            getEndDate()             { return endDate; }

    public void setNextExecutionDate(String date)  { this.nextExecutionDate = date; }
    public void setRepeatCount(int count)           { this.repeatCount = count; }
    public void setEndDate(String date)             { this.endDate = (date != null) ? date : ""; }
}
