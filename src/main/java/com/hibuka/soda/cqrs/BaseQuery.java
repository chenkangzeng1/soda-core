package com.hibuka.soda.cqrs;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Base class for queries, encapsulates common request parameters, the base parent class for all Queries, for unified extension and management.
 *
 * @author kangzeng.ckz
 * @since 2024/10/24
 **/
@Data
public abstract class BaseQuery<R> implements Query<R> {
    private static final long serialVersionUID = -5532460938533182975L;

    /**
     * Request ID
     */
    @Schema(hidden = true)
    private String requestId;

    /**
     * the aliUid of call open api.
     */
    @Schema(hidden = true)
    protected Long callerUid;
    /**
     * Username
     */
    @Schema(hidden = true)
    private String userName;
    /**
     * Authorities
     */
    @Schema(hidden = true)
    private String authorities;
    /**
     * jwt token id
     */
    @Schema(hidden = true)
    private String jti;

    /**
     * JWT token
     */
    @Schema(hidden = true)
    private String jwt;

    /**
     * tenantId
     */
    @Schema(hidden = true)
    private String tenantId;

    /**
     * userInfo
     */
    @Schema(hidden = true)
    private String userInfo;

    /**
     * extension
     */
    @Schema(hidden = true)
    private String extension;
} 