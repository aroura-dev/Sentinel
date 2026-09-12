package com.java3y.austin.logistics.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 履约 SLA 状态枚举
 *
 * @author sentinel
 */
@Getter
@AllArgsConstructor
public enum SlaStatus {

    NORMAL("NORMAL", "正常"),
    RISK("RISK", "时效预警"),
    BREACHED("BREACHED", "已违约"),
    NA("NA", "未评估"),
    ;

    private final String code;
    private final String description;
}
