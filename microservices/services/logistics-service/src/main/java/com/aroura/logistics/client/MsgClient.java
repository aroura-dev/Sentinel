package com.aroura.logistics.client;

import java.util.Collections;
import java.util.Map;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.aroura.sentinel.ms.web.RequestIdFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * msg-service 客户端：投递消息（复用其公开 POST /send，模板按 id）。
 * 仅负责把通知送进消息引擎，是否真实下发/落 sms_record 由 msg-service 负责。
 *
 * @author sentinel-ms
 */
@Component
public class MsgClient {

    private static final Logger log = LoggerFactory.getLogger(MsgClient.class);

    private final RestTemplate restTemplate;
    private final String msgUrl;

    public MsgClient(RestTemplate restTemplate, @Value("${service.msg-url}") String msgUrl) {
        this.restTemplate = restTemplate;
        this.msgUrl = msgUrl;
    }

    /**
     * 提交发送
     *
     * @return 是否被引擎受理（response.code == "0"）
     */
    public boolean send(long templateId, String bizId, String receiver, String orderNo) {
        Map<String, Object> messageParam = new java.util.HashMap<>(4);
        messageParam.put("bizId", bizId);
        messageParam.put("receiver", receiver);
        messageParam.put("variables", Collections.singletonMap("orderNo", orderNo));

        Map<String, Object> req = new java.util.HashMap<>(4);
        req.put("code", "send");
        req.put("messageTemplateId", templateId);
        req.put("messageParam", messageParam);
        try {
            // 透传请求关联 ID，让 msg-service 侧的日志能与本次请求对上
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String requestId = MDC.get(RequestIdFilter.MDC_KEY);
            if (requestId != null && !requestId.isEmpty()) {
                headers.set(RequestIdFilter.HDR_REQUEST_ID, requestId);
            }
            String resp = restTemplate.postForObject(msgUrl + "/send",
                    new HttpEntity<>(req, headers), String.class);
            if (resp == null) {
                return false;
            }
            JSONObject json = JSON.parseObject(resp);
            String code = json == null ? null : json.getString("code");
            return "0".equals(code);
        } catch (Exception e) {
            log.warn("[MsgClient] 提交发送失败 templateId={} orderNo={} err={}", templateId, orderNo, e.getMessage());
            return false;
        }
    }
}
