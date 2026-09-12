package com.java3y.austin.logistics.model.tms;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 承运商账单（承运商 × 账期），对应 bill 表
 *
 * @author sentinel
 */
@Data
@Builder
public class Bill {

    private Long id;
    private String billNo;
    private Long carrierId;
    private Date periodStart;
    private Date periodEnd;
    private String currency;
    private BigDecimal totalAmount;
    /**
     * 状态：DRAFT/SUBMITTED/VERIFIED/SETTLED/REJECTED
     */
    private String status;
    private String remark;
    private String submittedBy;
    private Date submittedAt;
    private String verifiedBy;
    private Date verifiedAt;
    private String settledBy;
    private Date settledAt;
    private String rejectedBy;
    private Date rejectedAt;
    private String rejectReason;
    private Long createdAt;
    private Long updatedAt;
    private Integer isDeleted;
}
