package com.ragask.ticketing.common.error;

import lombok.Getter;

/**
 * Business error codes for API responses.
 */
@Getter
public enum ErrorCode {
    BAD_REQUEST(400, "参数错误"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "系统内部错误"),

    L1_CATEGORY_REQUIRED(10001, "转接一线时问题分类为必选项");

    private final int code;
    private final String defaultMessage;

    ErrorCode(int code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }
}

