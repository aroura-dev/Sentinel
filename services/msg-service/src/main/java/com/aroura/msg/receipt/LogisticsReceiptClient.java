package com.aroura.msg.receipt;

import com.aroura.sentinel.ms.web.feign.FeignInternalInterceptor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import feign.RequestInterceptor;

/**
 * 把投递回执回调给 logistics-service。
 * <p>
 * 这是本服务唯一一条「反向」调用：正常情况下是 logistics 调 msg，而回执需要把
 * 渠道的真实结果送回去。走 Feign 契约而不引入对 logistics 领域库的依赖。
 * <p>
 * 目标是 {@code /internal/**}，因此必须带**服务签名**，与 logistics 调 agent 时同一套机制。
 * 失败由调用方（ReceiptEventListener）吞掉只记日志，不在这里做降级。
 *
 * @author sentinel-ms
 */
@FeignClient(name = "logistics-service",
        url = "${service.logistics-url:}",
        configuration = LogisticsReceiptClient.Config.class)
public interface LogisticsReceiptClient {

    /** fire-and-forget：回执不需要返回值。 */
    @PostMapping("/internal/notify/receipt")
    void report(@RequestBody ReceiptRequest request);

    /**
     * 刻意不加 {@code @Configuration}，理由见 logistics 侧 MsgClient.Config：
     * 本类会被 {@code @ComponentScan} 扫到，加了就会变成全局 bean 污染所有 Feign 客户端。
     */
    class Config {
        @Bean
        RequestInterceptor internalSignInterceptor(@Value("${sentinel.internal.secret:}") String secret) {
            return new FeignInternalInterceptor("svc:msg", secret);
        }
    }
}
