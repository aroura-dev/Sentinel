package com.java3y.austin.logistics.model.tms;

import lombok.Builder;
import lombok.Data;

/**
 * 商家（卖家）主数据，对应 merchant 表
 *
 * @author sentinel
 */
@Data
@Builder
public class Merchant {

    private Long id;
    private String merchantCode;
    private String merchantName;
    private Long userId;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String country;
    private Integer status;
    private Long createdAt;
    private Long updatedAt;
    private Integer isDeleted;
}
