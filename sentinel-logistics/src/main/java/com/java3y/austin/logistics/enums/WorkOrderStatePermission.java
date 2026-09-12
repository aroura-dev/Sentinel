package com.java3y.austin.logistics.enums;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 工单状态流转权限矩阵（P0-3）
 * <p>
 * 约束「哪个角色可以把工单流转到哪个状态」，非法操作返回无权限。
 * role 为空视为系统内部流转（自动化流程），放行。
 *
 * @author sentinel
 */
public final class WorkOrderStatePermission {

    private WorkOrderStatePermission() {
    }

    private static final Map<String, Set<WorkOrderState>> ROLE_TARGETS = new java.util.HashMap<>();

    static {
        ROLE_TARGETS.put("ADMIN", EnumSet.allOf(WorkOrderState.class));
        ROLE_TARGETS.put("CUSTOMER_SERVICE",
                EnumSet.of(WorkOrderState.PROCESSING, WorkOrderState.PUSHED, WorkOrderState.RESOLVED));
        ROLE_TARGETS.put("OPERATOR",
                EnumSet.of(WorkOrderState.PROCESSING, WorkOrderState.PUSHED));
        ROLE_TARGETS.put("FINANCE", Collections.emptySet());
        ROLE_TARGETS.put("MERCHANT", Collections.emptySet());
    }

    public static boolean canChange(String role, WorkOrderState target) {
        if (target == null) {
            return false;
        }
        if (role == null || role.trim().isEmpty()) {
            // 系统内部流转（定时任务/自动化编排）
            return true;
        }
        Set<WorkOrderState> allowed = ROLE_TARGETS.get(role.trim().toUpperCase());
        return allowed != null && allowed.contains(target);
    }

    /**
     * 校验角色是否有权流转到目标状态，无权限抛 SecurityException
     */
    public static void requireAllowed(String role, WorkOrderState target) {
        if (!canChange(role, target)) {
            throw new SecurityException("当前角色无权将工单流转到 " + (target == null ? "未知" : target.getCode()));
        }
    }

    public static Set<WorkOrderState> allowedTargets(String role) {
        if (role == null || role.trim().isEmpty()) {
            return EnumSet.allOf(WorkOrderState.class);
        }
        Set<WorkOrderState> allowed = ROLE_TARGETS.get(role.trim().toUpperCase());
        return allowed == null ? Collections.emptySet() : Collections.unmodifiableSet(allowed);
    }
}