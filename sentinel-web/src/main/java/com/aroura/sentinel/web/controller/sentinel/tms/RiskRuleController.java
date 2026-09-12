package com.aroura.sentinel.web.controller.sentinel.tms;

import com.aroura.sentinel.common.vo.BasicResultVO;
import com.aroura.sentinel.web.annotation.RequireRole;
import com.aroura.sentinel.web.service.sentinel.tms.RiskRuleService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 风险预警接口
 *
 * @author sentinel
 */
@RestController
@RequestMapping("/api/tms/risk")
@Api(tags = "风险预警")
public class RiskRuleController {

    private final RiskRuleService riskRuleService;

    public RiskRuleController(RiskRuleService riskRuleService) {
        this.riskRuleService = riskRuleService;
    }

    @GetMapping("/rule/list")
    @ApiOperation("处置规则列表")
    @RequireRole({"ADMIN", "OPERATOR"})
    public BasicResultVO list() {
        return BasicResultVO.success(riskRuleService.list());
    }

    @PostMapping("/rule")
    @ApiOperation("新增处置规则")
    @RequireRole({"ADMIN", "OPERATOR"})
    public BasicResultVO save(@RequestBody Map<String, Object> body) {
        return BasicResultVO.success(riskRuleService.save(
                str(body, "name"), str(body, "scene"), str(body, "triggerStatus"),
                str(body, "action"), str(body, "actionConfig"),
                body.get("enabled") == null ? null : Integer.valueOf(String.valueOf(body.get("enabled")))));
    }

    @PutMapping("/rule/{id}")
    @ApiOperation("更新处置规则")
    @RequireRole({"ADMIN", "OPERATOR"})
    public BasicResultVO update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return BasicResultVO.success(riskRuleService.update(id,
                str(body, "name"), str(body, "scene"), str(body, "triggerStatus"),
                str(body, "action"), str(body, "actionConfig"),
                body.get("enabled") == null ? null : Integer.valueOf(String.valueOf(body.get("enabled")))));
    }

    @DeleteMapping("/rule/{id}")
    @ApiOperation("删除处置规则")
    @RequireRole({"ADMIN", "OPERATOR"})
    public BasicResultVO delete(@PathVariable Long id) {
        riskRuleService.delete(id);
        return BasicResultVO.success();
    }

    @GetMapping("/orders")
    @ApiOperation("SLA 风险订单列表")
    @RequireRole({"ADMIN", "OPERATOR", "CUSTOMER_SERVICE", "FINANCE"})
    public BasicResultVO riskOrders(@RequestParam(required = false) String slaStatus,
                                    @RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "10") int perPage) {
        return BasicResultVO.success(riskRuleService.riskOrders(slaStatus, page, perPage));
    }

    @PostMapping("/execute")
    @ApiOperation("执行自动处置动作")
    @RequireRole({"ADMIN", "OPERATOR"})
    public BasicResultVO execute(@RequestParam Long ruleId, @RequestParam String orderNo) {
        return BasicResultVO.success(riskRuleService.execute(ruleId, orderNo));
    }

    private String str(Map<String, Object> body, String key) {
        return body.get(key) == null ? null : String.valueOf(body.get(key));
    }
}
