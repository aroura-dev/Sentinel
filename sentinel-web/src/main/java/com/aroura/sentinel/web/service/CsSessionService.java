package com.aroura.sentinel.web.service;

import com.alibaba.fastjson2.JSONObject;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 客服会话存储（内存实现）
 * <p>
 * 记录客服路由 Agent 的每次咨询结果，供管理端「客服 Chat」页面查看最近结果与历史。
 * 演示用内存存储，生产可替换为 Redis / DB。
 *
 * @author sentinel
 */
@Service
public class CsSessionService {

    private static final int MAX_HISTORY = 100;

    private final ConcurrentLinkedDeque<Map<String, Object>> sessions = new ConcurrentLinkedDeque<>();

    public CsSessionService() {
        long now = System.currentTimeMillis();
        addDemo("buyer_1001", "好的，请尽快帮我处理。", "query_track", "auto",
                "已记录，我们会持续跟进 OMT-SEED-0007 的中转分拨进度。", now - 18L * 60 * 1000);
        addDemo("buyer_1001", "我的包裹清关延误三天了，预计什么时候能发出？", "query_track", "auto",
                "订单 OMT-SEED-0007 正在中转分拨，预计 24 小时内更新轨迹。", now - 20L * 60 * 1000);
        addDemo("buyer_1002", "请尽快回复，我比较着急。", "complaint", "human",
                "已提醒人工客服优先处理，请保持电话畅通。", now - 40L * 60 * 1000);
        addDemo("buyer_1002", "包裹已经派送失败两次了，我要求退款。", "complaint", "human",
                "已为您转接人工客服，核实后按售后流程处理退款。", now - 45L * 60 * 1000);
        addDemo("buyer_1003", "订单号是 OMT-SEED-0010。", "change_address", "human",
                "已补充订单信息，人工客服会进一步确认是否可修改。", now - 115L * 60 * 1000);
        addDemo("buyer_1003", "收货地址写错了，现在还能修改吗？", "change_address", "human",
                "订单已进入派送环节，地址变更需人工核实，已转接客服。", now - 120L * 60 * 1000);
        addDemo("buyer_1004", "对应账单是 BILL-AIRGO-202608。", "invoice", "auto",
                "账单 BILL-AIRGO-202608 已提交，预计 3 个工作日内完成开票。", now - 25L * 60 * 60 * 1000);
        addDemo("buyer_1004", "这个月运费账单什么时候可以开发票？", "invoice", "auto",
                "账单已进入财务核验流程，开票后会发送到预留邮箱。", now - 26L * 60 * 60 * 1000);
        addDemo("buyer_1005", "请尽快核实，这个包裹比较贵重。", "complaint", "human",
                "已升级处理并通知承运商核查签收凭证。", now - 49L * 60 * 60 * 1000);
        addDemo("buyer_1005", "物流显示已签收，但我没有收到包裹。", "complaint", "human",
                "已登记异常并转人工客服，将联系承运商核实签收记录。", now - 50L * 60 * 60 * 1000);
    }

    /**
     * 记录一次客服咨询
     */
    public void record(String buyerId, String message, JSONObject result) {
        sessions.addFirst(buildEntry(buyerId, message,
                result == null ? null : result.getString("intent"),
                result == null ? null : result.getString("route"),
                result == null ? null : result.getString("reply"),
                System.currentTimeMillis()));
        trimHistory();
    }

    private void addDemo(String buyerId, String message, String intent, String route,
                         String reply, long timestamp) {
        sessions.addLast(buildEntry(buyerId, message, intent, route, reply, timestamp));
        trimHistory();
    }

    private Map<String, Object> buildEntry(String buyerId, String message, String intent,
                                           String route, String reply, long timestamp) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("buyerId", buyerId);
        entry.put("message", message);
        entry.put("intent", intent);
        entry.put("route", route);
        entry.put("reply", reply);
        entry.put("timestamp", timestamp);
        return entry;
    }

    private void trimHistory() {
        while (sessions.size() > MAX_HISTORY) {
            sessions.pollLast();
        }
    }
    /**
     * 最近一次咨询结果
     */
    public Map<String, Object> lastResult() {
        return sessions.peekFirst();
    }

    /**
     * 咨询历史（最近在前）
     */
    public List<Map<String, Object>> history() {
        return new ArrayList<>(sessions);
    }
}
