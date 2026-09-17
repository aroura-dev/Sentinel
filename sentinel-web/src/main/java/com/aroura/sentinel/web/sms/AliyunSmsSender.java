package com.aroura.sentinel.web.sms;

import com.aliyun.auth.credentials.Credential;
import com.aliyun.auth.credentials.provider.StaticCredentialProvider;
import com.aliyun.sdk.service.dypnsapi20170525.AsyncClient;
import com.aliyun.sdk.service.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.sdk.service.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.aliyun.sdk.service.dypnsapi20170525.models.SendSmsVerifyCodeResponseBody;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class AliyunSmsSender implements SmsSender {

    private final SmsProperties properties;

    public AliyunSmsSender(SmsProperties properties) {
        this.properties = properties;
    }

    @Override
    public String send(String phone, String code) {
        if (!properties.isEnabled()) {
            log.warn("[SMS] 阿里云短信认证未开启 phone={}", mask(phone));
            return null;
        }
        if (isBlank(properties.getAliyunAccessKeyId()) || isBlank(properties.getAliyunAccessKeySecret())
                || isBlank(properties.getAliyunSignName()) || isBlank(properties.getAliyunTemplateCode())) {
            log.error("[SMS] 阿里云短信认证配置不完整");
            return null;
        }

        AsyncClient client = null;
        try {
            StaticCredentialProvider provider = StaticCredentialProvider.create(Credential.builder()
                    .accessKeyId(properties.getAliyunAccessKeyId())
                    .accessKeySecret(properties.getAliyunAccessKeySecret())
                    .build());
            client = AsyncClient.builder()
                    .region(properties.getAliyunRegion())
                    .credentialsProvider(provider)
                    .build();

            SendSmsVerifyCodeRequest.Builder builder = SendSmsVerifyCodeRequest.builder()
                    .phoneNumber(phone)
                    .signName(properties.getAliyunSignName())
                    .templateCode(properties.getAliyunTemplateCode())
                    .codeLength(6L)
                    .codeType(1L)
                    .templateParam("{\"code\":\"##code##\",\"min\":\"" + properties.getAliyunValidTime() + "\"}")
                    .returnVerifyCode(true)
                    .validTime(properties.getAliyunValidTime())
                    .interval(properties.getAliyunInterval());
            if (!isBlank(properties.getAliyunSchemeName())) {
                builder.schemeName(properties.getAliyunSchemeName());
            }

            SendSmsVerifyCodeResponse response = client.sendSmsVerifyCode(builder.build())
                    .get(15, TimeUnit.SECONDS);
            SendSmsVerifyCodeResponseBody body = response == null ? null : response.getBody();
            if (body == null || !Boolean.TRUE.equals(body.getSuccess()) || body.getModel() == null) {
                log.error("[SMS] 阿里云短信认证发送失败 phone={} code={} message={}",
                        mask(phone),
                        body == null ? null : body.getCode(),
                        body == null ? null : body.getMessage());
                return null;
            }
            String verifyCode = body.getModel().getVerifyCode();
            if (isBlank(verifyCode)) {
                log.error("[SMS] 阿里云未返回验证码，无法写入本地 Redis phone={}", mask(phone));
                return null;
            }
            log.info("[SMS] 阿里云短信认证发送成功 phone={} bizId={}", mask(phone), body.getModel().getBizId());
            return verifyCode;
        } catch (Exception e) {
            log.error("[SMS] 阿里云短信认证发送异常 phone={} error={}", mask(phone), e.getMessage(), e);
            return null;
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String mask(String phone) {
        if (phone == null || phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}