package com.aroura.msg.receipt;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * 回执回调用的 RestTemplate。
 * <p>
 * 超时必须设：回执是 fire-and-forget 的辅助信息，但调用发生在发送线程上
 * （除非监听器走异步），没有超时就会因为一个慢下游把整条发送链路拖住。
 *
 * @author sentinel-ms
 */
@Configuration
public class ReceiptRestConfig {

    @Bean
    public RestTemplate receiptRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }
}
