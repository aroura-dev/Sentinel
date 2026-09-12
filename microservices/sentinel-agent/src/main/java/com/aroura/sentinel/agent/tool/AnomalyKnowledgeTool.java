package com.aroura.sentinel.agent.tool;

import com.alibaba.fastjson2.JSON;
import com.aroura.sentinel.logistics.dao.KnowledgeDao;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 异常知识库检索工具（Tool Calling / RAG）
 * <p>
 * 供 {@code AnomalyDiagnoseAgent} 与 {@code WorkorderAgent} 调用：根据物流状态码
 * 从 anomaly_knowledge 表检索异常原因、平均滞留时长与处理建议，避免 LLM 凭空猜测。
 *
 * @author sentinel
 */
@Component
public class AnomalyKnowledgeTool {

    private final KnowledgeDao knowledgeDao;

    public AnomalyKnowledgeTool(KnowledgeDao knowledgeDao) {
        this.knowledgeDao = knowledgeDao;
    }

    @Tool("根据物流异常状态码从异常知识库检索异常原因、平均滞留时长与处理建议")
    public String queryKnowledge(@P("物流异常状态码，例如 CUS-1102") String statusCode) {
        Map<String, Object> row = knowledgeDao.queryByStatusCode(statusCode);
        if (row == null) {
            return "未检索到状态码 " + statusCode + " 的知识条目，请基于通用国内物流经验作答。";
        }
        return JSON.toJSONString(row);
    }
}
