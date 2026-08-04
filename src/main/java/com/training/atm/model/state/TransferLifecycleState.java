package com.training.atm.model.state;

import com.training.atm.model.enums.TransferStatus;

/**
 * State interface for the scheduled-transfer lifecycle.
 *
 * Each state knows exactly which operations it permits and what the next
 * state should be.  This eliminates the scattered
 * {@code if (status == ACTIVE) … else if (status == PAUSED) …} chains that
 * previously appeared in {@link com.training.atm.session.CustomerSession} and
 * {@link com.training.atm.service.impl.TransferServiceImpl}.
 */
public interface TransferLifecycleState {
    boolean canExecute();
    boolean canPause();
    boolean canResume();
    boolean canCancel();

    /** Fired when the scheduler executes this transfer. */
    TransferLifecycleState onExecuteSuccess(boolean isLastExecution);

    /** Fired when the scheduler fails to execute this transfer. */
    TransferLifecycleState onExecuteFailure();

    TransferLifecycleState onPause();
    TransferLifecycleState onResume();
    TransferLifecycleState onCancel();

    /** Serialisation hook — maps the state back to the stored {@link TransferStatus}. */
    TransferStatus toStatus();
}
