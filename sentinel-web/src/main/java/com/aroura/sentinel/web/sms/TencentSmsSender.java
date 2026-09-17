package com.aroura.sentinel.web.sms;

import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest;
import com.tencentcloudapi.sms.v20210111.models.SendSmsResponse;
import com.tencentcloudapi.sms.v20210111.models.SendStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TencentSmsSender implements SmsSender {

    private final SmsProperties properties;

    public TencentSmsSender(SmsProperties properties) {
        this.properties = properties;
    }

    @Override
    public String send(String phone, String code) {
        if (!properties.isEnabled()) {
            log.warn("[SMS] 腾讯云短信未开启，拒绝伪发送 phone={}", mask(phone));
            return null;
        }
        if (isBlank(properties.getSecretId()) || isBlank(properties.getSecretKey())
                || isBlank(properties.getSdkAppId()) || isBlank(properties.getSignName())
                || isBlank(properties.getTemplateId())) {
            log.error("[SMS] 腾讯云短信配置不完整");
            return null;
        }

        try {
            Credential credential = new Credential(properties.getSecretId(), properties.getSecretKey());
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint(properties.getEndpoint());
            httpProfile.setConnTimeout(5);
            httpProfile.setReadTimeout(10);
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            SmsClient client = new SmsClient(credential, properties.getRegion(), clientProfile);

            SendSmsRequest request = new SendSmsRequest();
            request.setPhoneNumberSet(new String[]{"+86" + phone});
            request.setSmsSdkAppId(properties.getSdkAppId());
            request.setSignName(properties.getSignName());
            request.setTemplateId(properties.getTemplateId());
            request.setTemplateParamSet(new String[]{code});

            SendSmsResponse response = client.SendSms(request);
            SendStatus[] statuses = response == null ? null : response.getSendStatusSet();
            if (statuses == null || statuses.length == 0) {
                log.error("[SMS] 腾讯云无发送结果 phone={}", mask(phone));
                return null;
            }
            SendStatus status = statuses[0];
            boolean success = "Ok".equalsIgnoreCase(status.getCode());
            if (success) {
                log.info("[SMS] 腾讯云发送成功 phone={} serialNo={}", mask(phone), status.getSerialNo());
                return code;
            }
            log.error("[SMS] 腾讯云发送失败 phone={} code={} message={}",
                    mask(phone), status.getCode(), status.getMessage());
            return null;
        } catch (Exception e) {
            log.error("[SMS] 腾讯云发送异常 phone={} error={}", mask(phone), e.getMessage(), e);
            return null;
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