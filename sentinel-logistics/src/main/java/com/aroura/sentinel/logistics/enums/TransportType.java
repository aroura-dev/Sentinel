package com.aroura.sentinel.logistics.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 运输方式枚举（承运商/渠道）
 *
 * @author sentinel
 */
@Getter
@AllArgsConstructor
public enum TransportType {

    RAIL("rail", "铁路"),
    AIR("air", "空运"),
    SEA("sea", "海运"),
    EXPRESS("express", "快递"),
    ;

    private final String code;
    private final String description;
}
