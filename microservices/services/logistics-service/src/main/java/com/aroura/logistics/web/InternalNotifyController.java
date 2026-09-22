package com.aroura.logistics.web;

import java.util.HashMap;
import java.util.Map;

import com.aroura.logistics.service.LogisticsNotifyService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内部接口：接收 msg-service 回传的投递回执。
 * <p>
 * 路径在 {@code /internal/**} 下，因此由共享鉴权层要求**服务签名**
 * （见 CurrentUserFilter）—— 与 agent-service 被 logistics 调用时同一套机制，
 * 无需在这里再写一遍鉴权。
 * <p>
 * 不经网关暴露：网关只路由 {@code /api/**}，且服务端口不对宿主机发布。
 *
 * @author sentinel-ms
 */
@RestController
@RequestMapping("/internal/notify")
public class InternalNotifyController {

    @Autowired
    private LogisticsNotifyService notifyService;

    /**
     * body: {bizId, accepted, detail}
     * <p>
     * bizId 即 notification_record.trace_id（orderNo|node|lang|role）——
     * sms_record 本身不存这个值，所以必须由 msg-service 从任务上下文里带回来。
     */
    @PostMapping("/receipt")
    public Map<String, Object> receipt(@RequestBody Map<String, Object> req) {
        String bizId = req.get("bizId") == null ? null : String.valueOf(req.get("bizId"));
        boolean accepted = Boolean.parseBoolean(String.valueOf(req.get("accepted")));
        String detail = req.get("detail") == null ? null : String.valueOf(req.get("detail"));

        notifyService.applyReceipt(bizId, accepted, detail);

        Map<String, Object> resp = new HashMap<>(2);
        resp.put("status", "0");
        resp.put("msg", "ok");
        return resp;
    }
}
