package com.aroura.logistics.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * logistics-service 出站 HTTP 客户端（调 agent-service / msg-service）
 *
 * @author sentinel-ms
 */
@Configuration
public class RestConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(3000);
        f.setReadTimeout(8000);
        return new RestTemplate(f);
    }
}
