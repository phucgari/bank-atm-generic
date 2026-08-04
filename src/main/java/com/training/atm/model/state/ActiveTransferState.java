package com.training.atm.model.state;

import com.training.atm.model.enums.TransferStatus;

public class ActiveTransferState implements TransferLifecycleState {
    @Override public boolean canExecute() { return true;  }
    @Override public boolean canPause()   { return true;  }
    @Override public boolean canResume()  { return false; }
    @Override public boolean canCancel()  { return true;  }

    @Override
    public TransferLifecycleState onExecuteSuccess(boolean isLastExecution) {
        return isLastExecution ? new CompletedTransferState() : this;
    }

    @Override public TransferLifecycleState onExecuteFailure() { return new FailedTransferState(); }
    @Override public TransferLifecycleState onPause()          { return new PausedTransferState(); }
    @Override public TransferLifecycleState onResume()         { return this; } // already active
    @Override public TransferLifecycleState onCancel()         { return new CancelledTransferState(); }
    @Override public TransferStatus         toStatus()         { return TransferStatus.ACTIVE; }
}
