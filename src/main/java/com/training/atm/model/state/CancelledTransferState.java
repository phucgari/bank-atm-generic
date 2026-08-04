package com.training.atm.model.state;

import com.training.atm.model.enums.TransferStatus;

/** Terminal state reached when the customer cancels the scheduled transfer. */
public class CancelledTransferState implements TransferLifecycleState {
    @Override public boolean canExecute() { return false; }
    @Override public boolean canPause()   { return false; }
    @Override public boolean canResume()  { return false; }
    @Override public boolean canCancel()  { return false; }

    @Override public TransferLifecycleState onExecuteSuccess(boolean last) { return this; }
    @Override public TransferLifecycleState onExecuteFailure()              { return this; }
    @Override public TransferLifecycleState onPause()                       { return this; }
    @Override public TransferLifecycleState onResume()                      { return this; }
    @Override public TransferLifecycleState onCancel()                      { return this; }
    @Override public TransferStatus         toStatus()                      { return TransferStatus.CANCELLED; }
}
