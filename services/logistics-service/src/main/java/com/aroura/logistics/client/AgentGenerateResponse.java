package com.aroura.logistics.client;

import lombok.Data;

/**
 * agent-service {@code /internal/agent/generate} 的返回体：{@code {status, msg, data:{content}}}。
 * <p>
 * 改造前是手工 {@code JSON.parseObject} 逐层取 {@code data.content}，字段名拼错只会在运行时
 * 表现为"文案为空"，静默退化成默认文案。声明成类型后，结构不符会在反序列化处就暴露。
 *
 * @author sentinel-ms
 */
@Data
public class AgentGenerateResponse {

    private String status;
    private String msg;
    /** 字段名必须是 data 才能对上 agent 侧的响应体；类型名刻意不叫 Data —— 那会遮蔽 lombok.Data。 */
    private Payload data;

    /** 模型服务成功时返回 content；降级时 agent 侧仍会写 agent_call_log，但 content 可能为空。 */
    @Data
    public static class Payload {
        private String content;
    }
}
