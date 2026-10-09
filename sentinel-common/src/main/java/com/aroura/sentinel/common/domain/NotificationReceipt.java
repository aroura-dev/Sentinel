package com.aroura.sentinel.common.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 投递回执：渠道接口对一次发送的实际结果。
 * <p>
 * 此前这条信息只留在 msg-service 的 sms_record 里，logistics 完全看不到 ——
 * 它把「msg-service 受理」直接记成 {@code SENT}，而受理离真正发出去还有一步：
 * 渠道可能直接拒绝（余额不足、模板未报备、号码非法）。
 * 于是运营看到的「已发送」包含了一批根本没发出去的短信。
 * <p>
 * 回执把这一步补上：logistics 据此把 SENT 推进到 DISPATCHED，或落到 FAILED
 * 交给既有的补偿路径重投。
 *
 * @author sentinel-ms
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationReceipt implements Serializable {

    /**
     * 与 notification_record.trace_id 对应的业务标识（orderNo|node|lang|role）。
     * sms_record 本身不存这个值，所以回执必须由知道它的那一层带出来。
     */
    private String bizId;

    /** 渠道接口是否受理。true 表示已交给渠道，false 表示被拒绝。 */
    private boolean accepted;

    /** 简要说明，落进 notification_record.last_error 便于排查。 */
    private String detail;
}
