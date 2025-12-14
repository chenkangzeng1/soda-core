package com.hibuka.soda.context;

import org.springframework.beans.factory.ObjectProvider;

/**
 * Command context holder, providing thread-local access to command contexts.
 *
 * @author kangzeng.ckz
 * @since 2025/7/3
 **/
public class CommandContextHolder {

    private static final ThreadLocal<CommandContext> contextHolder = new ThreadLocal<>();
    private static ObjectProvider<CommandContext> commandContextProvider;

    /**
     * Sets the command context for the current thread.
     *
     * @param context command context
     */
    public static void setContext(CommandContext context) {
        contextHolder.set(context);
    }

    /**
     * Gets the command context for the current thread.
     *
     * @return command context, or null if not set
     */
    public static CommandContext getContext() {
        CommandContext context = contextHolder.get();
        if (context == null && commandContextProvider != null) {
            context = commandContextProvider.getIfAvailable();
            if (context != null) {
                contextHolder.set(context);
            }
        }
        return context;
    }

    /**
     * Clears the command context for the current thread.
     */
    public static void clearContext() {
        contextHolder.remove();
    }

    /**
     * Sets the command context provider.
     *
     * @param provider command context provider
     */
    public static void setProvider(ObjectProvider<CommandContext> provider) {
        commandContextProvider = provider;
    }
}