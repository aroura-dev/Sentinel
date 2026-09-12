package com.java3y.austin.logistics.enums;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 异常工单状态机转移规则表
 * <p>
 * 自研轻量状态机：枚举 + 转移表，非法迁移直接拒绝。
 * 参考 {@link LogisticsNodeTransition} 的实现风格。
 *
 * @author sentinel
 */
public final class WorkOrderStateTransition {

    private WorkOrderStateTransition() {
    }

    /**
     * 合法转移表：key = 当前状态，value = 可转移到的目标状态集合
     */
    private static final Map<WorkOrderState, Set<WorkOrderState>> TRANSITIONS =
            new EnumMap<>(WorkOrderState.class);

    static {
        TRANSITIONS.put(WorkOrderState.OPEN,
                EnumSet.of(WorkOrderState.PROCESSING, WorkOrderState.PUSHED,
                        WorkOrderState.RESOLVED, WorkOrderState.CLOSED));
        TRANSITIONS.put(WorkOrderState.PROCESSING,
                EnumSet.of(WorkOrderState.PUSHED, WorkOrderState.RESOLVED, WorkOrderState.CLOSED));
        TRANSITIONS.put(WorkOrderState.PUSHED,
                EnumSet.of(WorkOrderState.PROCESSING, WorkOrderState.RESOLVED, WorkOrderState.CLOSED));
        TRANSITIONS.put(WorkOrderState.RESOLVED, EnumSet.of(WorkOrderState.CLOSED));
        TRANSITIONS.put(WorkOrderState.CLOSED, Collections.emptySet());
    }

    public static boolean canTransit(WorkOrderState from, WorkOrderState to) {
        if (from == null || to == null) {
            return false;
        }
        Set<WorkOrderState> targets = TRANSITIONS.get(from);
        return targets != null && targets.contains(to);
    }

    public static Set<WorkOrderState> nextStates(WorkOrderState from) {
        Set<WorkOrderState> targets = TRANSITIONS.get(from);
        return targets == null ? Collections.emptySet() : Collections.unmodifiableSet(targets);
    }

    /**
     * 校验状态流转，非法时抛出业务异常（由上层转成接口失败响应）
     */
    public static void requireValid(WorkOrderState from, WorkOrderState to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("工单状态不能为空");
        }
        if (from == to) {
            throw new IllegalArgumentException("工单已处于 " + from.getCode() + " 状态");
        }
        if (!canTransit(from, to)) {
            throw new IllegalArgumentException(
                    "非法状态迁移: " + from.getCode() + " -> " + to.getCode());
        }
    }
}