package com.training.atm.config.db;

import java.sql.Connection;

@FunctionalInterface
public interface TransactionalOperation<T> {
    T execute(Connection conn) throws Exception;
}