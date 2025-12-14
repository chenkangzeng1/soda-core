package com.hibuka.soda.foundation.io;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Base request class, providing common fields and validation for all requests.
 *
 * @author kangzeng.ckz
 * @since 2025/7/3
 **/
public class BaseRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "requestId cannot be null")
    private String requestId;

    private String jti;

    private String authorities;

    private String userName;

    private String callerUid;

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

    public String getAuthorities() {
        return authorities;
    }

    public void setAuthorities(String authorities) {
        this.authorities = authorities;
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
}