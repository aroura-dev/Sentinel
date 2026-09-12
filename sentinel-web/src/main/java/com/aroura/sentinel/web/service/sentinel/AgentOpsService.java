package com.aroura.sentinel.web.service.sentinel;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.aroura.sentinel.agent.service.AgentCallLogService;
import com.aroura.sentinel.logistics.dao.LogisticsDao;
import com.aroura.sentinel.web.service.SentinelNotifyService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** Java AgentOps 编排：规划、只读诊断、写操作审批和链路汇总。 */
@Service
public class AgentOpsService {
    private static final String SUCCESS = "success";
    private static final String FAILED = "failed";
    private static final String PENDING = "pending_approval";
    private final AgentCallLogService logService;
    private final LogisticsDao logisticsDao;
    private final SentinelNotifyService notifyService;
    private final WorkorderService workorderService;
    private final Map<Long, Proposal> proposals = new ConcurrentHashMap<>();
    private final List<Map<String, Object>> runs = new CopyOnWriteArrayList<>();
    public AgentOpsService(AgentCallLogService logService, LogisticsDao logisticsDao, SentinelNotifyService notifyService, WorkorderService workorderService) { this.logService=logService; this.logisticsDao=logisticsDao; this.notifyService=notifyService; this.workorderService=workorderService; }
    public Map<String,Object> run(String task,String orderNo,String requestedTraceId){ Map<String,Object> order=logisticsDao.findOrderByNo(orderNo); if(order==null) throw new IllegalArgumentException("订单不存在: "+orderNo); String node=String.valueOf(order.getOrDefault("current_node","CUSTOMS_DELAY")); String traceId=requestedTraceId==null||requestedTraceId.trim().isEmpty()?UUID.randomUUID().toString().replace("-",""):requestedTraceId.trim(); String scene="异常自动处置（"+nodeText(node)+"）"; Map<String,Object> run=new LinkedHashMap<>(); run.put("traceId",traceId); run.put("orderNo",orderNo); run.put("scene",scene); run.put("task",task); runs.add(run); requireCallId(logService.recordAndReturnId("java_planner",task,mapOf("scene",scene,"node",node),null,0,0L,SUCCESS,traceId)); List<Map<String,Object>> track=logisticsDao.listTracks(orderNo); String diagnosis=buildDiagnosis(orderNo,node,order,track); String content=buildContent(orderNo,node,order); Long diagnoseId=requireCallId(logService.recordAndReturnId("java_diagnose",mapOf("orderNo",orderNo,"node",node),diagnosis,"read_track,read_knowledge",0,0L,SUCCESS,traceId)); Long contentId=requireCallId(logService.recordAndReturnId("java_content",mapOf("orderNo",orderNo,"lang","zh"),content,"template",0,0L,SUCCESS,traceId)); Long notifyId=propose(traceId,"notification/send",mapOf("orderNo",orderNo,"node",node,"role","buyer","channel","push"),content); Long workorderId=propose(traceId,"workorder/process",mapOf("orderNo",orderNo,"anomalyDesc",diagnosis),"异常自动诊断，需创建或更新问题工单"); List<Map<String,Object>> steps=new ArrayList<>(); steps.add(step("diagnose","异常诊断","read",false,SUCCESS,diagnoseId)); steps.add(step("content","多语言文案（模板）","read",false,SUCCESS,contentId)); steps.add(step("notify","发送通知","write",true,PENDING,notifyId)); steps.add(step("workorder","创建/更新工单","write",true,PENDING,workorderId)); Map<String,Object> result=new LinkedHashMap<>(); result.put("traceId",traceId); result.put("orderNo",orderNo); result.put("scene",scene); result.put("status",PENDING); result.put("plan",steps); return result; }
    public Map<String,Object> tasks(String status){ Map<String,Integer> pendingByTrace=new HashMap<>(); for(Map<String,Object> group:logService.pendingGroups()){ Object trace=group.get("trace_id"); Object pending=group.get("pending"); if(trace!=null&&pending instanceof Number) pendingByTrace.put(String.valueOf(trace),((Number)pending).intValue()); } List<Map<String,Object>> rows=new ArrayList<>(); for(Map<String,Object> run:runs){ String traceId=String.valueOf(run.get("traceId")); int pending=pendingByTrace.getOrDefault(traceId,0); pendingByTrace.remove(traceId); Map<String,Object> row=new LinkedHashMap<>(run); row.put("status",pending>0?"running":"done"); row.put("pendingSteps",pending); row.put("totalSteps",4); rows.add(row); } for(Map.Entry<String,Integer> entry:pendingByTrace.entrySet()){ Map<String,Object> row=new LinkedHashMap<>(); row.put("traceId",entry.getKey()); row.put("orderNo",""); row.put("scene","待审批步骤"); row.put("task",""); row.put("status","running"); row.put("pendingSteps",entry.getValue()); row.put("totalSteps",0); rows.add(row); } if(status!=null&&!status.trim().isEmpty()){ List<Map<String,Object>> filtered=new ArrayList<>(); for(Map<String,Object> row:rows) if(status.equals(row.get("status"))||("pending_approval".equals(status)&&"running".equals(row.get("status")))) filtered.add(row); rows=filtered; } Map<String,Object> result=new LinkedHashMap<>(); result.put("count",rows.size()); result.put("rows",rows); return result; }
    public List<Map<String,Object>> trace(String traceId){ return logService.queryByTraceId(traceId); }
    public Map<String,Object> approve(Long callLogId,String reason){
        Proposal proposal=loadProposal(callLogId);
        try{
            Object result=execute(proposal);
            logService.updateStatus(callLogId,SUCCESS,JSON.toJSONString(mapOf("approvedBy","approver","reason",reason==null?"":reason,"result",result)));
            if(logService.pendingCount(proposal.traceId)==0) writeSummary(proposal.traceId,String.valueOf(proposal.params.get("orderNo")));
            return mapOf("callLogId",callLogId,"action",proposal.action,"resultStatus",SUCCESS,"result",result);
        }catch(Exception e){
            logService.updateStatus(callLogId,FAILED,JSON.toJSONString(mapOf("error",e.getMessage())));
            throw new IllegalStateException("执行失败："+e.getMessage(),e);
        }
    }
    public Map<String,Object> reject(Long callLogId,String reason){
        Proposal proposal=loadProposal(callLogId);
        logService.updateStatus(callLogId,FAILED,JSON.toJSONString(mapOf("approved",false,"reason",reason==null?"人工拒绝":reason)));
        return mapOf("callLogId",callLogId,"traceId",proposal.traceId,"resultStatus",FAILED);
    }
    /** 审批上下文以 agent_call_log.output 为准，服务重启后仍可恢复执行。 */
    private Proposal loadProposal(Long callLogId){
        Proposal cached=proposals.remove(callLogId);
        if(cached!=null) return cached;
        Map<String,Object> row=logService.queryById(callLogId);
        if(row==null) throw new IllegalArgumentException("审批步骤不存在");
        if(!"pending_approval".equals(String.valueOf(row.get("status")))) throw new IllegalArgumentException("该步骤不在待审批状态");
        JSONObject persisted=JSON.parseObject(String.valueOf(row.get("output")));
        if(persisted==null||!persisted.containsKey("action")||!persisted.containsKey("params")) throw new IllegalArgumentException("审批上下文缺失，无法安全执行");
        Map<String,Object> params=new LinkedHashMap<>();
        JSONObject input=persisted.getJSONObject("params");
        if(input!=null) params.putAll(input);
        return new Proposal(callLogId,String.valueOf(row.get("trace_id")),persisted.getString("action"),params);
    }
    public Map<String,Object> health(){ boolean dbOk=true; try{ logService.pendingCount("__health__"); }catch(Exception e){ dbOk=false; } return mapOf("status","UP","db_ok",dbOk); }
    private Object execute(Proposal p){ if("notification/send".equals(p.action)) return notifyService.send(string(p.params,"orderNo"),string(p.params,"node"),string(p.params,"role"),string(p.params,"channel")); if("workorder/process".equals(p.action)) return workorderService.process(string(p.params,"orderNo"),string(p.params,"anomalyDesc")); throw new IllegalArgumentException("未知写能力: "+p.action); }
    private Long propose(String traceId,String action,Map<String,Object> params,String reason){ Map<String,Object> proposal=new LinkedHashMap<>(); proposal.put("action",action); proposal.put("params",params); proposal.put("reason",reason==null?"":reason.substring(0,Math.min(120,reason.length()))); Long callId=requireCallId(logService.recordAndReturnId("java_ops",params,proposal,null,0,0L,PENDING,traceId)); proposals.put(callId,new Proposal(callId,traceId,action,params)); return callId; }
    private void writeSummary(String traceId,String orderNo){ logService.record("java_ops_summary",mapOf("orderNo",orderNo),"订单 "+orderNo+" 异常自动处置完成：已执行通知并创建或更新工单。",null,0,0L,SUCCESS,traceId); }
    private Long requireCallId(Long id){ if(id==null) throw new IllegalStateException("agent_call_log 写入失败"); return id; }
    private Map<String,Object> step(String intent,String label,String mode,boolean needApproval,String status,Long callLogId){ Map<String,Object> step=new LinkedHashMap<>(); step.put("intent",intent); step.put("label",label); step.put("mode",mode); step.put("needApproval",needApproval); step.put("status",status); step.put("callLogId",callLogId); return step; }
    private String buildDiagnosis(String orderNo,String node,Map<String,Object> order,List<Map<String,Object>> track){ String merchant=String.valueOf(order.getOrDefault("merchant_name","商家")); return "订单 "+orderNo+" 当前处于「"+nodeText(node)+"」异常节点（轨迹 "+(track==null?0:track.size())+" 条）：初步判断为运输链路延误，需联系承运商核实原因，并同步 "+merchant+" 与买家预计延迟。"; }
    private String buildContent(String orderNo,String node,Map<String,Object> order){ String merchant=String.valueOf(order.getOrDefault("merchant_name","物流平台")); return "【物流通知】您好，您的包裹（订单 "+orderNo+"）在运输途中因"+nodeText(node)+"将出现延迟，我们已督促承运商优先处理。给您带来不便敬请谅解，最新进展将第一时间通知您。—— "+merchant; }
    private String nodeText(String node){ Map<String,String> labels=new HashMap<>(); labels.put("CREATED","已创建"); labels.put("WAREHOUSE_OUT","仓库出库"); labels.put("DOMESTIC_PICKED","国内揽收"); labels.put("EXPORT_CUSTOMS","中转分拨"); labels.put("IN_TRANSIT","干线运输"); labels.put("IMPORT_CUSTOMS","到达分拨"); labels.put("LAST_MILE","末端派送"); labels.put("DELIVERED","已签收"); labels.put("CUSTOMS_DELAY","中转延误"); labels.put("DELIVERY_FAILED","派送失败"); labels.put("LOST","丢件"); labels.put("RETURNED","退回"); labels.put("CANCELED","已取消"); return labels.getOrDefault(node,node); }
    private String string(Map<String,Object> map,String key){ Object value=map.get(key); return value==null?null:String.valueOf(value); }
    private Map<String,Object> mapOf(Object... values){ Map<String,Object> map=new LinkedHashMap<>(); for(int i=0;i+1<values.length;i+=2) map.put(String.valueOf(values[i]),values[i+1]); return map; }
    private static final class Proposal { private final Long callLogId; private final String traceId; private final String action; private final Map<String,Object> params; private Proposal(Long callLogId,String traceId,String action,Map<String,Object> params){ this.callLogId=callLogId; this.traceId=traceId; this.action=action; this.params=params; } }
}

