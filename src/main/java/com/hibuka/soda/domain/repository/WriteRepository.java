package com.hibuka.soda.domain.repository;

import com.hibuka.soda.base.error.BaseException;
import com.hibuka.soda.domain.AbstractAggregateRoot;

public interface WriteRepository<T extends AbstractAggregateRoot> {
    boolean save(T aggregate) throws BaseException;
    boolean update(T aggregate) throws BaseException;
    boolean delete(T aggregate) throws BaseException;
    boolean operate(T aggregate, int operationType) throws BaseException;
}
