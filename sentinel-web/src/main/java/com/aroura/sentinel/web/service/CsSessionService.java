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

    /**
     * 记录一次客服咨询
     */
    public void record(String buyerId, String message, JSONObject result) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("buyerId", buyerId);
        entry.put("message", message);
        entry.put("intent", result == null ? null : result.getString("intent"));
        entry.put("route", result == null ? null : result.getString("route"));
        entry.put("reply", result == null ? null : result.getString("reply"));
        entry.put("timestamp", System.currentTimeMillis());
        sessions.addFirst(entry);
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
