package com.hibuka.soda.core.context;

import org.springframework.beans.factory.ObjectProvider;

public final class CommandContextHolder {
    private static final ThreadLocal<CommandContext> fallback = new ThreadLocal<>();
    private static ObjectProvider<CommandContext> provider;

    private CommandContextHolder() {}

    public static void setProvider(ObjectProvider<CommandContext> p) { provider = p; }

    public static CommandContext get() {
        CommandContext ctx = provider != null ? provider.getIfAvailable() : null;
        if (ctx != null) {
            return ctx;
        }
        return fallback.get();
    }

    public static void set(CommandContext ctx) {
        fallback.set(ctx);
    }

    public static void clear() {
        fallback.remove();
    }
}
