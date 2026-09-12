package com.aroura.sentinel.logistics.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 物流节点枚举（状态机节点）
 * <p>
 * 参考 sentinel 的 ChannelType 枚举设计风格。
 * 节点转移规则见 {@link LogisticsNodeTransition}。
 *
 * @author sentinel
 */
@Getter
@AllArgsConstructor
public enum LogisticsNode {

    /**
     * 下单
     */
    CREATED(10, "CREATED", "下单"),
    /**
     * 仓库出库
     */
    WAREHOUSE_OUT(20, "WAREHOUSE_OUT", "仓库出库"),
    /**
     * 揽收
     */
    DOMESTIC_PICKED(30, "DOMESTIC_PICKED", "揽收"),
    /**
     * 中转分拨
     */
    EXPORT_CUSTOMS(40, "EXPORT_CUSTOMS", "中转分拨"),
    /**
     * 干线运输
     */
    IN_TRANSIT(50, "IN_TRANSIT", "干线运输"),
    /**
     * 到达分拨
     */
    IMPORT_CUSTOMS(60, "IMPORT_CUSTOMS", "到达分拨"),
    /**
     * 末端派送
     */
    LAST_MILE(70, "LAST_MILE", "末端派送"),
    /**
     * 已签收（终态）
     */
    DELIVERED(80, "DELIVERED", "已签收"),

    /* ============ 异常分支 ============ */
    /**
     * 中转延误
     */
    CUSTOMS_DELAY(61, "CUSTOMS_DELAY", "中转延误"),
    /**
     * 派送失败
     */
    DELIVERY_FAILED(71, "DELIVERY_FAILED", "派送失败"),
    /**
     * 丢件（终态）
     */
    LOST(51, "LOST", "丢件"),
    /**
     * 退回（终态）
     */
    RETURNED(62, "RETURNED", "退回"),
    ;

    /**
     * 编码值
     */
    private final Integer code;
    /**
     * 英文标识（物流商原始状态码映射用）
     */
    private final String codeEn;
    /**
     * 中文描述
     */
    private final String description;

    /**
     * 判断是否为终态节点
     */
    public boolean isTerminal() {
        return this == DELIVERED || this == LOST || this == RETURNED;
    }

    /**
     * 判断是否为异常节点
     */
    public boolean isAnomaly() {
        return this == CUSTOMS_DELAY || this == DELIVERY_FAILED
                || this == LOST || this == RETURNED;
    }

    /**
     * 通过 code 获取枚举
     */
    public static LogisticsNode getByCode(Integer code) {
        return Arrays.stream(values())
                .filter(node -> Objects.equals(code, node.getCode()))
                .findFirst().orElse(null);
    }

    /**
     * 通过英文标识获取枚举
     */
    public static LogisticsNode getByCodeEn(String codeEn) {
        return Arrays.stream(values())
                .filter(node -> Objects.equals(codeEn, node.getCodeEn()))
                .findFirst().orElse(null);
    }
}
