package com.java3y.austin.logistics.mock;

import com.java3y.austin.logistics.enums.LogisticsNode;
import com.java3y.austin.logistics.enums.LogisticsNodeTransition;
import com.java3y.austin.logistics.model.LogisticsOrder;
import com.java3y.austin.logistics.model.LogisticsTrack;
import com.java3y.austin.logistics.spi.TrackGenerator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 按目的地省份路由脚本的物流轨迹生成器（真实业务规则版，国内物流）
 * <p>
 * 取代旧的纯随机生成，修复三类"demo 感"：
 * 1. 状态码与节点对齐（不再"仓库出库"却挂"中转延误"状态码），且与 anomaly_knowledge 知识库一致；
 * 2. 位置按国内干线真实路由（深圳发货 → 广州分拨 → 各省分拨 → 末端派送）；
 * 3. 转移带权重（异常低概率：丢件 ~1%、退回 ~2%、派送失败 ~9%、中转延误 ~15%），
 *    并提供节点驻留时长供自动推进尊重真实时效。
 * <p>
 * 轨迹描述为中文（贴近国内快递查询页文案）。接入真实物流商后以 SPI 实现替换。
 *
 * @author sentinel
 */
@Component
public class RouteScriptTrackGenerator implements TrackGenerator {

    /** 节点 → 标准原始状态码（与 anomaly_knowledge 表对齐） */
    private static final Map<LogisticsNode, String> NODE_STATUS = new EnumMap<>(LogisticsNode.class);

    /** 目的地 → 路由脚本（每个节点对应的真实地理位置） */
    private static final Map<String, Map<LogisticsNode, String>> ROUTES = new HashMap<>();

    /** 节点 → 转移权重（正常为主、异常低概率） */
    private static final Map<LogisticsNode, Map<LogisticsNode, Integer>> TRANSITION_WEIGHTS = new EnumMap<>(LogisticsNode.class);

    /** 节点 → 驻留时长范围[minH,maxH]，用于真实时效推进 */
    private static final Map<LogisticsNode, int[]> DWELL_HOURS = new EnumMap<>(LogisticsNode.class);

    static {
        NODE_STATUS.put(LogisticsNode.CREATED, "EXP-0010");
        NODE_STATUS.put(LogisticsNode.WAREHOUSE_OUT, "EXP-0020");
        NODE_STATUS.put(LogisticsNode.DOMESTIC_PICKED, "EXP-0030");
        NODE_STATUS.put(LogisticsNode.EXPORT_CUSTOMS, "EXP-0040");
        NODE_STATUS.put(LogisticsNode.IN_TRANSIT, "EXP-0050");
        NODE_STATUS.put(LogisticsNode.IMPORT_CUSTOMS, "EXP-0060");
        NODE_STATUS.put(LogisticsNode.LAST_MILE, "EXP-0070");
        NODE_STATUS.put(LogisticsNode.DELIVERED, "EXP-0080");
        NODE_STATUS.put(LogisticsNode.CUSTOMS_DELAY, "CUS-1102");
        NODE_STATUS.put(LogisticsNode.DELIVERY_FAILED, "EXP-0071");
        NODE_STATUS.put(LogisticsNode.LOST, "EXP-0051");
        NODE_STATUS.put(LogisticsNode.RETURNED, "EXP-0062");

        // 国内干线路由（深圳发货 → 广州首转 → 各省分拨 → 末端派送）
        String[][] prov = {
                {"GD", "广州"}, {"ZJ", "杭州"}, {"JS", "南京"}, {"SH", "上海"}, {"BJ", "北京"},
                {"SC", "成都"}, {"HB", "武汉"}, {"HEN", "郑州"}, {"SD", "济南"}, {"FJ", "福州"},
                {"HN", "长沙"}, {"AH", "合肥"}, {"HEB", "石家庄"}, {"LN", "沈阳"}, {"SN", "西安"}, {"CQ", "重庆"}
        };
        for (String[] p : prov) {
            ROUTES.put(p[0], domesticRoute(p[1]));
        }
        // 兜底默认：华南主线（未知目的地走广东）
        ROUTES.put("default", ROUTES.get("GD"));

        // 转移权重（正常为主，异常低概率）
        weight(LogisticsNode.CREATED, LogisticsNode.WAREHOUSE_OUT, 10);
        weight(LogisticsNode.WAREHOUSE_OUT, LogisticsNode.DOMESTIC_PICKED, 10);
        weight(LogisticsNode.DOMESTIC_PICKED, LogisticsNode.EXPORT_CUSTOMS, 10);
        weight(LogisticsNode.EXPORT_CUSTOMS, LogisticsNode.IN_TRANSIT, 10);
        weight(LogisticsNode.IN_TRANSIT, LogisticsNode.IMPORT_CUSTOMS, 10, LogisticsNode.LOST, 1);
        weight(LogisticsNode.IMPORT_CUSTOMS, LogisticsNode.LAST_MILE, 10, LogisticsNode.CUSTOMS_DELAY, 2, LogisticsNode.RETURNED, 1);
        weight(LogisticsNode.LAST_MILE, LogisticsNode.DELIVERED, 10, LogisticsNode.DELIVERY_FAILED, 1);
        weight(LogisticsNode.CUSTOMS_DELAY, LogisticsNode.IMPORT_CUSTOMS, 10);
        weight(LogisticsNode.DELIVERY_FAILED, LogisticsNode.LAST_MILE, 10);

        // 驻留时长（小时），与真实物流链路对应（IN_TRANSIT/IMPORT_CUSTOMS 最久）
        dwell(LogisticsNode.CREATED, 0, 1);
        dwell(LogisticsNode.WAREHOUSE_OUT, 4, 12);
        dwell(LogisticsNode.DOMESTIC_PICKED, 6, 18);
        dwell(LogisticsNode.EXPORT_CUSTOMS, 8, 24);
        dwell(LogisticsNode.IN_TRANSIT, 24, 72);
        dwell(LogisticsNode.IMPORT_CUSTOMS, 24, 72);
        dwell(LogisticsNode.LAST_MILE, 6, 24);
        dwell(LogisticsNode.DELIVERED, 0, 0);
        dwell(LogisticsNode.CUSTOMS_DELAY, 48, 72);
        dwell(LogisticsNode.DELIVERY_FAILED, 24, 48);
        dwell(LogisticsNode.LOST, 0, 0);
        dwell(LogisticsNode.RETURNED, 0, 0);
    }

    @Override
    public LogisticsTrack nextTrack(LogisticsOrder order) {
        LogisticsNode current = LogisticsNode.getByCodeEn(order.getCurrentNode());
        if (current == null || current.isTerminal()) {
            return null;
        }
        List<LogisticsNode> targets = new ArrayList<>(LogisticsNodeTransition.nextNodes(current));
        if (targets.isEmpty()) {
            return null;
        }
        LogisticsNode next = weightedPick(current, targets);
        Map<LogisticsNode, String> route = resolveRoute(order.getDestinationCountry());
        String location = route.get(next);
        if (location == null) {
            location = route.get(current);
        }
        return LogisticsTrack.builder()
                .orderNo(order.getOrderNo())
                .node(next.getCodeEn())
                .rawStatus(NODE_STATUS.get(next))
                .rawDesc(carrierDesc(next, location))
                .location(location)
                .trackTime(System.currentTimeMillis())
                .build();
    }

    @Override
    public LogisticsTrack initialTrack(String orderNo) {
        return LogisticsTrack.builder()
                .orderNo(orderNo)
                .node(LogisticsNode.CREATED.getCodeEn())
                .rawStatus(NODE_STATUS.get(LogisticsNode.CREATED))
                .rawDesc(carrierDesc(LogisticsNode.CREATED, "深圳仓"))
                .location("深圳仓")
                .trackTime(System.currentTimeMillis())
                .build();
    }

    /**
     * 当前节点的驻留时长下限（毫秒），用于自动推进跳过仍在驻留窗口的订单
     */
    @Override
    public long dwellMillis(LogisticsNode node) {
        int[] range = DWELL_HOURS.get(node);
        if (range == null) {
            return 0L;
        }
        return range[0] * 3600_000L;
    }

    private Map<LogisticsNode, String> resolveRoute(String dest) {
        if (dest == null) {
            return ROUTES.get("default");
        }
        Map<LogisticsNode, String> route = ROUTES.get(dest.trim().toUpperCase());
        return route == null ? ROUTES.get("default") : route;
    }

    private LogisticsNode weightedPick(LogisticsNode current, List<LogisticsNode> targets) {
        Map<LogisticsNode, Integer> weights = TRANSITION_WEIGHTS.get(current);
        int total = 0;
        for (LogisticsNode t : targets) {
            Integer w = weights == null ? null : weights.get(t);
            total += (w == null ? 10 : w);
        }
        int r = ThreadLocalRandom.current().nextInt(total);
        for (LogisticsNode t : targets) {
            Integer w = weights == null ? null : weights.get(t);
            r -= (w == null ? 10 : w);
            if (r < 0) {
                return t;
            }
        }
        return targets.get(targets.size() - 1);
    }

    /** 国内干线路由：深圳发货 → 目的城市分拨 → 末端派送 */
    private static Map<LogisticsNode, String> domesticRoute(String city) {
        Map<LogisticsNode, String> r = new EnumMap<>(LogisticsNode.class);
        r.put(LogisticsNode.CREATED, "深圳仓");
        r.put(LogisticsNode.WAREHOUSE_OUT, "深圳仓");
        r.put(LogisticsNode.DOMESTIC_PICKED, "深圳·揽收");
        r.put(LogisticsNode.EXPORT_CUSTOMS, "广州分拨中心");
        r.put(LogisticsNode.IN_TRANSIT, "干线运输 ·深圳→" + city);
        r.put(LogisticsNode.IMPORT_CUSTOMS, city + "分拨中心");
        r.put(LogisticsNode.LAST_MILE, city + "·末端派送");
        r.put(LogisticsNode.DELIVERED, city);
        r.put(LogisticsNode.CUSTOMS_DELAY, city + "分拨中心");
        r.put(LogisticsNode.DELIVERY_FAILED, city + "·末端派送");
        r.put(LogisticsNode.LOST, "干线运输中");
        r.put(LogisticsNode.RETURNED, "广州分拨中心");
        return r;
    }

    private static String carrierDesc(LogisticsNode node, String location) {
        switch (node) {
            case CREATED:
                return "包裹已下单，等待揽收";
            case WAREHOUSE_OUT:
                return "包裹已从仓库出库";
            case DOMESTIC_PICKED:
                return "快递员已揽收";
            case EXPORT_CUSTOMS:
                return "包裹已到达中转分拨中心，等待发运";
            case IN_TRANSIT:
                return "包裹正在干线运输途中";
            case IMPORT_CUSTOMS:
                return "包裹已到达目的地分拨中心，正在分拣";
            case LAST_MILE:
                return "包裹已进入末端派送";
            case DELIVERED:
                return "包裹已签收";
            case CUSTOMS_DELAY:
                return "包裹在中转环节延误，正在处理";
            case DELIVERY_FAILED:
                return "派送失败，将重新派送";
            case LOST:
                return "包裹在运输途中丢失，正在核查";
            case RETURNED:
                return "包裹已退回发件仓";
            default:
                return node.getDescription() + " · " + (location == null ? "" : location);
        }
    }

    private static void weight(LogisticsNode from, LogisticsNode to, int w) {
        TRANSITION_WEIGHTS.computeIfAbsent(from, k -> new EnumMap<>(LogisticsNode.class)).put(to, w);
    }

    private static void weight(LogisticsNode from, LogisticsNode to1, int w1, LogisticsNode to2, int w2) {
        weight(from, to1, w1);
        weight(from, to2, w2);
    }

    private static void weight(LogisticsNode from, LogisticsNode to1, int w1, LogisticsNode to2, int w2, LogisticsNode to3, int w3) {
        weight(from, to1, w1);
        weight(from, to2, w2);
        weight(from, to3, w3);
    }

    private static void dwell(LogisticsNode node, int minH, int maxH) {
        DWELL_HOURS.put(node, new int[]{minH, maxH});
    }
}
