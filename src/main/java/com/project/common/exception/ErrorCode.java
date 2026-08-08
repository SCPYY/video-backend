package com.project.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // 通用
    UNKNOWN_ERROR(10000, "未知错误"),
    PARAM_ERROR(10001, "参数校验失败"),

    // 用户 2xxxx
    USER_NOT_FOUND(20001, "用户不存在"),
    USERNAME_EXISTS(20002, "用户名已存在"),
    EMAIL_EXISTS(20003, "邮箱已注册"),
    PASSWORD_ERROR(20004, "密码错误"),
    ACCOUNT_DISABLED(20005, "账号已被禁用"),
    TOKEN_INVALID(20006, "Token无效"),
    TOKEN_EXPIRED(20007, "Token已过期"),
    LOGIN_LOCKED(20008, "登录尝试次数过多，请稍后再试"),

    // 内容 3xxxx
    CONTENT_NOT_FOUND(30001, "内容不存在"),
    EPISODE_NOT_FOUND(30002, "剧集不存在"),
    CONTENT_OFFLINE(30003, "内容已下架"),

    // 订单 4xxxx
    ORDER_NOT_FOUND(40001, "订单不存在"),
    ORDER_CANNOT_CANCEL(40002, "订单无法取消"),
    ORDER_EXPIRED(40003, "订单已过期"),
    DUPLICATE_ORDER(40004, "请勿重复下单"),

    // 支付 5xxxx
    PAYMENT_FAILED(50001, "支付失败"),
    PAYMENT_SIGNATURE_INVALID(50002, "支付签名验证失败"),
    PAYMENT_AMOUNT_MISMATCH(50003, "支付金额不匹配"),

    // 权益 6xxxx
    NO_ENTITLEMENT(60001, "无访问权限"),
    ENTITLEMENT_EXPIRED(60002, "权限已过期"),

    // 管理员 7xxxx
    ADMIN_NOT_FOUND(70001, "管理员不存在"),
    ADMIN_FORBIDDEN(70002, "无操作权限"),

    // 社交/评论 8xxxx
    COMMENT_SENSITIVE(80001, "评论包含违规内容"),
    COMMENT_RATE_LIMIT(80002, "评论过于频繁，请稍后再试"),
    COMMENT_NOT_FOUND(80003, "评论不存在");

    private final int code;
    private final String msg;

    ErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
