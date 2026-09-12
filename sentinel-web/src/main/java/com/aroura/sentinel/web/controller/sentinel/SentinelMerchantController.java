package com.aroura.sentinel.web.controller.sentinel;

import com.aroura.sentinel.common.vo.BasicResultVO;
import com.aroura.sentinel.logistics.dao.LogisticsDao;
import com.aroura.sentinel.logistics.dao.WorkorderDao;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OmniMerchant 联动接口（PRD 8.7，对外提供）
 * <p>
 * 修复：原接口仅存在于前端 Mock，后端补齐。
 *
 * @author sentinel
 */
@RestController
@RequestMapping("/api/omnimerchant")
@Api(tags = "Sentinel OmniMerchant 联动接口")
public class SentinelMerchantController {

    @Autowired
    private LogisticsDao logisticsDao;
    @Autowired
    private WorkorderDao workorderDao;

    @GetMapping("/track/{orderNo}")
    @ApiOperation("OmniMerchant 查询轨迹")
    public BasicResultVO track(@PathVariable String orderNo) {
        Map<String, Object> order = logisticsDao.findOrderByNo(orderNo);
        if (order == null) {
            return BasicResultVO.fail("订单不存在");
        }
        List<Map<String, Object>> tracks = logisticsDao.listTracks(orderNo);
        Map<String, Object> result = new HashMap<>(4);
        result.put("orderNo", orderNo);
        result.put("currentNode", order.get("current_node"));
        result.put("destinationCountry", order.get("destination_country"));
        result.put("buyerLanguage", order.get("buyer_language"));
        result.put("tracks", tracks);
        return BasicResultVO.success(result);
    }

    @PostMapping("/workorder/callback")
    @ApiOperation("OmniMerchant 工单回调")
    public BasicResultVO callback(@RequestBody Map<String, Object> body) {
        Object workorderId = body.get("workorderId");
        Object action = body.get("action");
        if (workorderId == null || action == null) {
            return BasicResultVO.fail("参数缺失：workorderId / action");
        }
        Long id = Long.valueOf(String.valueOf(workorderId));
        Map<String, Object> row = workorderDao.queryById(id);
        if (row == null) {
            return BasicResultVO.fail("工单不存在");
        }
        String status;
        switch (String.valueOf(action)) {
            case "resolve":
                status = "RESOLVED";
                break;
            case "close":
                status = "CLOSED";
                break;
            case "processing":
                status = "PROCESSING";
                break;
            default:
                return BasicResultVO.fail("未知 action");
        }
        workorderDao.updateStatus(id, status);
        Map<String, Object> result = new HashMap<>(4);
        result.put("workorderId", id);
        result.put("status", status);
        result.put("callbackTime", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));
        return BasicResultVO.success(result);
    }
}