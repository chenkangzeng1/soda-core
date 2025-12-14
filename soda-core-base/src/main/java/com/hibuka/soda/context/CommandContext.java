package com.hibuka.soda.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Command context, providing a way to store and retrieve context information for commands.
 *
 * @author kangzeng.ckz
 * @since 2025/7/3
 **/
public class CommandContext {
    private final Map<String, Object> context = new ConcurrentHashMap<>();
    private String requestId;
    private String jti;
    private String userName;
    private String callerUid;
    private String[] authorities;

    /**
     * Sets an attribute in the context.
     *
     * @param key attribute key
     * @param value attribute value
     */
    public void setAttribute(String key, Object value) {
        context.put(key, value);
    }

    /**
     * Gets an attribute from the context.
     *
     * @param key attribute key
     * @param <T> attribute type
     * @return attribute value, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) context.get(key);
    }

    /**
     * Gets an attribute from the context, returns defaultValue if not found.
     *
     * @param key attribute key
     * @param defaultValue default value
     * @param <T> attribute type
     * @return attribute value, or defaultValue if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key, T defaultValue) {
        return (T) context.getOrDefault(key, defaultValue);
    }

    /**
     * Removes an attribute from the context.
     *
     * @param key attribute key
     */
    public void removeAttribute(String key) {
        context.remove(key);
    }

    /**
     * Clears all attributes in the context.
     */
    public void clear() {
        context.clear();
    }

    /**
     * Checks if the context contains an attribute with the given key.
     *
     * @param key attribute key
     * @return true if the attribute exists, false otherwise
     */
    public boolean containsAttribute(String key) {
        return context.containsKey(key);
    }

    /**
     * Gets the context as a map.
     *
     * @return context map
     */
    public Map<String, Object> getContext() {
        return context;
    }

    // Getters and setters
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getJti() {
        return jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getCallerUid() {
        return callerUid;
    }

    public void setCallerUid(String callerUid) {
        this.callerUid = callerUid;
    }

    public String[] getAuthorities() {
        return authorities;
    }

    public void setAuthorities(String[] authorities) {
        this.authorities = authorities;
    }
}