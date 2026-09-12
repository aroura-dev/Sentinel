package com.java3y.austin.agent.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 通知模板查询工具（Tool Calling / 模板优先）
 * <p>
 * 【sentinel-ms 改造】message_template 已拆到 msg-service(sentinel_msg)，agent 进程不再直读，
 * 此处模板命中短路改为空实现（返回 null → 走 LLM / 降级路径）；如需模板优先，由 msg-service
 * 提供读接口后改远程调用。其余 Agent 的 Tool 签名保持兼容。
 *
 * @author sentinel-ms
 */
@Component
public class TemplateTool {

    private static final Logger log = LoggerFactory.getLogger(TemplateTool.class);

    public TemplateTool() {
    }

    @Tool("按物流节点和语言从消息平台查询通知模板（agent 侧不直读 msg 库，返回 null 走兜底）")
    public String queryTemplate(@P("物流节点 codeEn，如 IMPORT_CUSTOMS") String node,
                                @P("语言，如 ru/en/es/zh") String language) {
        log.debug("[TemplateTool] msg 库已拆分，跳过模板命中 node={} lang={}", node, language);
        return null;
    }
}
