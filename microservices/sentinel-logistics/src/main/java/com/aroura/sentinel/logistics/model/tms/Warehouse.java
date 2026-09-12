package com.aroura.sentinel.logistics.model.tms;

import lombok.Builder;
import lombok.Data;

/**
 * 发货仓库，对应 warehouse 表
 *
 * @author sentinel
 */
@Data
@Builder
public class Warehouse {

    private Long id;
    private String warehouseCode;
    private String warehouseName;
    private String country;
    private String city;
    private String address;
    private Integer status;
    private Long createdAt;
    private Long updatedAt;
    private Integer isDeleted;
}
