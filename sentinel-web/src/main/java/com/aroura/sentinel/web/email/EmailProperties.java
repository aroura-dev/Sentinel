package com.aroura.sentinel.web.email;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "sentinel.email")
public class EmailProperties {

    private boolean enabled = false;
    private String host;
    private int port = 465;
    private String username;
    private String password;
    private String from;
    private boolean sslEnabled = true;
    private boolean starttlsEnabled = false;
    private int connectionTimeoutSeconds = 10;
    private int timeoutSeconds = 15;
    private long codeTtlSeconds = 300;
    private long sendThrottleSeconds = 60;
    private int maxAttempts = 5;
    private int dailyLimit = 10;
}