package com.java3y.austin.logistics.enums;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 物流节点状态机转移规则表
 * <p>
 * 自研轻量版状态机：枚举 + 转移表。
 * 参考 austin 的 ChannelType 枚举设计思路，扩展节点转移合法性校验。
 *
 * @author sentinel
 */
public final class LogisticsNodeTransition {

    private LogisticsNodeTransition() {
    }

    /**
     * 合法转移表：key = 当前节点，value = 可转移到的目标节点集合
     * <p>
     * 规则：
     * - 非法转移自动拒绝（如签收后不可变更）
     * - 异常分支可恢复（如清关延误解除 → 目的地清关）
     */
    private static final Map<LogisticsNode, Set<LogisticsNode>> TRANSITIONS = new EnumMap<>(LogisticsNode.class);

    static {
        // 正常链路
        TRANSITIONS.put(LogisticsNode.CREATED, EnumSet.of(LogisticsNode.WAREHOUSE_OUT));
        TRANSITIONS.put(LogisticsNode.WAREHOUSE_OUT, EnumSet.of(LogisticsNode.DOMESTIC_PICKED));
        TRANSITIONS.put(LogisticsNode.DOMESTIC_PICKED, EnumSet.of(LogisticsNode.EXPORT_CUSTOMS));
        TRANSITIONS.put(LogisticsNode.EXPORT_CUSTOMS, EnumSet.of(LogisticsNode.IN_TRANSIT));
        TRANSITIONS.put(LogisticsNode.IN_TRANSIT, EnumSet.of(LogisticsNode.IMPORT_CUSTOMS, LogisticsNode.LOST));
        TRANSITIONS.put(LogisticsNode.IMPORT_CUSTOMS, EnumSet.of(LogisticsNode.LAST_MILE, LogisticsNode.CUSTOMS_DELAY, LogisticsNode.RETURNED));
        TRANSITIONS.put(LogisticsNode.LAST_MILE, EnumSet.of(LogisticsNode.DELIVERED, LogisticsNode.DELIVERY_FAILED));

        // 异常分支恢复
        TRANSITIONS.put(LogisticsNode.CUSTOMS_DELAY, EnumSet.of(LogisticsNode.IMPORT_CUSTOMS));
        TRANSITIONS.put(LogisticsNode.DELIVERY_FAILED, EnumSet.of(LogisticsNode.LAST_MILE));

        // 终态节点不允许转移
        TRANSITIONS.put(LogisticsNode.DELIVERED, Collections.emptySet());
        TRANSITIONS.put(LogisticsNode.LOST, Collections.emptySet());
        TRANSITIONS.put(LogisticsNode.RETURNED, Collections.emptySet());
    }

    /**
     * 判断转移是否合法
     *
     * @param from 当前节点
     * @param to   目标节点
     * @return true 合法 / false 非法
     */
    public static boolean canTransit(LogisticsNode from, LogisticsNode to) {
        Set<LogisticsNode> targets = TRANSITIONS.get(from);
        return targets != null && targets.contains(to);
    }

    /**
     * 获取某节点可转移到的目标节点集合
     */
    public static Set<LogisticsNode> nextNodes(LogisticsNode from) {
        Set<LogisticsNode> targets = TRANSITIONS.get(from);
        return targets == null ? Collections.emptySet() : Collections.unmodifiableSet(targets);
    }
}
