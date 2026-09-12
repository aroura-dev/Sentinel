package com.java3y.austin.web.controller.sentinel;

import com.java3y.austin.common.vo.BasicResultVO;
import com.java3y.austin.logistics.dao.NotificationDao;
import com.java3y.austin.web.annotation.RequireRole;
import com.java3y.austin.web.service.SentinelNotifyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 通知相关接口（PRD 8.2）
 * <p>
 * 修复：原接口仅存在于前端 Mock，后端补齐并落库 notification_record。
 * 发送逻辑统一收敛到 SentinelNotifyService，与状态机触发共用。
 *
 * @author sentinel
 */
@RestController
@RequestMapping("/api/notification")
@Api(tags = "Sentinel 通知接口")
public class SentinelNotificationController {

    @Autowired
    private NotificationDao notificationDao;
    @Autowired
    private SentinelNotifyService notifyService;

    @GetMapping("/stats")
    @ApiOperation("通知统计")
    public BasicResultVO stats() {
        return BasicResultVO.success(notificationDao.stats());
    }

    @GetMapping("/list")
    @ApiOperation("通知记录分页")
    public BasicResultVO list(@RequestParam(required = false) String orderNo,
                              @RequestParam(required = false) String channel,
                              @RequestParam(required = false) String status,
                              @RequestParam(defaultValue = "1") Integer page,
                              @RequestParam(defaultValue = "10") Integer perPage) {
        return BasicResultVO.success(notificationDao.queryPage(orderNo, channel, status, page, perPage));
    }

    @GetMapping("/{id}")
    @ApiOperation("通知详情")
    public BasicResultVO detail(@PathVariable Long id) {
        Map<String, Object> row = notificationDao.queryById(id);
        return row == null ? BasicResultVO.fail("通知记录不存在") : BasicResultVO.success(row);
    }

    @PostMapping("/{id}/resend")
    @ApiOperation("通知补发（失败重试，跳过 24h 去重）")
    @RequireRole({"ADMIN", "OPERATOR", "CUSTOMER_SERVICE"})
    public BasicResultVO resend(@PathVariable Long id) {
        Map<String, Object> row = notificationDao.queryById(id);
        if (row == null) {
            return BasicResultVO.fail("通知记录不存在");
        }
        Map<String, Object> created = notifyService.send(
                String.valueOf(row.get("order_no")),
                String.valueOf(row.get("node")),
                row.get("role") == null ? "buyer" : String.valueOf(row.get("role")),
                row.get("channel") == null ? "push" : String.valueOf(row.get("channel")),
                true);
        return created == null ? BasicResultVO.fail("补发失败，请稍后重试") : BasicResultVO.success(created);
    }
    @PostMapping("/send")
    @ApiOperation("手动触发通知（Agent 生成文案 + 落库）")
    @RequireRole({"ADMIN", "OPERATOR", "CUSTOMER_SERVICE"})
    public BasicResultVO send(@RequestParam String orderNo,
                              @RequestParam String node,
                              @RequestParam(required = false, defaultValue = "buyer") String role,
                              @RequestParam(required = false, defaultValue = "push") String channel) {
        Map<String, Object> row = notifyService.send(orderNo, node, role, channel);
        return row == null ? BasicResultVO.success() : BasicResultVO.success(row);
    }

    @PostMapping("/{id}/status")
    @ApiOperation("通知状态回执（PENDING/SENT/FAILED，供渠道回执/外部系统调用）")
    public BasicResultVO updateStatus(@PathVariable Long id, @RequestParam String status) {
        Map<String, Object> row = notificationDao.queryById(id);
        if (row == null) {
            return BasicResultVO.fail("通知记录不存在");
        }
        notificationDao.updateStatus(id, status);
        return BasicResultVO.success(true);
    }
}