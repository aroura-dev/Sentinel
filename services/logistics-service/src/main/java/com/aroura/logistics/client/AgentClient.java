package com.aroura.logistics.client;

import com.aroura.sentinel.ms.web.feign.FeignInternalInterceptor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import feign.RequestInterceptor;

/**
 * agent-service 客户端：请求 AI 生成通知文案。
 * <p>
 * agent 侧会写 agent_call_log；调用失败由调用方降级为默认文案，不让主链路中断
 * （降级策略留在编排层 {@code LogisticsNotifyService}，此处只做传输）。
 *
 * @author sentinel-ms
 */
@FeignClient(name = "agent-service",
        url = "${service.agent-url:}",
        configuration = AgentClient.Config.class)
public interface AgentClient {

    @PostMapping("/internal/agent/generate")
    AgentGenerateResponse generate(@RequestBody AgentGenerateRequest request);

    /**
     * 目标是 {@code /internal/**}，必须带服务签名（见 CurrentUserFilter）：agent-service 校验
     * X-Service-Name / X-Auth-Timestamp / X-Auth-Signature，缺失或错误一律 401。
     * 未配置密钥时不带签名 —— 仅供本地无鉴权联调。
     * <p>
     * 刻意不加 {@code @Configuration}，理由见 MsgClient.Config。
     */
    class Config {
        @Bean
        RequestInterceptor internalSignInterceptor(@Value("${sentinel.internal.secret:}") String secret) {
            return new FeignInternalInterceptor("svc:logistics", secret);
        }
    }
}
