package com.training.atm.repository.db;

import com.training.atm.model.Identifiable;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public interface GenericRepository<T extends Identifiable<ID>, ID> {
    Optional<T> findById(ID id);

    List<T> findAll();

    T save(T entity);

    T update(T entity);

    boolean deleteById(ID id);

    long count();

    boolean existsById(ID id);
}
