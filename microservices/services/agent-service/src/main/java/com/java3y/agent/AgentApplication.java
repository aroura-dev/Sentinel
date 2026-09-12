package com.java3y.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * sentinel-ms AI 服务入口（独立进程 :8084，连 sentinel_agent）。
 * <p>
 * 只装配 sentinel-agent 域中与本进程相关的 bean：
 * ContentGenAgent + AgentCallLogService/DAO + 遥测 + TemplateTool；
 * 剔除其余 5 个 Agent、5 个物流/计费 Tool 与原 LangChain4jConfig（其全量装配 6 assistant
 * 需物流库表/msg 模板），改由本服务 AgentLlmConfig 只建 ContentGenAssistant。
 * message_template 归属 msg-service，agent 不直读。
 *
 * @author sentinel-ms
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.java3y.austin.agent", "com.java3y.agent"}, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.REGEX, pattern =
                "com\\.java3y\\.austin\\.agent\\.agent\\.(AnomalyDiagnose|Workorder|CsRoute|EtaPredict|RouteAdvice)Agent"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern =
                "com\\.java3y\\.austin\\.agent\\.tool\\.(LogisticsQuery|ChannelQuote|EtaPredict|Workorder|AnomalyKnowledge)Tool"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern =
                "com\\.java3y\\.austin\\.agent\\.config\\.LangChain4jConfig")
})
public class AgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentApplication.class, args);
    }
}
