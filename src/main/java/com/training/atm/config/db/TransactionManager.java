package com.training.atm.config.db;

public interface TransactionManager {
    <T> T executeInTransaction(TransactionalOperation<T> cb);
}
