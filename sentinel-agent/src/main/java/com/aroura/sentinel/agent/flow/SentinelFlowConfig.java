package com.aroura.sentinel.agent.flow;

import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.FlowExecutorHolder;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.property.RuleDbCacheConfig;
import com.yomahub.liteflow.property.RuleDbConfig;
import com.yomahub.liteflow.property.RuleDbSqlConfig;
import com.yomahub.liteflow.property.RuleDbSyncConfig;
import com.yomahub.liteflow.repository.RuleDbProviderHolder;
import com.yomahub.liteflow.repository.RuleDbRuntime;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;
import java.util.Map;

/**
 * Sentinel 异常处置编排配置（P1-2，core 模式手动装配）
 * <p>
 * 使用 liteflow-core（不引 starter）避免 Spring Boot 2 / jakarta 冲突；
 * 开启 Rule-DB 时从 SQL 加载并轮询热更新，关闭时使用 classpath EL 回退。
 *
 * @author sentinel
 */
@Configuration
@EnableConfigurationProperties(SentinelFlowRuleProperties.class)
public class SentinelFlowConfig {

    public static final String CHAIN_ID = "sentinelExceptionFlow";

    @Bean
    public FlowExecutor sentinelFlowExecutor(Map<String, NodeComponent> nodeComponents,
                                              SentinelFlowRuleProperties properties,
                                              ObjectProvider<SentinelFlowRuleService> ruleServiceProvider) {
        LiteflowConfig config = new LiteflowConfig();
        config.setPrintBanner(false);
        if (properties.isEnabled()) {
            config.setRuleDb(buildRuleDbConfig(properties));
        } else {
            RuleDbConfig disabled = new RuleDbConfig();
            disabled.setEnabled(false);
            config.setRuleDb(disabled);
        }

        FlowExecutor executor = new FlowExecutor();
        executor.setLiteflowConfig(config);
        // liteflow-core 不提供 Spring 自动装配，必须显式注册 Spring 容器中的节点。
        nodeComponents.forEach(FlowBus::addManagedNode);
        executor.init(true);
        if (properties.isEnabled()) {
            ruleServiceProvider.getObject().bootstrapDefaultChain();
        } else {
            LiteFlowChainELBuilder.createChain()
                    .setChainId(CHAIN_ID)
                    .setEL(SentinelFlowDefinition.defaultEl())
                    .build();
        }
        return executor;
    }

    private RuleDbConfig buildRuleDbConfig(SentinelFlowRuleProperties properties) {
        RuleDbConfig ruleDb = new RuleDbConfig();
        ruleDb.setEnabled(true);
        ruleDb.setApplicationName(properties.getApplicationName());

        RuleDbCacheConfig cache = new RuleDbCacheConfig();
        cache.setCapacity(properties.getCacheCapacity());
        cache.setPreloadChainIds(CHAIN_ID);
        ruleDb.setCache(cache);

        RuleDbSyncConfig sync = new RuleDbSyncConfig();
        sync.setPollSeconds(properties.getPollSeconds());
        sync.setReconcileSeconds(properties.getReconcileSeconds());
        ruleDb.setSync(sync);

        RuleDbSqlConfig sql = new RuleDbSqlConfig();
        sql.setUrl(properties.getUrl());
        sql.setUsername(properties.getUsername());
        sql.setPassword(properties.getPassword());
        sql.setDriverClassName(properties.getDriverClassName());
        sql.setTablePrefix(properties.getTablePrefix());
        sql.setAutoInitTable(properties.isAutoInitTable());
        ruleDb.setSql(sql);
        return ruleDb;
    }

    @PreDestroy
    public void destroy() {
        if (RuleDbRuntime.isActive()) {
            RuleDbRuntime.destroy();
        }
        RuleDbProviderHolder.reset();
        LiteflowConfigGetter.clean();
        FlowExecutorHolder.clean();
        FlowBus.cleanCache();
    }
}
