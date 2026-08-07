package com.training.atm.config.db;

import java.util.concurrent.Callable;

public interface TransactionManager {
    <T> T executeInTransaction(Callable<T> cb);
}
