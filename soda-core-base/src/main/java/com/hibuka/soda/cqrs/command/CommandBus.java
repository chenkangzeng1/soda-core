package com.hibuka.soda.cqrs.command;


import com.hibuka.soda.foundation.error.BaseException;
import com.hibuka.soda.cqrs.command.Command;

/**
 * Command bus interface, responsible for dispatching and executing Commands, decoupling commands and handlers, supporting command processing flow under DDD/CQRS architecture.
 *
 * @author kangzeng.ckz
 * @since 2024/10/27
 **/
public interface CommandBus {
    /**
     * Sends a command for execution.
     * @param command the command to send
     * @param <R> the result type
     * @return the result of command execution
     * @throws BaseException if command execution fails
     */
    <R> R send(Command<R> command) throws BaseException;
} 