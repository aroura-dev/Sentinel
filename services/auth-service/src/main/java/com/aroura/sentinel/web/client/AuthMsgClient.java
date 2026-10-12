package com.aroura.sentinel.web.client;

import com.aroura.sentinel.ms.web.feign.FeignInternalInterceptor;
import com.aroura.sentinel.service.api.domain.SendRequest;
import com.aroura.sentinel.service.api.domain.SendResponse;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import feign.RequestInterceptor;

/**
 * msg-service 客户端：验证码短信下发（走其公开 {@code POST /send} + 短信模板）。
 * <p>
 * 改造前这里是散装 Map + Hutool {@code HttpRequest} 手拼 JSON —— 全项目唯一一处
 * "用第三方 HTTP 工具打内部服务"的地方，改服务发现时最容易漏掉。
 * 现在与其他跨服务调用一致：Feign 契约 + {@code sentinel-service-api} 的 DTO。
 *
 * <h3>url 占位符沿用已有属性名</h3>
 * {@code ${sms.engine.msg-url:}} —— 沿用旧的属性名，因此 compose 里既有的
 * {@code SMS_ENGINE_MSG_URL} 环境变量继续生效；留空则走 Nacos 服务发现。
 *
 * @author sentinel-ms
 */
@FeignClient(name = "msg-service",
        url = "${sms.engine.msg-url:}",
        configuration = AuthMsgClient.Config.class)
public interface AuthMsgClient {

    @PostMapping("/send")
    SendResponse send(@RequestBody SendRequest request);

    /**
     * 只透传 X-Request-Id，不带服务签名 —— {@code /send} 是无鉴权的公开接口。
     * <p>
     * 刻意不加 {@code @Configuration}：本类会被 {@code @ComponentScan} 扫到
     * （AuthApplication 的扫描根就是 com.aroura.sentinel），加了会变成全局 bean
     * 污染本服务所有 Feign 客户端。
     */
    class Config {
        @Bean
        RequestInterceptor requestIdInterceptor() {
            return new FeignInternalInterceptor(null, null);
        }
    }
}
