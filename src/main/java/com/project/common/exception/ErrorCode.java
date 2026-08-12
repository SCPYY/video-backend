package com.project.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    UNKNOWN_ERROR(10000, "未知错误"),
    PARAM_ERROR(10001, "参数校验失败"),

    USER_NOT_FOUND(20001, "用户不存在"),
    USERNAME_EXISTS(20002, "用户名已存在"),
    EMAIL_EXISTS(20003, "邮箱已注册"),
    PASSWORD_ERROR(20004, "密码错误"),
    ACCOUNT_DISABLED(20005, "账号已被禁用"),
    TOKEN_INVALID(20006, "Token无效"),
    TOKEN_EXPIRED(20007, "Token已过期"),
    LOGIN_LOCKED(20008, "登录尝试次数过多，请稍后再试"),

    CONTENT_NOT_FOUND(30001, "内容不存在"),
    EPISODE_NOT_FOUND(30002, "剧集不存在"),
    CONTENT_OFFLINE(30003, "内容已下架"),

    ORDER_NOT_FOUND(40001, "订单不存在"),
    ORDER_CANNOT_CANCEL(40002, "订单无法取消"),
    ORDER_EXPIRED(40003, "订单已过期"),
    DUPLICATE_ORDER(40004, "请勿重复下单"),

    PAYMENT_FAILED(50001, "支付失败"),
    PAYMENT_SIGNATURE_INVALID(50002, "支付签名验证失败"),
    PAYMENT_AMOUNT_MISMATCH(50003, "支付金额不匹配"),

    NO_ENTITLEMENT(60001, "无访问权限"),
    ENTITLEMENT_EXPIRED(60002, "权限已过期"),

    ADMIN_NOT_FOUND(70001, "管理员不存在"),
    ADMIN_FORBIDDEN(70002, "无操作权限"),

    COMMENT_SENSITIVE(80001, "评论包含违规内容"),
    COMMENT_RATE_LIMIT(80002, "评论过于频繁，请稍后再试"),
    COMMENT_NOT_FOUND(80003, "评论不存在"),
    DANMAKU_RATE_LIMIT(80004, "弹幕发送过于频繁，请稍后再试"),
    DANMAKU_NOT_FOUND(80005, "弹幕不存在"),

    WALLET_NOT_FOUND(90001, "钱包不存在"),
    WALLET_DISABLED(90002, "钱包已被冻结"),
    WALLET_INSUFFICIENT_BALANCE(90003, "钱包余额不足"),
    WALLET_CURRENCY_NOT_SUPPORTED(90004, "不支持的钱包币种"),
    WALLET_DUPLICATE_TRANSACTION(90005, "请勿重复提交钱包交易"),
    ORDER_CANNOT_PAY(90006, "当前订单无法支付"),
    ORDER_CANNOT_REFUND(90007, "当前订单无法退款");

    private final int code;
    private final String msg;

    ErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
