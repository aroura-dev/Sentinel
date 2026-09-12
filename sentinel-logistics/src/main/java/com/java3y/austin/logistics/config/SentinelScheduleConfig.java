package com.java3y.austin.logistics.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 开启 Spring Scheduling（供 AnomalyScanTask 使用）
 *
 * @author sentinel
 */
@Configuration
@EnableScheduling
public class SentinelScheduleConfig {
}