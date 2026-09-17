package com.aroura.sentinel.web.sms;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class SmsSenderRouter implements SmsSender {

    private final SmsProperties properties;
    private final TencentSmsSender tencentSmsSender;
    private final AliyunSmsSender aliyunSmsSender;

    public SmsSenderRouter(SmsProperties properties, TencentSmsSender tencentSmsSender,
                           AliyunSmsSender aliyunSmsSender) {
        this.properties = properties;
        this.tencentSmsSender = tencentSmsSender;
        this.aliyunSmsSender = aliyunSmsSender;
    }

    @Override
    public String send(String phone, String code) {
        if ("aliyun".equalsIgnoreCase(properties.getProvider())) {
            return aliyunSmsSender.send(phone, code);
        }
        return tencentSmsSender.send(phone, code);
    }
}