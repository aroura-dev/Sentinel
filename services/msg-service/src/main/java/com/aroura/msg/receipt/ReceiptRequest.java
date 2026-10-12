package com.aroura.msg.receipt;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 投递回执回调 logistics 的入参。
 * <p>
 * 放在调用方（msg）而非共享库：msg-service 不依赖 sentinel-logistics，
 * 也不该依赖 —— 回执是它唯一关心的那一个契约面，为它抽一个模块不划算。
 *
 * @author sentinel-ms
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptRequest {

    /** 业务 traceId，logistics 用它定位 notification_record。 */
    private String bizId;
    /** 渠道是否受理。false 会触发 logistic 侧的补偿重投。 */
    private boolean accepted;
    /** 拒收原因等补充信息。 */
    private String detail;
}
