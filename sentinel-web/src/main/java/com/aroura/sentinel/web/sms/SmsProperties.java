package com.aroura.sentinel.web.sms;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "sentinel.sms")
public class SmsProperties {

    private boolean enabled = false;
    private String secretId;
    private String secretKey;
    private String region = "ap-guangzhou";
    private String sdkAppId;
    private String signName;
    private String templateId;
    private String endpoint = "sms.tencentcloudapi.com";
    private boolean devReturnCode = false;
    private long codeTtlSeconds = 300;
    private long sendThrottleSeconds = 60;
    private int maxAttempts = 5;
    private int dailyLimit = 10;
}