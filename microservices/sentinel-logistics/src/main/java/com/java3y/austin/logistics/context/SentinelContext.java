package com.java3y.austin.logistics.context;

import com.java3y.austin.common.enums.ChannelType;

/**
 * 物流通知上下文载体
 * <p>
 * 统一把「订单号 + 物流节点 + 语言 + 角色」编码到 {@code TaskInfo.bizId}（形如
 * {@code orderNo|node|language|role}），使 Web 层分发与 Handler 层 Agent Action
 * 能从 MQ 序列化后的 TaskInfo 中还原出真实业务上下文，从而让 Agent 决策拿到真实数据。
 *
 * @author sentinel
 */
public final class SentinelContext {

    private static final String SEP = "|";

    private final String orderNo;
    private final String node;
    private final String language;
    private final String role;

    private SentinelContext(String orderNo, String node, String language, String role) {
        this.orderNo = orderNo;
        this.node = node == null || node.trim().isEmpty() ? "DEFAULT_NODE" : node;
        this.language = language == null || language.trim().isEmpty() ? "zh" : language;
        this.role = role == null || role.trim().isEmpty() ? "buyer" : role;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public String getNode() {
        return node;
    }

    public String getLanguage() {
        return language;
    }

    public String getRole() {
        return role;
    }

    public String toBizId() {
        return orderNo + SEP + node + SEP + language + SEP + role;
    }

    public static SentinelContext of(String orderNo, String node, String language, String role) {
        return new SentinelContext(orderNo, node, language, role);
    }

    /**
     * 从 bizId 解析上下文；不含分隔符（如纯 traceId）时返回空上下文
     */
    public static SentinelContext fromBizId(String bizId) {
        if (bizId == null || !bizId.contains(SEP)) {
            return new SentinelContext(null, null, null, null);
        }
        String[] parts = bizId.split("\\" + SEP);
        String orderNo = parts.length > 0 ? parts[0] : null;
        String node = parts.length > 1 ? parts[1] : null;
        String language = parts.length > 2 ? parts[2] : null;
        String role = parts.length > 3 ? parts[3] : null;
        return new SentinelContext(orderNo, node, language, role);
    }

    /**
     * 异常节点 → 异常类型（customs_delay/lost/returned/delivery_failed）
     */
    public static String anomalyType(String node) {
        if (node == null) {
            return "customs_delay";
        }
        switch (node.toUpperCase()) {
            case "LOST":
                return "lost";
            case "RETURNED":
                return "returned";
            case "DELIVERY_FAILED":
                return "delivery_failed";
            case "CUSTOMS_DELAY":
            default:
                return "customs_delay";
        }
    }

    /**
     * 异常节点 → 知识库状态码（匹配 anomaly_knowledge 种子数据，供 RAG 命中）
     */
    public static String anomalyStatusCode(String node) {
        if (node == null) {
            return "CUS-1102";
        }
        switch (node.toUpperCase()) {
            case "LOST":
                return "EXP-0051";
            case "RETURNED":
                return "EXP-0062";
            case "DELIVERY_FAILED":
                return "EXP-0071";
            case "CUSTOMS_DELAY":
            default:
                return "CUS-1102";
        }
    }

    /**
     * 把渠道英文名映射为 austin ChannelType 的 code（push/sms/email/feishu）
     */
    public static Integer channelCode(String channelName) {
        if (channelName == null || channelName.trim().isEmpty()) {
            return null;
        }
        switch (channelName.trim().toLowerCase()) {
            case "push":
                return ChannelType.PUSH.getCode();
            case "sms":
                return ChannelType.SMS.getCode();
            case "email":
                return ChannelType.EMAIL.getCode();
            case "feishu":
                return ChannelType.FEI_SHU_ROBOT.getCode();
            default:
                return null;
        }
    }
}
