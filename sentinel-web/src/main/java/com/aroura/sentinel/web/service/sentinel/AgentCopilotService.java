package com.aroura.sentinel.web.service.sentinel;

import com.aroura.sentinel.agent.service.AgentCallLogService;
import com.aroura.sentinel.logistics.dao.KnowledgeDao;
import com.aroura.sentinel.logistics.dao.LogisticsDao;
import com.aroura.sentinel.web.service.sentinel.tms.TmsDashboardService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Java 智能助手服务：知识库问答、订单轨迹解释和运营指标自然语言查询。
 *
 * <p>问答采用“关键词召回 + 业务数据校验”的轻量 RAG，不依赖外部脚本服务；
 * 分析只映射到固定的只读 SQL，不接受用户文本拼接 SQL。</p>
 */
@Service
public class AgentCopilotService {

    private static final String SUCCESS = "success";
    private static final String DEGRADED = "degraded";
    private static final String METRIC_NOTE = "统计范围为当前数据库未删除订单，基于演示数据实时聚合；不等同于生产报表口径。";

    private final AgentCallLogService logService;
    private final KnowledgeDao knowledgeDao;
    private final LogisticsDao logisticsDao;
    private final TmsDashboardService dashboardService;
    private final JdbcTemplate jdbcTemplate;

    public AgentCopilotService(AgentCallLogService logService,
                               KnowledgeDao knowledgeDao,
                               LogisticsDao logisticsDao,
                               TmsDashboardService dashboardService,
                               JdbcTemplate jdbcTemplate) {
        this.logService = logService;
        this.knowledgeDao = knowledgeDao;
        this.logisticsDao = logisticsDao;
        this.dashboardService = dashboardService;
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 物流知识问答；传入订单号时先查询真实订单轨迹。 */
    public Map<String, Object> chat(String question, String orderNo) {
        if (question == null || question.trim().isEmpty()) {
            throw new IllegalArgumentException("问题不能为空");
        }
        long start = System.currentTimeMillis();
        String traceId = logService.generateTraceId();
        boolean grounded = false;
        StringBuilder answer = new StringBuilder();
        List<Map<String, Object>> sources = findSources(question);
        List<Map<String, Object>> business = new ArrayList<>();

        if (orderNo != null && !orderNo.trim().isEmpty()) {
            Map<String, Object> order = logisticsDao.findOrderByNo(orderNo.trim());
            if (order == null) {
                answer.append("未查询到订单 ").append(orderNo.trim()).append("，请核对订单号。");
            } else {
                grounded = true;
                List<Map<String, Object>> tracks = logisticsDao.listTracks(orderNo.trim());
                String node = stringValue(order.get("current_node"));
                String latestTrack = tracks.isEmpty() ? "暂无轨迹明细"
                        : stringValue(tracks.get(tracks.size() - 1).get("raw_desc"));
                answer.append("订单 ").append(orderNo.trim())
                        .append(" 当前状态为「").append(nodeText(node)).append("」，轨迹 ")
                        .append(tracks.size()).append(" 条。最近一条：").append(latestTrack).append("。");
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("order_no", orderNo.trim());
                item.put("current_node", node);
                item.put("track_summary", latestTrack);
                business.add(item);
            }
        } else if (question.contains("订单号") || question.contains("包裹到哪")) {
            answer.append("请补充订单号，我会结合实时轨迹回答，避免只根据规则猜测物流进度。");
        }

        if (!sources.isEmpty()) {
            grounded = true;
            Map<String, Object> source = sources.get(0);
            answer.append(" 知识库判断：").append(stringValue(source.get("description")));
            String suggestion = stringValue(source.get("suggestion"));
            if (suggestion != null && !suggestion.isEmpty()) {
                answer.append("；处理建议：").append(suggestion);
            }
        }
        if (!grounded) {
            answer.append("当前未命中可核验的知识条目或订单数据，建议转人工客服确认，避免给出确定性结论。");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("answer", answer.toString());
        result.put("degraded", !grounded);
        result.put("sources", sources);
        result.put("business", business);
        result.put("trace_id", traceId);
        long latency = System.currentTimeMillis() - start;
        logService.record("JavaKnowledgeAgent",
                mapOf("question", question, "orderNo", orderNo), result,
                "query_track,query_knowledge", 0, latency, grounded ? SUCCESS : DEGRADED, traceId);
        return result;
    }

    /** 将常见运营问题映射到固定只读指标，返回表格和可解释结论。 */
    public Map<String, Object> analytics(String question) {
        if (question == null || question.trim().isEmpty()) {
            throw new IllegalArgumentException("分析问题不能为空");
        }
        long start = System.currentTimeMillis();
        String traceId = logService.generateTraceId();
        AnalysisData data;
        boolean degraded = false;
        try {
            String q = question.trim();
            if (q.contains("渠道")) {
                data = channelAnalysis();
            } else if (q.contains("承运商")) {
                data = carrierAnalysis();
            } else if (q.contains("状态") || q.contains("节点") || q.contains("分布")) {
                data = statusAnalysis();
            } else if (q.contains("延误") || q.contains("异常") || q.contains("滞留")) {
                data = anomalyAnalysis();
            } else if (q.contains("趋势") || q.contains("近7天") || q.contains("近七天")) {
                data = trendAnalysis();
            } else {
                data = overviewAnalysis();
            }
        } catch (Exception e) {
            degraded = true;
            data = new AnalysisData("实时指标查询失败，当前仅返回可解释的口径说明，请稍后重试。",
                    Arrays.asList("指标", "结果"), Arrays.asList(Arrays.<Object>asList("数据状态", "不可用")));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("answer", data.answer);
        result.put("degraded", degraded);
        result.put("data", mapOf("columns", data.columns, "rows", data.rows));
        result.put("metric_note", METRIC_NOTE);
        result.put("trace_id", traceId);
        long latency = System.currentTimeMillis() - start;
        logService.record("JavaAnalyticsAgent", mapOf("question", question), result,
                "read_metrics", 0, latency, degraded ? DEGRADED : SUCCESS, traceId);
        return result;
    }

    private List<Map<String, Object>> findSources(String question) {
        Map<String, Map<String, Object>> unique = new LinkedHashMap<>();
        for (String keyword : keywords(question)) {
            for (Map<String, Object> row : knowledgeDao.search(keyword, 3)) {
                unique.put(stringValue(row.get("id")), row);
            }
        }
        List<Map<String, Object>> sources = new ArrayList<>();
        for (Map<String, Object> row : unique.values()) {
            Map<String, Object> source = new LinkedHashMap<>();
            source.put("type", row.get("type"));
            source.put("status_code", row.get("status_code"));
            source.put("description", row.get("description"));
            source.put("suggestion", row.get("suggestion"));
            sources.add(source);
            if (sources.size() >= 3) {
                break;
            }
        }
        return sources;
    }

    private Set<String> keywords(String question) {
        Set<String> result = new LinkedHashSet<>();
        addKeyword(result, question, "中转", "中转");
        addKeyword(result, question, "滞留", "滞留");
        addKeyword(result, question, "延误", "延误");
        addKeyword(result, question, "丢件", "丢件");
        addKeyword(result, question, "派送失败", "派送失败");
        addKeyword(result, question, "签收", "签收");
        addKeyword(result, question, "退款", "退款");
        addKeyword(result, question, "投诉", "投诉");
        addKeyword(result, question, "运费", "运费");
        addKeyword(result, question, "时效", "时效");
        if (result.isEmpty()) {
            result.add("物流");
        }
        return result;
    }

    private void addKeyword(Set<String> target, String question, String token, String keyword) {
        if (question.contains(token)) {
            target.add(keyword);
        }
    }

    private AnalysisData channelAnalysis() {
        List<Map<String, Object>> raw = jdbcTemplate.queryForList(
                "SELECT COALESCE(c.channel_name, c.channel_code, '未配置渠道') AS label, COUNT(o.id) AS cnt "
                        + "FROM logistics_order o LEFT JOIN carrier_channel c ON o.channel_id = c.id "
                        + "WHERE o.is_deleted = 0 GROUP BY label ORDER BY cnt DESC, label ASC LIMIT 10");
        List<List<Object>> rows = tableRows(raw, "label", "cnt");
        return new AnalysisData("当前订单量最高的渠道为 " + topLabel(raw) + "；下表为 Top 10 实际订单分布。",
                Arrays.asList("渠道", "订单量"), rows);
    }

    private AnalysisData carrierAnalysis() {
        List<Map<String, Object>> raw = jdbcTemplate.queryForList(
                "SELECT COALESCE(carrier_name, carrier_code, '未分配承运商') AS label, COUNT(*) AS cnt "
                        + "FROM logistics_order WHERE is_deleted = 0 GROUP BY label ORDER BY cnt DESC, label ASC LIMIT 10");
        List<List<Object>> rows = tableRows(raw, "label", "cnt");
        return new AnalysisData("当前发货量最高的承运商为 " + topLabel(raw) + "；结果用于对比履约负载，不代表服务质量。",
                Arrays.asList("承运商", "订单量"), rows);
    }

    private AnalysisData statusAnalysis() {
        List<Map<String, Object>> raw = jdbcTemplate.queryForList(
                "SELECT current_node AS label, COUNT(*) AS cnt FROM logistics_order "
                        + "WHERE is_deleted = 0 GROUP BY current_node ORDER BY cnt DESC");
        for (Map<String, Object> row : raw) {
            row.put("label", nodeText(stringValue(row.get("label"))));
        }
        List<List<Object>> rows = tableRows(raw, "label", "cnt");
        return new AnalysisData("当前订单处于 " + raw.size() + " 个物流节点；具体分布如下，异常节点需结合 SLA 和轨迹继续核查。",
                Arrays.asList("物流节点", "订单量"), rows);
    }

    private AnalysisData anomalyAnalysis() {
        List<Map<String, Object>> raw = jdbcTemplate.queryForList(
                "SELECT current_node AS label, COUNT(*) AS cnt FROM logistics_order "
                        + "WHERE is_deleted = 0 AND current_node IN ('CUSTOMS_DELAY','DELIVERY_FAILED','LOST','RETURNED') "
                        + "GROUP BY current_node ORDER BY cnt DESC");
        for (Map<String, Object> row : raw) {
            row.put("label", nodeText(stringValue(row.get("label"))));
        }
        List<List<Object>> rows = tableRows(raw, "label", "cnt");
        return new AnalysisData("延误或异常订单中，数量最多的是 " + topLabel(raw) + "；建议按节点进入处置流程并核对承运商数据。",
                Arrays.asList("异常类型", "订单量"), rows);
    }

    private AnalysisData trendAnalysis() {
        List<Map<String, Object>> raw = dashboardService.orderTrend();
        List<List<Object>> rows = tableRows(raw, "date", "cnt");
        return new AnalysisData("近 7 天创建订单趋势如下；小样本日期波动仅用于开发验证，不作为增长结论。",
                Arrays.asList("日期", "订单量"), rows);
    }

    private AnalysisData overviewAnalysis() {
        Map<String, Object> overview = dashboardService.overview();
        List<List<Object>> rows = new ArrayList<>();
        rows.add(Arrays.<Object>asList("总订单", number(overview.get("totalOrders"))));
        rows.add(Arrays.<Object>asList("运输中", number(overview.get("inTransit"))));
        rows.add(Arrays.<Object>asList("已签收", number(overview.get("delivered"))));
        rows.add(Arrays.<Object>asList("SLA 风险", number(overview.get("slaRisk"))));
        rows.add(Arrays.<Object>asList("异常订单", number(overview.get("anomaly"))));
        return new AnalysisData("当前共有 " + number(overview.get("totalOrders")) + " 单，其中运输中 "
                + number(overview.get("inTransit")) + " 单，SLA 风险 " + number(overview.get("slaRisk"))
                + " 单；风险指标需要结合时间窗口和样本量解释。",
                Arrays.asList("指标", "数量"), rows);
    }

    private List<List<Object>> tableRows(List<Map<String, Object>> raw, String labelKey, String valueKey) {
        List<List<Object>> rows = new ArrayList<>();
        for (Map<String, Object> row : raw) {
            rows.add(Arrays.<Object>asList(row.get(labelKey), row.get(valueKey)));
        }
        return rows;
    }

    private String topLabel(List<Map<String, Object>> rows) {
        return rows.isEmpty() ? "暂无数据" : stringValue(rows.get(0).get("label"));
    }

    private long number(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).longValue();
        }
        return 0L;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String nodeText(String node) {
        if ("CREATED".equals(node)) return "已创建";
        if ("WAREHOUSE_OUT".equals(node)) return "仓库出库";
        if ("DOMESTIC_PICKED".equals(node)) return "国内揽收";
        if ("EXPORT_CUSTOMS".equals(node)) return "中转分拨";
        if ("IN_TRANSIT".equals(node)) return "干线运输";
        if ("IMPORT_CUSTOMS".equals(node)) return "到达分拨";
        if ("LAST_MILE".equals(node)) return "末端派送";
        if ("DELIVERED".equals(node)) return "已签收";
        if ("CUSTOMS_DELAY".equals(node)) return "中转延误";
        if ("DELIVERY_FAILED".equals(node)) return "派送失败";
        if ("LOST".equals(node)) return "丢件";
        if ("RETURNED".equals(node)) return "退回";
        if ("CANCELED".equals(node)) return "已取消";
        return node;
    }

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) {
            result.put(String.valueOf(values[i]), values[i + 1]);
        }
        return result;
    }

    private static final class AnalysisData {
        private final String answer;
        private final List<String> columns;
        private final List<List<Object>> rows;

        private AnalysisData(String answer, List<String> columns, List<List<Object>> rows) {
            this.answer = answer;
            this.columns = columns;
            this.rows = rows;
        }
    }
}

