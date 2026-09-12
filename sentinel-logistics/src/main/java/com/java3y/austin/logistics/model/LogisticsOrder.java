package com.java3y.austin.logistics.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 物流订单领域对象（对应 logistics_order 表）
 * <p>
 * 字段对齐 PRD v0.7 第七章；TMS 扩展字段见各列注释。
 *
 * @author sentinel
 */
@Data
@Builder
public class LogisticsOrder {

    private Long id;
    private String orderNo;
    private String buyerId;
    private String buyerName;
    private String buyerPhone;
    /**
     * 买家语言：zh
     */
    private String buyerLanguage;
    private Long merchantId;
    private String merchantName;
    /**
     * 目的地
     */
    private String destinationCountry;
    /**
     * 当前物流节点（LogisticsNode.code 的字符串形式）
     */
    private String currentNode;
    private Long channelId;
    private Long carrierId;
    private Long warehouseId;
    private String itemsJson;
    private BigDecimal declaredValue;
    private String declaredCurrency;
    private BigDecimal freightCost;
    private String freightCurrency;
    private Date promiseEta;
    private String slaStatus;
    private String waybillNo;
    private String buyerAddress;
    private String buyerCity;
    private String buyerPostal;
    /**
     * 业务备注（跨角色协同）
     */
    private String businessNotes;
    private Long createdAt;
    private Long updatedAt;
    private Integer isDeleted;
}
