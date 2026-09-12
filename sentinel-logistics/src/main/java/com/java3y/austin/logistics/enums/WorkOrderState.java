package com.java3y.austin.logistics.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 异常工单状态枚举（状态机状态定义）
 * <p>
 * 与 workorder.status 现有取值保持一致，避免存量数据迁移。
 * 转移规则见 {@link WorkOrderStateTransition}。
 *
 * @author sentinel
 */
@Getter
@AllArgsConstructor
public enum WorkOrderState {

    OPEN(10, "OPEN", "待处理"),
    PROCESSING(20, "PROCESSING", "处理中"),
    PUSHED(30, "PUSHED", "已推送"),
    RESOLVED(70, "RESOLVED", "已解决"),
    CLOSED(80, "CLOSED", "已关闭"),
    ;

    private final int order;
    private final String code;
    private final String description;

    /**
     * 按 code 解析状态，非法返回 null
     */
    public static WorkOrderState fromCode(String code) {
        if (code == null) {
            return null;
        }
        String c = code.trim().toUpperCase();
        for (WorkOrderState s : values()) {
            if (s.code.equals(c)) {
                return s;
            }
        }
        return null;
    }

    public boolean isTerminal() {
        return this == CLOSED;
    }
}