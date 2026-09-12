package com.java3y.austin.agent.flow;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 异常处置链默认定义，本地回退和 DB 首次初始化共用同一份 EL。
 *
 * @author sentinel
 */
public final class SentinelFlowDefinition {

    private SentinelFlowDefinition() {
    }

    public static String defaultEl() {
        ClassPathResource resource = new ClassPathResource("flow/sentinel-flow.el");
        try (InputStream input = resource.getInputStream()) {
            return StreamUtils.copyToString(input, StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            throw new IllegalStateException("读取默认 LiteFlow EL 失败", e);
        }
    }
}
