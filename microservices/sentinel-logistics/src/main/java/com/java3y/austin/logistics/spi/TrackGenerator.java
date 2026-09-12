package com.java3y.austin.logistics.spi;

import com.java3y.austin.logistics.enums.LogisticsNode;
import com.java3y.austin.logistics.model.LogisticsOrder;
import com.java3y.austin.logistics.model.LogisticsTrack;

/**
 * 物流轨迹生成 SPI（接口缝）
 * <p>
 * 当前实现 {@code RouteScriptTrackGenerator}（按目的地路由脚本模拟物流商推送）；
 * 接入真实物流商轨迹 API 后，替换实现为「轮询/回调消费」即可，下游状态机/通知/异常链路不变。
 *
 * @author sentinel
 */
public interface TrackGenerator {

    /**
     * 生成下一条轨迹（按状态机合法转移）
     *
     * @return 下一条轨迹；当前节点为终态时返回 null
     */
    LogisticsTrack nextTrack(LogisticsOrder order);

    /**
     * 生成初始轨迹（CREATED 节点）
     */
    LogisticsTrack initialTrack(String orderNo);

    /**
     * 当前节点的最小驻留时长（毫秒），用于「自动推进尊重真实时效」
     */
    long dwellMillis(LogisticsNode node);
}
