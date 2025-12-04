package com.hibuka.soda.core.context;

import org.springframework.web.context.annotation.RequestScope;

@RequestScope
public class CommandContext {
    private String requestId;
    private Long callerUid;
    private String userName;
    private String authorities;
    private String jti;

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public Long getCallerUid() { return callerUid; }
    public void setCallerUid(Long callerUid) { this.callerUid = callerUid; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getAuthorities() { return authorities; }
    public void setAuthorities(String authorities) { this.authorities = authorities; }
    public String getJti() { return jti; }
    public void setJti(String jti) { this.jti = jti; }
}
