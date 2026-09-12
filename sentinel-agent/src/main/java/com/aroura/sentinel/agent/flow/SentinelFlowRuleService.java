package com.aroura.sentinel.agent.flow;

import com.yomahub.liteflow.publisher.PublishChainRequest;
import com.yomahub.liteflow.publisher.PublishResult;
import com.yomahub.liteflow.publisher.RulePublisher;
import com.yomahub.liteflow.publisher.RulePublisherFactory;
import com.yomahub.liteflow.repository.RuleDbRuntime;
import com.yomahub.liteflow.repository.sql.SqlPublisherConfig;
import com.yomahub.liteflow.repository.vo.ChangeRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LiteFlow DB 规则发布与查询服务。
 *
 * @author sentinel
 */
@Service
@ConditionalOnProperty(prefix = "sentinel.flow.rule-db", name = "enabled", havingValue = "true")
public class SentinelFlowRuleService {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final SentinelFlowRuleProperties properties;

    public SentinelFlowRuleService(JdbcTemplate jdbcTemplate, DataSource dataSource,
                                   SentinelFlowRuleProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
        this.properties = properties;
        if (properties.getTablePrefix() == null
                || !properties.getTablePrefix().matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("LiteFlow 表前缀非法: " + properties.getTablePrefix());
        }
    }

    public Map<String, Object> currentRule() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT chain_id, el_data, route_data, version, content_md5, enable, gmt_modified FROM "
                        + chainTable() + " WHERE application_name = ? AND chain_id = ?",
                properties.getApplicationName(), SentinelFlowConfig.CHAIN_ID);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void bootstrapDefaultChain() {
        if (currentRule() == null) {
            publish(SentinelFlowDefinition.defaultEl(), 0L);
        }
    }

    public Map<String, Object> publish(String el, Long expectedVersion) {
        if (el == null || el.trim().isEmpty()) {
            throw new IllegalArgumentException("LiteFlow EL 不能为空");
        }
        PublishChainRequest request = PublishChainRequest.builder()
                .chainId(SentinelFlowConfig.CHAIN_ID)
                .el(el.trim())
                .expectedVersion(expectedVersion)
                .build();
        PublishResult result;
        try (RulePublisher publisher = RulePublisherFactory.create(SqlPublisherConfig.builder()
                .applicationName(properties.getApplicationName())
                .dataSource(dataSource)
                .tablePrefix(properties.getTablePrefix())
                .build())) {
            result = publisher.publishChain(request);
        }
        RuleDbRuntime.applyChange(new ChangeRecord(result.getSequence(), result.getTargetType(),
                result.getTargetId(), result.getOperation(), result.getVersion()));

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("chainId", result.getTargetId());
        output.put("version", result.getVersion());
        output.put("sequence", result.getSequence());
        output.put("operation", result.getOperation().name());
        return output;
    }

    private String chainTable() {
        return properties.getTablePrefix() + "chain";
    }
}
