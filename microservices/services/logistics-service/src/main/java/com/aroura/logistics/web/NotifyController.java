package com.aroura.logistics.web;

import java.util.HashMap;
import java.util.Map;

import com.aroura.sentinel.web.annotation.RequireRole;
import com.aroura.logistics.service.LogisticsNotifyService;

import org.springframework.beans.factory.annotation.Autowired;
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
public class NotifyController {

    @Autowired
    private LogisticsNotifyService notifyService;

    @PostMapping("/send")
    public Map<String, Object> send(@RequestParam String orderNo,
                                    @RequestParam(defaultValue = "IN_TRANSIT") String node,
                                    @RequestParam(defaultValue = "buyer") String role,
                                    @RequestParam(defaultValue = "sms") String channel) {
        Map<String, Object> row = notifyService.send(orderNo, node, role, channel);
        Map<String, Object> resp = new HashMap<>(4);
        resp.put("orderNo", orderNo);
        resp.put("node", node);
        resp.put("role", role);
        resp.put("notification", row);
        return resp;
    }
}
