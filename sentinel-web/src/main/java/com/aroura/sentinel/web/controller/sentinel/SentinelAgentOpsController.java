package com.aroura.sentinel.web.controller.sentinel;

import com.aroura.sentinel.common.vo.BasicResultVO;
import com.aroura.sentinel.web.annotation.RequireRole;
import com.aroura.sentinel.web.service.sentinel.AgentOpsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Java AgentOps API：规划、只读执行、写操作审批和链路查询。 */
@RestController
@RequestMapping("/api/agent/ops")
@Api(tags = "Sentinel Java AgentOps")
@RequireRole({"ADMIN", "OPERATOR"})
public class SentinelAgentOpsController {
    private final AgentOpsService agentOpsService;
    public SentinelAgentOpsController(AgentOpsService agentOpsService){ this.agentOpsService=agentOpsService; }
    @PostMapping("/run") @ApiOperation("启动一次 AgentOps 编排")
    public BasicResultVO run(@RequestBody Map<String,Object> body){ String task=value(body,"task"); String orderNo=value(body,"orderNo"); if(task==null||orderNo==null) return BasicResultVO.fail("task 和 orderNo 不能为空"); try{ return BasicResultVO.success(agentOpsService.run(task,orderNo,value(body,"traceId"))); }catch(Exception e){ return BasicResultVO.fail("编排执行失败："+e.getMessage()); } }
    @GetMapping("/tasks") @ApiOperation("AgentOps 任务列表")
    public BasicResultVO tasks(@RequestParam(required=false) String status){ return BasicResultVO.success(agentOpsService.tasks(status)); }
    @GetMapping("/trace/{traceId}") @ApiOperation("按 traceId 查询 AgentOps 链路")
    public BasicResultVO trace(@PathVariable String traceId){ return BasicResultVO.success(agentOpsService.trace(traceId)); }
    @PostMapping("/steps/{callLogId}/approve") @ApiOperation("审批并执行写步骤")
    public BasicResultVO approve(@PathVariable Long callLogId,@RequestBody(required=false) Map<String,Object> body){ try{ return BasicResultVO.success(agentOpsService.approve(callLogId,value(body,"reason"))); }catch(Exception e){ return BasicResultVO.fail(e.getMessage()); } }
    @PostMapping("/steps/{callLogId}/reject") @ApiOperation("拒绝写步骤")
    public BasicResultVO reject(@PathVariable Long callLogId,@RequestBody(required=false) Map<String,Object> body){ try{ return BasicResultVO.success(agentOpsService.reject(callLogId,value(body,"reason"))); }catch(Exception e){ return BasicResultVO.fail(e.getMessage()); } }
    @GetMapping("/health") @ApiOperation("AgentOps 健康检查")
    public BasicResultVO health(){ return BasicResultVO.success(agentOpsService.health()); }
    private String value(Map<String,Object> body,String key){ if(body==null) return null; Object value=body.get(key); return value==null?null:String.valueOf(value); }
}

