package com.aroura.sentinel.agent.config;

import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Agent 调用遥测采集器（ThreadLocal）
 * <p>
 * 通过 {@code SentinelChatModelListener} 在每次 LLM 调用完成后把 token 消耗与
 * 被调用的 Tool 累加到当前线程的 {@link Telemetry}；Agent 在调用 AiServices 前
 * {@link #begin()}，调用后 {@link #capture()} 取出并清空，随 agent_call_log 落库。
 * <p>
 * 说明：AiServices 在调用线程同步执行模型调用，故 ThreadLocal 足以关联本次调用；
 * 若未来改为异步线程池执行，需替换为 TransmittableThreadLocal 或 traceId 关联。
 *
 * @author sentinel
 */
@Component
public class AgentTelemetry {

    private final ThreadLocal<Telemetry> holder = new ThreadLocal<>();

    /**
     * 开始一次 Agent 调用，重置当前线程的遥测累加器
     */
    public void begin() {
        holder.set(new Telemetry());
    }

    /**
     * 结束并取出当前线程累加的遥测数据，同时清空
     */
    public Telemetry capture() {
        Telemetry telemetry = holder.get();
        holder.remove();
        return telemetry == null ? new Telemetry() : telemetry;
    }

    /**
     * 累加 token 消耗（由 listener 调用）
     */
    public void onTokenUsage(Integer tokens) {
        Telemetry telemetry = holder.get();
        if (telemetry != null && tokens != null) {
            telemetry.totalTokens += tokens;
        }
    }

    /**
     * 记录被调用的 Tool（由 listener 调用）
     */
    public void onToolCalled(String toolName) {
        Telemetry telemetry = holder.get();
        if (telemetry != null && toolName != null) {
            telemetry.tools.add(toolName);
        }
    }

    /**
     * 单次 Agent 调用的遥测数据
     */
    public static class Telemetry {
        private int totalTokens = 0;
        private final Set<String> tools = new LinkedHashSet<>();

        public int getTotalTokens() {
            return totalTokens;
        }

        public Set<String> getTools() {
            return tools;
        }

        /**
         * 逗号连接的 Tool 列表，未调用 Tool 时返回 null
         */
        public String toolsCalled() {
            return tools.isEmpty() ? null : String.join(",", tools);
        }
    }
}
