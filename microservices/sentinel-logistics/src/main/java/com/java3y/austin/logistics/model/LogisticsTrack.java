package com.java3y.austin.logistics.model;

import lombok.Builder;
import lombok.Data;

/**
 * 物流轨迹领域对象（对应 logistics_track 表）
 *
 * @author sentinel
 */
@Data
@Builder
public class LogisticsTrack {

    private Long id;
    private String orderNo;
    /**
     * 节点（LogisticsNode.codeEn）
     */
    private String node;
    /**
     * 物流商原始状态码
     */
    private String rawStatus;
    /**
     * 物流商原始描述
     */
    private String rawDesc;
    /**
     * 位置
     */
    private String location;
    /**
     * 承运商编码
     */
    private String carrierCode;
    /**
     * 轨迹时间（毫秒）
     */
    private Long trackTime;
    private Long createdAt;
    private Long updatedAt;
    private Integer isDeleted;
}
