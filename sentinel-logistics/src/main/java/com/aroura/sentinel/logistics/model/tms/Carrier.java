package com.aroura.sentinel.logistics.model.tms;

import lombok.Builder;
import lombok.Data;

/**
 * 承运商主数据，对应 carrier 表
 *
 * @author sentinel
 */
@Data
@Builder
public class Carrier {

    private Long id;
    private String carrierCode;
    private String carrierName;
    private String type;
    private String country;
    private String apiEndpoint;
    private String apiKey;
    private Integer status;
    private Long createdAt;
    private Long updatedAt;
    private Integer isDeleted;
}
