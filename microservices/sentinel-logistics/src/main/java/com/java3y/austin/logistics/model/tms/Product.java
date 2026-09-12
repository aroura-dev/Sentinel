package com.java3y.austin.logistics.model.tms;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品 SKU，对应 product 表
 *
 * @author sentinel
 */
@Data
@Builder
public class Product {

    private Long id;
    private Long merchantId;
    private String sku;
    private String name;
    private String hsCode;
    private BigDecimal declaredValue;
    private String currency;
    private BigDecimal weightKg;
    private BigDecimal volumeL;
    private String originCountry;
    private Integer status;
    private Long createdAt;
    private Long updatedAt;
    private Integer isDeleted;
}
