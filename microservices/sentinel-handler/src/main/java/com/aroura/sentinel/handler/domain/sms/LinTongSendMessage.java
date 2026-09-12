package com.aroura.sentinel.handler.domain.sms;

import lombok.Builder;
import lombok.Data;

/**
 * <span>Form File</span>
 * <p>Description</p>
 * <p>Company:QQ 752340543</p>
 *
 * @author topsuder
 * @version v1.0.0
 * @DATE 2022/11/24-15:58
 * @Description
 * @see com.aroura.sentinel.handler.domain.sms sentinel
 */
@Data
@Builder
public class LinTongSendMessage {
    String phone;
    String content;
}
