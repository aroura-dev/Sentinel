package com.aroura.sentinel.web.sms;

public interface SmsSender {

    boolean send(String phone, String code);
}