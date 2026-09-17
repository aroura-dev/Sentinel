package com.aroura.sentinel.web.sms;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "sentinel.sms")
public class SmsProperties {

    private boolean enabled = false;
    private String provider = "tencent";

    private String secretId;
    private String secretKey;
    private String region = "ap-guangzhou";
    private String sdkAppId;
    private String signName;
    private String templateId;
    private String endpoint = "sms.tencentcloudapi.com";

    private String aliyunAccessKeyId;
    private String aliyunAccessKeySecret;
    private String aliyunRegion = "cn-hangzhou";
    private String aliyunSignName;
    private String aliyunTemplateCode;
    private String aliyunSchemeName;
    private long aliyunValidTime = 5;
    private long aliyunInterval = 30;

    private boolean devReturnCode = false;
    private long codeTtlSeconds = 300;
    private long sendThrottleSeconds = 60;
    private int maxAttempts = 5;
    private int dailyLimit = 10;
}