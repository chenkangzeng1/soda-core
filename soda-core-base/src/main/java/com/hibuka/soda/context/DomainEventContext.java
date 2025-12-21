package com.hibuka.soda.context;

/**
 * Domain event context, providing context information for domain events.
 *
 * @author kangzeng.ckz
 * @since 2025/7/3
 **/
public class DomainEventContext {
    private static final ThreadLocal<String> requestIdHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> jtiHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> userNameHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> authoritiesHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> callerUidHolder = new ThreadLocal<>();
    private static final ThreadLocal<Integer> hopCountHolder = new ThreadLocal<>();

    /**
     * Sets the request ID for the current thread.
     *
     * @param requestId request ID
     */
    public static void setRequestId(String requestId) {
        requestIdHolder.set(requestId);
    }

    /**
     * Gets the request ID for the current thread.
     *
     * @return request ID, or null if not set
     */
    public static String getRequestId() {
        return requestIdHolder.get();
    }

    /**
     * Sets the JTI for the current thread.
     *
     * @param jti JTI
     */
    public static void setJti(String jti) {
        jtiHolder.set(jti);
    }

    /**
     * Gets the JTI for the current thread.
     *
     * @return JTI, or null if not set
     */
    public static String getJti() {
        return jtiHolder.get();
    }

    /**
     * Sets the user name for the current thread.
     *
     * @param userName user name
     */
    public static void setUserName(String userName) {
        userNameHolder.set(userName);
    }

    /**
     * Gets the user name for the current thread.
     *
     * @return user name, or null if not set
     */
    public static String getUserName() {
        return userNameHolder.get();
    }

    /**
     * Sets the authorities for the current thread.
     *
     * @param authorities authorities
     */
    public static void setAuthorities(String authorities) {
        authoritiesHolder.set(authorities);
    }

    /**
     * Gets the authorities for the current thread.
     *
     * @return authorities, or null if not set
     */
    public static String getAuthorities() {
        return authoritiesHolder.get();
    }

    /**
     * Sets the caller UID for the current thread.
     *
     * @param callerUid caller UID
     */
    public static void setCallerUid(String callerUid) {
        callerUidHolder.set(callerUid);
    }

    /**
     * Gets the caller UID for the current thread.
     *
     * @return caller UID, or null if not set
     */
    public static String getCallerUid() {
        return callerUidHolder.get();
    }

    /**
     * Sets the hop count for the current thread.
     *
     * @param hopCount hop count
     */
    public static void setHopCount(Integer hopCount) {
        hopCountHolder.set(hopCount);
    }

    /**
     * Gets the hop count for the current thread.
     *
     * @return hop count, or null if not set
     */
    public static Integer getHopCount() {
        return hopCountHolder.get();
    }

    /**
     * Clears all context information for the current thread.
     */
    public static void clear() {
        requestIdHolder.remove();
        jtiHolder.remove();
        userNameHolder.remove();
        authoritiesHolder.remove();
        callerUidHolder.remove();
        hopCountHolder.remove();
    }
}