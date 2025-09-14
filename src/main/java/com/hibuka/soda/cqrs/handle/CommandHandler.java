package com.hibuka.soda.cqrs.handle;


import com.hibuka.soda.base.error.BaseException;
import com.hibuka.soda.cqrs.Command;

/**
 * Command handler interface, defines the specific business logic for Command, decoupling command and business.
 *
 * @author kangzeng.ckz
 * @since 2024/10/24
 **/
public interface CommandHandler<C extends Command<R>, R> {
    /**
     * Handles a command.
     * @param command the command to handle
     * @return the result of command handling
     * @throws BaseException if command handling fails
     */
    R handle(C command) throws BaseException;
} 