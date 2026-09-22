package com.aroura.sentinel.support.service.impl;

import cn.hutool.core.text.StrPool;
import cn.hutool.setting.dialect.Props;
import com.ctrip.framework.apollo.Config;
import com.aroura.sentinel.support.service.ConfigService;
import com.aroura.sentinel.support.utils.NacosUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;


/**
 * @author Sentinel
 * 读取配置实现类
 */
@Service
public class ConfigServiceImpl implements ConfigService {

    /**
     * 本地配置
     */
    private static final String PROPERTIES_PATH = "local.properties";
    private final Props PROPS = new Props(PROPERTIES_PATH, StandardCharsets.UTF_8);

    /**
     * apollo配置
     */
    @Value("${apollo.bootstrap.enabled}")
    private Boolean enableApollo;
    @Value("${apollo.bootstrap.namespaces}")
    private String namespaces;
    /**
     * nacos配置
     */
    @Value("${sentinel.nacos.enabled}")
    private Boolean enableNacos;
    @Autowired
    private NacosUtils nacosUtils;


    /**
     * Spring 环境（含环境变量与系统属性），用于覆盖打包进 jar 的本地配置。
     */
    @Autowired
    private Environment environment;

    @Override
    public String getProperty(String key, String defaultValue) {
        if (Boolean.TRUE.equals(enableApollo)) {
            Config config = com.ctrip.framework.apollo.ConfigService.getConfig(namespaces.split(StrPool.COMMA)[0]);
            return config.getProperty(key, defaultValue);
        } else if (Boolean.TRUE.equals(enableNacos)) {
            return nacosUtils.getProperty(key, defaultValue);
        }
        // 本地兜底：先看 Spring 环境（环境变量 / JVM 参数），再退回 jar 内的 local.properties。
        //
        // local.properties 是打在 jar 里的资源 —— 放在那里的值（去重规则、流控阈值、渠道权重）
        // 改一次就要重新构建并重新部署镜像，而它们恰恰是运维最需要按环境调整的东西。
        // camelCase 的键按 Spring 的宽松绑定对应大写下划线形式，例如
        // deduplicationRule -> DEDUPLICATION_RULE。
        String fromEnvironment = environment.getProperty(key);
        if (fromEnvironment != null) {
            return fromEnvironment;
        }
        return PROPS.getProperty(key, defaultValue);
    }
}
