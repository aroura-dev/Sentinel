package com.java3y.austin.logistics.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 账单状态枚举（状态机）
 * <p>
 * 流转：DRAFT → SUBMITTED → VERIFIED → SETTLED；SUBMITTED/VERIFIED 可驳回 → REJECTED；REJECTED → DRAFT 重开。
 *
 * @author sentinel
 */
@Getter
@AllArgsConstructor
public enum BillStatus {

    DRAFT("DRAFT", "草稿"),
    SUBMITTED("SUBMITTED", "已提交"),
    VERIFIED("VERIFIED", "已核销"),
    SETTLED("SETTLED", "已结算"),
    REJECTED("REJECTED", "已驳回"),
    ;

    private final String code;
    private final String description;

    /**
     * 校验 from → to 是否合法转移
     */
    public static boolean canTransition(BillStatus from, BillStatus to) {
        if (from == null || to == null) {
            return false;
        }
        switch (from) {
            case DRAFT:
                return to == SUBMITTED;
            case SUBMITTED:
                return to == VERIFIED || to == REJECTED;
            case VERIFIED:
                return to == SETTLED || to == REJECTED;
            case REJECTED:
                return to == DRAFT;
            default:
                return false;
        }
    }
}
