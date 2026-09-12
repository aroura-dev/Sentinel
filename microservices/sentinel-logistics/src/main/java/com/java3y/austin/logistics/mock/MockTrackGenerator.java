package com.java3y.austin.logistics.mock;

import com.java3y.austin.logistics.enums.LogisticsNode;
import com.java3y.austin.logistics.enums.LogisticsNodeTransition;
import com.java3y.austin.logistics.model.LogisticsOrder;
import com.java3y.austin.logistics.model.LogisticsTrack;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Mock 物流轨迹生成器
 * <p>
 * 用途：开发/演示用，模拟物流商推送的轨迹数据。
 * Mock 数据范围：按订单推进 10 个正常节点 + 4 个异常分支。
 *
 * @author sentinel
 */
public class MockTrackGenerator {

    /**
     * Mock 位置样本（国内物流路径）
     */
    private static final List<String> LOCATIONS = Arrays.asList(
            "深圳分拨中心", "广州分拨中心", "武汉中转中心", "杭州分拨中心",
            "上海分拨中心", "北京分拨中心", "末端网点"
    );

    /**
     * Mock 状态码（模拟物流商原始状态码）
     */
    private static final List<String> RAW_STATUSES = Arrays.asList(
            "EXP-0010", "EXP-0020", "EXP-0030", "EXP-0040", "EXP-0050",
            "EXP-0060", "EXP-0070", "EXP-0080", "CUS-1102", "CUS-1105"
    );

    /**
     * 生成下一条轨迹
     * <p>
     * 按状态机合法转移推进，若当前节点为终态返回 null。
     *
     * @param order 物流订单
     * @return 下一条轨迹，若已终态则返回 null
     */
    public LogisticsTrack nextTrack(LogisticsOrder order) {
        LogisticsNode current = LogisticsNode.getByCodeEn(order.getCurrentNode());
        if (current == null || current.isTerminal()) {
            return null;
        }

        // 从合法转移表中随机选一个目标节点
        List<LogisticsNode> candidates = Arrays.asList(
                LogisticsNodeTransition.nextNodes(current).toArray(new LogisticsNode[0])
        );
        if (candidates.isEmpty()) {
            return null;
        }
        LogisticsNode next = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));

        // 构造 Mock 轨迹
        return LogisticsTrack.builder()
                .orderNo(order.getOrderNo())
                .node(next.getCodeEn())
                .rawStatus(RAW_STATUSES.get(ThreadLocalRandom.current().nextInt(RAW_STATUSES.size())))
                .rawDesc(next.getDescription() + " at " + LOCATIONS.get(
                        ThreadLocalRandom.current().nextInt(LOCATIONS.size())))
                .location(LOCATIONS.get(ThreadLocalRandom.current().nextInt(LOCATIONS.size())))
                .trackTime(System.currentTimeMillis())
                .build();
    }

    /**
     * 生成初始轨迹（CREATED 节点）
     */
    public LogisticsTrack initialTrack(String orderNo) {
        return LogisticsTrack.builder()
                .orderNo(orderNo)
                .node(LogisticsNode.CREATED.getCodeEn())
                .rawStatus("EXP-0010")
                .rawDesc("包裹已在深圳仓创建")
                .location("深圳仓")
                .trackTime(System.currentTimeMillis())
                .build();
    }
}
