package com.training.atm.model.state;

import com.training.atm.model.enums.TransferStatus;

public class PausedTransferState implements TransferLifecycleState {
    @Override public boolean canExecute() { return false; }
    @Override public boolean canPause()   { return false; }
    @Override public boolean canResume()  { return true;  }
    @Override public boolean canCancel()  { return true;  }

    @Override public TransferLifecycleState onExecuteSuccess(boolean last) { return this; } // should not be called
    @Override public TransferLifecycleState onExecuteFailure()              { return this; }
    @Override public TransferLifecycleState onPause()                       { return this; }
    @Override public TransferLifecycleState onResume()                      { return new ActiveTransferState(); }
    @Override public TransferLifecycleState onCancel()                      { return new CancelledTransferState(); }
    @Override public TransferStatus         toStatus()                      { return TransferStatus.PAUSED; }
}
