package com.aroura.sentinel.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 客服路由结果（Agent 6 结构化输出）
 *
 * @author sentinel
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsRouteResult {

    /**
     * 咨询意图：query_track/complaint/refund/other
     */
    private String intent;

    /**
     * 路由决策：auto/human
     */
    private String route;

    /**
     * 回复内容
     */
    private String reply;
}
