package com.java3y.austin.agent.flow;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Sentinel LiteFlow Rule-DB 配置。
 *
 * @author sentinel
 */
@Data
@ConfigurationProperties(prefix = "sentinel.flow.rule-db")
public class SentinelFlowRuleProperties {

    private boolean enabled;
    private String applicationName = "sentinel";
    private String url;
    private String username;
    private String password;
    private String driverClassName;
    private String tablePrefix = "lf_";
    private boolean autoInitTable = true;
    private int pollSeconds = 3;
    private int reconcileSeconds = 30;
    private int cacheCapacity = 20;
}
