package com.java3y.austin.web.service.sentinel.tms;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.java3y.austin.logistics.dao.LogisticsDao;
import com.java3y.austin.logistics.dao.tms.CarrierChannelDao;
import com.java3y.austin.logistics.dao.tms.CarrierDao;
import com.java3y.austin.logistics.dao.tms.WaybillDao;
import com.java3y.austin.logistics.engine.FreightCalculator;
import com.java3y.austin.logistics.model.LogisticsTrack;
import com.java3y.austin.logistics.model.tms.FreightQuote;
import com.java3y.austin.logistics.model.tms.Waybill;
import com.java3y.austin.web.exception.CommonException;
import com.java3y.austin.web.service.SentinelNotifyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 运单服务：出库生成运单（运费快照 + 承诺ETA + 轨迹 + 通知）
 *
 * @author sentinel
 */
@Service
public class WaybillService {

    private static final Logger log = LoggerFactory.getLogger(WaybillService.class);
    private static final String CNY = "CNY";

    private final WaybillDao waybillDao;
    private final LogisticsDao logisticsDao;
    private final CarrierChannelDao channelDao;
    private final CarrierDao carrierDao;
    private final FreightCalculator freightCalculator;
    private final SentinelNotifyService notifyService;
    private final SlaService slaService;
    private final AuditLogService auditLogService;

    public WaybillService(WaybillDao waybillDao, LogisticsDao logisticsDao, CarrierChannelDao channelDao,
                          CarrierDao carrierDao, FreightCalculator freightCalculator,
                          SentinelNotifyService notifyService, SlaService slaService,
                          AuditLogService auditLogService) {
        this.waybillDao = waybillDao;
        this.logisticsDao = logisticsDao;
        this.channelDao = channelDao;
        this.carrierDao = carrierDao;
        this.freightCalculator = freightCalculator;
        this.notifyService = notifyService;
        this.slaService = slaService;
        this.auditLogService = auditLogService;
    }

    /**
     * 出库生成运单。
     * 整单出库：幂等（已有运单直接返回）；分批出库（partial=true）：允许同一订单多次出库，
     * 每次可指定本次出库商品子集 items（不传则出全量），运单记录本次出库商品明细，订单商品保持全量。
     */
    public Map<String, Object> generate(String orderNo, String items, boolean partial) {
        Map<String, Object> order = logisticsDao.findOrderByNo(orderNo);
        if (order == null) {
            throw new CommonException("订单不存在: " + orderNo);
        }
        if (!partial && order.get("waybill_no") != null) {
            throw new CommonException("订单已生成运单: " + order.get("waybill_no"));
        }
        Object reviewStatus = order.get("review_status");
        if (reviewStatus != null && !"APPROVED".equals(String.valueOf(reviewStatus))) {
            throw new CommonException("订单未审核，请先在「订单审核」通过后再出库");
        }
        Object channelIdObj = order.get("channel_id");
        if (channelIdObj == null) {
            throw new CommonException("订单未选择物流渠道");
        }
        Long channelId = Long.valueOf(String.valueOf(channelIdObj));
        Map<String, Object> channel = channelDao.findById(channelId);
        if (channel == null) {
            throw new CommonException("渠道不存在: " + channelId);
        }

        // 分批出库：校验本次出库商品为订单商品子集，运单记录子集；整单出库：运单记录订单全量
        String outItemsJson = String.valueOf(order.get("items_json"));
        if (partial && items != null && !items.trim().isEmpty()) {
            validateSubset(order.get("items_json"), items);
            outItemsJson = items;
        }
        Aggregate agg = aggregate(outItemsJson);
        FreightQuote quote = freightCalculator.quote(channelId, agg.weightKg, agg.volumeL,
                String.valueOf(order.get("destination_country")));

        String trackingNo = String.valueOf(channel.get("tracking_prefix") == null ? "T" : channel.get("tracking_prefix"))
                + System.currentTimeMillis();
        String waybillNo = "WB" + channel.get("channel_code") + System.currentTimeMillis();
        Date promiseEta = slaService.computeEta(toInt(channel.get("transit_days_max"), 10), new Date());

        Map<String, Object> carrier = carrierDao.findById(toLong(channel.get("carrier_id")));

        Waybill wb = Waybill.builder()
                .waybillNo(waybillNo)
                .orderNo(orderNo)
                .merchantId(toLong(order.get("merchant_id")))
                .channelId(channelId)
                .carrierId(toLong(channel.get("carrier_id")))
                .trackingNo(trackingNo)
                .carrierCode(carrier == null ? null : String.valueOf(carrier.get("carrier_code")))
                .weightKg(agg.weightKg)
                .volumeL(agg.volumeL)
                .billableWeightKg(quote.getBillableWeight())
                .declaredValue(agg.declaredValue)
                .declaredCurrency(CNY)
                .freightCost(quote.getFreight())
                .freightCurrency(quote.getCurrency())
                .zone(quote.getZone())
                .promiseEta(promiseEta)
                .status("ACTIVE")
                .billed(0)
                .itemsJson(outItemsJson)
                .build();
        waybillDao.insert(wb);

        if (partial && order.get("waybill_no") != null) {
            // 分批出库后续批次：保留首个运单号，仅确保订单处于出库中节点
            logisticsDao.updateOrderCurrentNode(orderNo, "WAREHOUSE_OUT");
            logisticsDao.updateSlaStatus(orderNo, "NORMAL");
        } else {
            logisticsDao.updateOrderWaybill(orderNo, waybillNo, promiseEta);
        }
        auditLogService.log("waybill", "GENERATE", waybillNo,
                (partial ? "分批出库" : "整单出库") + "生成运单 tracking=" + trackingNo + " 计费重="
                        + quote.getBillableWeight() + "kg 运费=" + quote.getFreight());

        LogisticsTrack track = LogisticsTrack.builder()
                .orderNo(orderNo)
                .node("WAREHOUSE_OUT")
                .rawStatus("EXP-0020")
                .rawDesc("Package departed warehouse")
                .location("发货仓出库")
                .carrierCode(wb.getCarrierCode())
                .trackTime(System.currentTimeMillis())
                .build();
        logisticsDao.saveTrack(track);

        try {
            notifyService.send(orderNo, "WAREHOUSE_OUT", "buyer", "push");
        } catch (Exception e) {
            log.warn("[Waybill] 出库通知触发失败 orderNo={}", orderNo, e);
        }

        Map<String, Object> result = new HashMap<>(4);
        result.put("waybill", waybillDao.findByWaybillNo(waybillNo));
        result.put("quote", quote);
        result.put("partial", partial);
        return result;
    }

    /**
     * 合并运单：同商家、同目的地、未出库、已审核订单合并生成一张运单。
     * 运费 = 各订单运费快照合计；商品明细 = 各单合并；历史记录写入审计（MERGE），无需新表。
     */
    public Map<String, Object> merge(List<String> orderNos) {
        if (orderNos == null || orderNos.size() < 2) {
            throw new CommonException("合并运单至少需要 2 个订单");
        }
        List<Map<String, Object>> orders = new ArrayList<>();
        for (String no : orderNos) {
            Map<String, Object> o = logisticsDao.findOrderByNo(no);
            if (o == null) {
                throw new CommonException("订单不存在: " + no);
            }
            orders.add(o);
        }
        Map<String, Object> first = orders.get(0);
        Long merchantId = toLong(first.get("merchant_id"));
        String dest = String.valueOf(first.get("destination_country"));
        Long channelId = toLong(first.get("channel_id"));
        for (Map<String, Object> o : orders) {
            Object rs = o.get("review_status");
            if (rs != null && !"APPROVED".equals(String.valueOf(rs))) {
                throw new CommonException("订单 " + o.get("order_no") + " 未审核通过，不能合并");
            }
            if (o.get("waybill_no") != null || !"CREATED".equals(String.valueOf(o.get("current_node")))) {
                throw new CommonException("订单 " + o.get("order_no") + " 已出库，不能合并");
            }
            if (!merchantId.equals(toLong(o.get("merchant_id")))) {
                throw new CommonException("合并要求所有订单属于同一商家");
            }
            if (!dest.equals(String.valueOf(o.get("destination_country")))) {
                throw new CommonException("合并要求所有订单发往同一目的地");
            }
        }
        if (channelId == null) {
            throw new CommonException("订单未选择物流渠道");
        }
        Map<String, Object> channel = channelDao.findById(channelId);
        if (channel == null) {
            throw new CommonException("渠道不存在: " + channelId);
        }

        // 商品明细合并 + 运费合计（订单侧运费快照累加，结算归属结算模块）
        JSONArray allItems = new JSONArray();
        BigDecimal totalFreight = BigDecimal.ZERO;
        for (Map<String, Object> o : orders) {
            Object f = o.get("freight_cost");
            if (f != null) {
                totalFreight = totalFreight.add(new BigDecimal(String.valueOf(f)));
            }
            JSONArray arr = JSON.parseArray(String.valueOf(o.get("items_json")));
            if (arr != null) {
                for (int i = 0; i < arr.size(); i++) {
                    allItems.add(arr.getJSONObject(i));
                }
            }
        }
        String itemsJson = allItems.toJSONString();
        Aggregate agg = aggregate(itemsJson);

        String trackingNo = String.valueOf(channel.get("tracking_prefix") == null ? "T" : channel.get("tracking_prefix"))
                + System.currentTimeMillis();
        String waybillNo = "WB" + channel.get("channel_code") + System.currentTimeMillis();
        Date promiseEta = slaService.computeEta(toInt(channel.get("transit_days_max"), 10), new Date());
        Map<String, Object> carrier = carrierDao.findById(toLong(channel.get("carrier_id")));

        Waybill wb = Waybill.builder()
                .waybillNo(waybillNo)
                .orderNo(String.valueOf(first.get("order_no")))
                .merchantId(merchantId)
                .channelId(channelId)
                .carrierId(toLong(channel.get("carrier_id")))
                .trackingNo(trackingNo)
                .carrierCode(carrier == null ? null : String.valueOf(carrier.get("carrier_code")))
                .weightKg(agg.weightKg)
                .volumeL(agg.volumeL)
                .billableWeightKg(agg.weightKg)
                .declaredValue(agg.declaredValue)
                .declaredCurrency(CNY)
                .freightCost(totalFreight)
                .freightCurrency(CNY)
                .zone(channel.get("zone") == null ? null : String.valueOf(channel.get("zone")))
                .promiseEta(promiseEta)
                .status("ACTIVE")
                .billed(0)
                .itemsJson(itemsJson)
                .build();
        waybillDao.insert(wb);

        for (Map<String, Object> o : orders) {
            String no = String.valueOf(o.get("order_no"));
            logisticsDao.updateOrderWaybill(no, waybillNo, promiseEta);
            logisticsDao.saveTrack(LogisticsTrack.builder()
                    .orderNo(no)
                    .node("WAREHOUSE_OUT")
                    .rawStatus("EXP-0020")
                    .rawDesc("合并生成运单 " + waybillNo)
                    .location("发货仓出库")
                    .carrierCode(wb.getCarrierCode())
                    .trackTime(System.currentTimeMillis())
                    .build());
            try {
                notifyService.send(no, "WAREHOUSE_OUT", "buyer", "push");
            } catch (Exception e) {
                log.warn("[Waybill] 合并出库通知触发失败 orderNo={}", no, e);
            }
        }
        auditLogService.log("waybill", "MERGE", String.valueOf(first.get("order_no")),
                "合并生成运单 " + waybillNo + "，包含订单: " + String.join(",", orderNos) + "，运费合计=" + totalFreight);

        Map<String, Object> result = new HashMap<>(4);
        result.put("waybill", waybillDao.findByWaybillNo(waybillNo));
        result.put("orderNos", orderNos);
        result.put("freightTotal", totalFreight);
        return result;
    }

    /**
     * 校验分批出库商品子集：每个 sku 出库数量不得超过订单数量
     */
    private void validateSubset(Object orderItemsJson, String outItemsJson) {
        if (orderItemsJson == null) {
            throw new CommonException("订单无商品明细");
        }
        JSONArray orderArr = JSON.parseArray(String.valueOf(orderItemsJson));
        JSONArray outArr = JSON.parseArray(outItemsJson);
        for (int i = 0; i < outArr.size(); i++) {
            JSONObject out = outArr.getJSONObject(i);
            String sku = out.getString("sku");
            int qty = out.getIntValue("qty", 1);
            JSONObject match = null;
            for (int j = 0; j < orderArr.size(); j++) {
                JSONObject row = orderArr.getJSONObject(j);
                if (sku != null && sku.equals(row.getString("sku"))) {
                    match = row;
                    break;
                }
            }
            if (match == null) {
                throw new CommonException("出库商品不在订单明细中: " + sku);
            }
            if (qty > match.getIntValue("qty", 0)) {
                throw new CommonException("出库数量超过订单数量: " + sku
                        + "（订单 " + match.getIntValue("qty", 0) + "，本次 " + qty + "）");
            }
        }
    }

    /**
     * 订单推进到 DELIVERED 时回写运单妥投（分批出库场景：该订单下所有运单一并妥投）
     */
    public void onDelivered(String orderNo) {
        waybillDao.markDeliveredByOrderNo(orderNo, new Date());
    }

    public Map<String, Object> list(String orderNo, String waybillNo, String trackingNo, Long channelId, Long carrierId, int page, int perPage) {
        return waybillDao.findPage(orderNo, waybillNo, trackingNo, channelId, carrierId, page, perPage);
    }

    public Map<String, Object> detail(String waybillNo) {
        Map<String, Object> wb = waybillDao.findByWaybillNo(waybillNo);
        if (wb == null) {
            throw new CommonException("运单不存在: " + waybillNo);
        }
        return wb;
    }

    /* ---------- 聚合工具 ---------- */

    private Aggregate aggregate(Object itemsJson) {
        Aggregate a = new Aggregate();
        if (itemsJson == null) {
            return a;
        }
        JSONArray arr = JSON.parseArray(String.valueOf(itemsJson));
        BigDecimal weight = BigDecimal.ZERO;
        BigDecimal volume = BigDecimal.ZERO;
        BigDecimal declared = BigDecimal.ZERO;
        for (int i = 0; i < arr.size(); i++) {
            JSONObject row = arr.getJSONObject(i);
            int qty = row.getIntValue("qty", 1);
            BigDecimal unitW = row.getBigDecimal("unit_weight_kg") == null ? BigDecimal.ZERO : row.getBigDecimal("unit_weight_kg");
            BigDecimal unitV = row.getBigDecimal("unit_volume_l") == null ? BigDecimal.ZERO : row.getBigDecimal("unit_volume_l");
            BigDecimal unitD = row.getBigDecimal("unit_declared_value") == null ? BigDecimal.ZERO : row.getBigDecimal("unit_declared_value");
            weight = weight.add(unitW.multiply(BigDecimal.valueOf(qty)));
            volume = volume.add(unitV.multiply(BigDecimal.valueOf(qty)));
            declared = declared.add(unitD.multiply(BigDecimal.valueOf(qty)));
        }
        a.weightKg = weight.setScale(3, RoundingMode.HALF_UP);
        a.volumeL = volume.setScale(3, RoundingMode.HALF_UP);
        a.declaredValue = declared.setScale(2, RoundingMode.HALF_UP);
        return a;
    }

    private static int toInt(Object v, int def) {
        return v == null ? def : Integer.parseInt(String.valueOf(v));
    }

    private static Long toLong(Object v) {
        return v == null ? null : Long.valueOf(String.valueOf(v));
    }

    private static final class Aggregate {
        private BigDecimal weightKg;
        private BigDecimal volumeL;
        private BigDecimal declaredValue;
    }
}
