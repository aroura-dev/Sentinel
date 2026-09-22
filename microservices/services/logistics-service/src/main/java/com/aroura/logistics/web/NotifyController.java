package com.aroura.logistics.web;

import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.aroura.sentinel.web.annotation.RequireRole;
import com.aroura.logistics.service.LogisticsNotifyService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通知闭环触发（logistics-service）。
 * 纵向切片验收用：手动触发一次「订单节点通知」，观察
 * notification_record(logistics) / agent_call_log(agent) / sms_record(msg) 三库各落行。
 *
 * @author sentinel-ms
 */
@RestController
@RequestMapping("/api/logistics/notify")
@RequireRole({"ADMIN", "OPERATOR"})
@Validated
public class NotifyController {

    @Autowired
    private LogisticsNotifyService notifyService;

    /**
     * 参数设界：orderNo 此前不校验，传空串会插出一条永远匹配不到订单的通知记录；
     * node/role/channel 是自由入参，会原样进入 traceId 与落库字段。
     */
    @PostMapping("/send")
    public Map<String, Object> send(@RequestParam @NotBlank(message = "不能为空")
                                    @Size(max = 64, message = "长度不能超过 64") String orderNo,
                                    @RequestParam(defaultValue = "IN_TRANSIT")
                                    @Size(max = 32, message = "长度不能超过 32") String node,
                                    @RequestParam(defaultValue = "buyer")
                                    @Pattern(regexp = "buyer|merchant|customer_service",
                                            message = "只能是 buyer/merchant/customer_service") String role,
                                    @RequestParam(defaultValue = "sms")
                                    @Pattern(regexp = "sms|push|email|feishu",
                                            message = "只能是 sms/push/email/feishu") String channel) {
        Map<String, Object> row = notifyService.send(orderNo, node, role, channel);
        Map<String, Object> resp = new HashMap<>(4);
        resp.put("orderNo", orderNo);
        resp.put("node", node);
        resp.put("role", role);
        resp.put("notification", row);
        return resp;
    }
}
