package com.aroura.logistics.client;

import com.aroura.sentinel.ms.web.feign.FeignInternalInterceptor;
import com.aroura.sentinel.service.api.domain.SendRequest;
import com.aroura.sentinel.service.api.domain.SendResponse;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import feign.RequestInterceptor;

/**
 * msg-service 客户端：投递消息（复用其公开 {@code POST /send}，模板按 id）。
 * <p>
 * 只负责把通知送进消息引擎；是否真实下发、是否落 sms_record 由 msg-service 负责。
 * 请求/响应复用 {@code sentinel-service-api} 的领域契约（SendRequest/SendResponse），
 * 不再手拼 Map + JSON 解析。
 *
 * <h3>url 占位符</h3>
 * {@code ${service.msg-url:}} 默认留空 —— 有值走静态直连（容器化前的联调），
 * 为空则走 Nacos 服务发现 + 负载均衡。切回退只需改 env，不必重建镜像。
 *
 * @author sentinel-ms
 */
@FeignClient(name = "msg-service",
        url = "${service.msg-url:}",
        configuration = MsgClient.Config.class)
public interface MsgClient {

    /**
     * 提交发送。
     *
     * @return msg-service 的受理结果；{@code code == "0"} 表示已被受理
     */
    @PostMapping("/send")
    SendResponse send(@RequestBody SendRequest request);

    /**
     * 只透传 X-Request-Id，不带服务签名 —— {@code /send} 是设计上无鉴权的公开接口。
     * <p>
     * 刻意不加 {@code @Configuration}：本类会被 {@code @ComponentScan} 扫到
     * （服务的扫描根覆盖 {@code com.aroura.sentinel.ms.web}），加了就会变成全局 bean，
     * 把不签名的拦截器套到该服务所有 Feign 客户端上。
     */
    class Config {
        @Bean
        RequestInterceptor requestIdInterceptor() {
            return new FeignInternalInterceptor(null, null);
        }
    }
}
