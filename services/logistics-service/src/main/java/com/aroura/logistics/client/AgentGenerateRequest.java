package com.aroura.logistics.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 调 agent-service 生成通知文案的入参。
 * <p>
 * 放在调用方（logistics）而非 {@code sentinel-agent}：agent 模块带着 LangChain4j +
 * DashScope，把它的 DTO 抽到共享模块会让物流服务平白背上一整套模型依赖。
 * 这里是纯传输契约，只涉及本模块自己的领域字段。
 *
 * @author sentinel-ms
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentGenerateRequest {

    private String orderNo;
    /** 物流节点（如 IN_TRANSIT） */
    private String node;
    /** 买家语言 */
    private String language;
    /** 商品上下文（目的地/商家） */
    private String productInfo;
    /** 业务 traceId（orderNo|node|lang|role），用于三库关联 */
    private String traceId;
}
