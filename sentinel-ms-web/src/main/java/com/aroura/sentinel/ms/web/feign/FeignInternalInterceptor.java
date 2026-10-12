package com.aroura.sentinel.ms.web.feign;

import com.aroura.sentinel.ms.web.InternalAuth;
import com.aroura.sentinel.ms.web.RequestIdFilter;

import org.slf4j.MDC;

import feign.RequestInterceptor;
import feign.RequestTemplate;

/**
 * 服务间 Feign 出站拦截器：统一注入 {@code X-Request-Id}，按需附加服务签名。
 * <p>
 * 改造前这段逻辑散在 3 个手写客户端里（AgentClient / MsgClient / LogisticsReceiptClient），
 * 每个都自己 set 头、自己算签名 —— 加一条内部调用就得复制一遍，漏了就是静默的鉴权失败。
 * 收敛到一处后，各客户端只需声明"我是谁"，签名算法只有一份（{@link InternalAuth}）。
 *
 * <h3>不要加 @Component / @Configuration</h3>
 * 4 个业务服务的 {@code @ComponentScan} 都覆盖 {@code com.aroura.sentinel.ms.web}，
 * 一旦本类被扫成 bean，它就会成为**该服务所有 Feign 客户端的全局拦截器** ——
 * 于是不签名的客户端也会被加上签名（反之亦然）。正确用法是作为
 * {@code @FeignClient} 接口内的静态嵌套类、由 {@code configuration =} 显式引用。
 *
 * @author sentinel-ms
 */
public class FeignInternalInterceptor implements RequestInterceptor {

    /** 调用方服务身份（如 {@code svc:logistics}）；为 null/空 表示本次调用不签名。 */
    private final String serviceName;
    /** 服务间共享密钥；未配置时不签名（仅供本地无鉴权联调）。 */
    private final String secret;

    public FeignInternalInterceptor(String serviceName, String secret) {
        this.serviceName = serviceName;
        this.secret = secret;
    }

    @Override
    public void apply(RequestTemplate template) {
        // 透传请求关联 ID，否则链路在这一跳就断了，下游日志无从关联。
        // Feign 默认同步调用，MDC 可用；Kafka 监听线程等场景 MDC 为空则不带头（与改造前一致）。
        String requestId = MDC.get(RequestIdFilter.MDC_KEY);
        if (requestId != null && !requestId.isEmpty()) {
            template.header(RequestIdFilter.HDR_REQUEST_ID, requestId);
        }

        // 内部接口（/internal/**）要求服务签名，缺失或错误一律 401。
        // 时间戳参与签名且下游有防重放窗口，故每次都现算。
        if (serviceName != null && !serviceName.isEmpty() && secret != null && !secret.isEmpty()) {
            long ts = System.currentTimeMillis() / 1000L;
            template.header(InternalAuth.HDR_SERVICE, serviceName);
            template.header(InternalAuth.HDR_TIMESTAMP, String.valueOf(ts));
            template.header(InternalAuth.HDR_SIGNATURE,
                    InternalAuth.sign(secret, serviceName, InternalAuth.SERVICE_ROLE, ts));
        }
    }
}
