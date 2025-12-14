package com.hibuka.soda.foundation.error;

import com.hibuka.soda.foundation.enums.CodeEnum;

/**
 * Base error code definitions, implementing the CodeEnum interface, providing common error codes and messages.
 *
 * @author kangzeng.ckz
 * @since 2025/7/3
 */
public enum BaseErrorCode implements CodeEnum<Integer> {
    SUCCESS(200, "成功"),
    BAD_REQUEST(400, "请求错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务不可用"),
    SYSTEM_ERROR(10000, "系统错误"),
    PARAM_ERROR(10001, "参数错误"),
    BUSINESS_ERROR(10002, "业务错误"),
    NETWORK_ERROR(10003, "网络错误"),
    DATABASE_ERROR(10004, "数据库错误"),
    VALIDATION_ERROR(10005, "验证错误"),
    AUTHENTICATION_ERROR(10006, "认证错误"),
    PERMISSION_ERROR(10007, "权限错误"),
    RESOURCE_ERROR(10008, "资源错误"),
    TIMEOUT_ERROR(10009, "超时错误"),
    CONFLICT_ERROR(10010, "冲突错误"),
    CONFIG_ERROR(10011, "配置错误"),
    RPC_ERROR(10012, "RPC调用错误"),
    CACHE_ERROR(10013, "缓存错误"),
    FILE_ERROR(10014, "文件错误"),
    ENCRYPT_ERROR(10015, "加密错误"),
    DECRYPT_ERROR(10016, "解密错误"),
    SERIALIZATION_ERROR(10017, "序列化错误"),
    DESERIALIZATION_ERROR(10018, "反序列化错误"),
    INVALID_FORMAT(10019, "格式错误"),
    INVALID_STATE(10020, "状态错误"),
    UNSUPPORTED_OPERATION(10021, "不支持的操作"),
    NULL_POINTER_ERROR(10022, "空指针错误"),
    INDEX_OUT_OF_BOUNDS_ERROR(10023, "索引越界错误"),
    DIVISION_BY_ZERO_ERROR(10024, "除零错误"),
    ILLEGAL_ARGUMENT_ERROR(10025, "非法参数错误"),
    ILLEGAL_STATE_ERROR(10026, "非法状态错误"),
    CONCURRENT_ERROR(10027, "并发错误"),
    DEADLOCK_ERROR(10028, "死锁错误"),
    MEMORY_ERROR(10029, "内存错误"),
    STACK_OVERFLOW_ERROR(10030, "栈溢出错误"),
    IO_ERROR(10031, "IO错误"),
    INTERRUPTED_ERROR(10032, "中断错误"),
    CLASS_NOT_FOUND_ERROR(10033, "类未找到错误"),
    NO_SUCH_METHOD_ERROR(10034, "方法未找到错误"),
    ILLEGAL_ACCESS_ERROR(10035, "非法访问错误"),
    INSTANTIATION_ERROR(10036, "实例化错误"),
    TYPE_ERROR(10037, "类型错误"),
    CAST_ERROR(10038, "类型转换错误"),
    VERIFICATION_ERROR(10039, "验证错误"),
    CUSTOM_ERROR(19999, "自定义错误");

    private final Integer code;
    private final String desc;

    BaseErrorCode(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getDesc() {
        return desc;
    }

    /**
     * Gets the description for a given error code.
     *
     * @param code the error code
     * @return the description, or "未知错误" if the code is not found
     */
    public static String getMessage(Integer code) {
        for (BaseErrorCode errorCode : values()) {
            if (errorCode.code.equals(code)) {
                return errorCode.desc;
            }
        }
        return "未知错误";
    }
}