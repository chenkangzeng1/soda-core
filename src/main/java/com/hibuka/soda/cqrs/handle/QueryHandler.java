package com.hibuka.soda.cqrs.handle;


import com.hibuka.soda.base.error.BaseException;
import com.hibuka.soda.cqrs.Query;

/**
 * Query handler interface, defines the specific business logic for Query, decoupling query and business.
 *
 * @author kangzeng.ckz
 * @since 2024/10/24
 **/
public interface QueryHandler<Q extends Query<R>, R> {
    /**
     * Handles a query.
     * @param query the query to handle
     * @return the result of query handling
     * @throws BaseException if query handling fails
     */
    R handle(Q query) throws BaseException;
} 