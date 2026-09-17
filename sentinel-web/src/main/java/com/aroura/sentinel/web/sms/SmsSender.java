package com.aroura.sentinel.web.sms;

public interface SmsSender {

    /**
     * 发送验证码并返回实际发送的验证码；失败返回 null。
     */
    String send(String phone, String code);
}