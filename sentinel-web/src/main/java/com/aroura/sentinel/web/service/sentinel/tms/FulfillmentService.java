package com.aroura.sentinel.web.service.sentinel.tms;

import org.springframework.transaction.annotation.Transactional;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.aroura.sentinel.logistics.dao.LogisticsDao;
import com.aroura.sentinel.logistics.dao.tms.CarrierChannelDao;
import com.aroura.sentinel.logistics.dao.tms.MerchantDao;
import com.aroura.sentinel.logistics.dao.tms.ProductDao;
import com.aroura.sentinel.logistics.engine.FreightCalculator;
import com.aroura.sentinel.logistics.enums.LogisticsNode;
import com.aroura.sentinel.logistics.model.LogisticsOrder;
import com.aroura.sentinel.logistics.model.tms.FreightQuote;
import com.aroura.sentinel.web.exception.CommonException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * 履约服务：运费试算 + 建单
 *
 * @author sentinel
 */
@Service
public class FulfillmentService {

    private static final String CNY = "CNY";

    private final CarrierChannelDao channelDao;
    private final ProductDao productDao;
    private final MerchantDao merchantDao;
    private final LogisticsDao logisticsDao;
    private final FreightCalculator freightCalculator;
    private final AuditLogService auditLogService;

    public FulfillmentService(CarrierChannelDao channelDao, ProductDao productDao, MerchantDao merchantDao,
                              LogisticsDao logisticsDao, FreightCalculator freightCalculator,
                              AuditLogService auditLogService) {
        this.channelDao = channelDao;
        this.productDao = productDao;
        this.merchantDao = merchantDao;
        this.logisticsDao = logisticsDao;
        this.freightCalculator = freightCalculator;
        this.auditLogService = auditLogService;
    }

    /**
     * 运费试算（不落库）
     */
    public Map<String, Object> quote(Map<String, Object> body) {
        Long merchantId = Long.valueOf(String.valueOf(body.get("merchantId")));
        Long channelId = Long.valueOf(String.valueOf(body.get("channelId")));
        String destCountry = String.valueOf(body.get("destinationCountry"));
        Aggregate agg = aggregate(merchantId, parseItems(body.get("items")));
        FreightQuote quote = freightCalculator.quote(channelId, agg.weightKg, agg.volumeL, destCountry);
        return quoteResult(agg, quote);
    }

    /**
     * 渠道比价：对目的地全部可用渠道批量报价，按运费升序
     * <p>让卖家一眼对比"时效 vs 价格"，替代逐个试算。
     */
    public List<Map<String, Object>> compareChannels(Map<String, Object> body) {
        Long merchantId = Long.valueOf(String.valueOf(body.get("merchantId")));
        String destCountry = String.valueOf(body.get("destinationCountry"));
        Aggregate agg = aggregate(merchantId, parseItems(body.get("items")));
        List<Map<String, Object>> channels = channelDao.listByDestCountry(destCountry);
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (Map<String, Object> ch : channels) {
            if (!"1".equals(String.valueOf(ch.get("status")))) {
                continue;
            }
            Long cid = Long.valueOf(String.valueOf(ch.get("id")));
            Map<String, Object> row = new java.util.HashMap<>(8);
            row.put("channelId", cid);
            row.put("channelCode", ch.get("channel_code"));
            row.put("channelName", ch.get("channel_name"));
            row.put("type", ch.get("type"));
            row.put("transitDaysMin", ch.get("transit_days_min"));
            row.put("transitDaysMax", ch.get("transit_days_max"));
            row.put("trackingPrefix", ch.get("tracking_prefix"));
            try {
                FreightQuote q = freightCalculator.quote(cid, agg.weightKg, agg.volumeL, destCountry);
                row.put("freight", q.getFreight());
                row.put("billableWeight", q.getBillableWeight());
                row.put("mode", q.getMode());
                row.put("currency", q.getCurrency());
            } catch (Exception e) {
                row.put("freight", null); // 无价卡渠道，前端显示"未报价"
                row.put("billableWeight", null);
                row.put("mode", null);
                row.put("currency", null);
            }
            result.add(row);
        }
        result.sort((a, b) -> {
            java.math.BigDecimal fa = (java.math.BigDecimal) a.get("freight");
            java.math.BigDecimal fb = (java.math.BigDecimal) b.get("freight");
            if (fa == null) return 1;
            if (fb == null) return -1;
            return fa.compareTo(fb);
        });
        return result;
    }

    /**
     * 建单（校验渠道覆盖目的地 + 运费快照）
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createOrder(Map<String, Object> body) {
        Long merchantId = Long.valueOf(String.valueOf(body.get("merchantId")));
        Long channelId = Long.valueOf(String.valueOf(body.get("channelId")));
        String destCountry = String.valueOf(body.get("destinationCountry"));

        Map<String, Object> channel = channelDao.findById(channelId);
        if (channel == null) {
            throw new CommonException("渠道不存在");
        }
        if (!"1".equals(String.valueOf(channel.get("status")))) {
            throw new CommonException("渠道已停用");
        }
        if (!destCountry.equals(String.valueOf(channel.get("dest_country")))) {
            throw new CommonException("渠道不覆盖目的地: " + destCountry);
        }

        Aggregate agg = aggregate(merchantId, parseItems(body.get("items")));
        FreightQuote quote = freightCalculator.quote(channelId, agg.weightKg, agg.volumeL, destCountry);

        Map<String, Object> merchant = merchantDao.findById(merchantId);
        String merchantName = merchant == null ? "" : String.valueOf(merchant.get("merchant_name"));

        LogisticsOrder order = LogisticsOrder.builder()
                .orderNo("OMT-TMS-" + System.currentTimeMillis())
                .buyerId(String.valueOf(body.get("buyerId")))
                .buyerName(body.get("buyerName") == null ? null : String.valueOf(body.get("buyerName")))
                .buyerPhone(body.get("buyerPhone") == null ? null : String.valueOf(body.get("buyerPhone")))
                .buyerLanguage(String.valueOf(body.get("buyerLanguage") == null ? "zh" : body.get("buyerLanguage")))
                .merchantId(merchantId)
                .merchantName(merchantName)
                .destinationCountry(destCountry)
                .currentNode(LogisticsNode.CREATED.getCodeEn())
                .channelId(channelId)
                .carrierId(toLong(channel.get("carrier_id")))
                .warehouseId(body.get("warehouseId") == null ? null : Long.valueOf(String.valueOf(body.get("warehouseId"))))
                .itemsJson(agg.itemsJson)
                .declaredValue(agg.declaredValue)
                .declaredCurrency(CNY)
                .freightCost(quote.getFreight())
                .freightCurrency(quote.getCurrency())
                .slaStatus("NA")
                .buyerAddress(body.get("buyerAddress") == null ? null : String.valueOf(body.get("buyerAddress")))
                .buyerCity(body.get("buyerCity") == null ? null : String.valueOf(body.get("buyerCity")))
                .buyerPostal(body.get("buyerPostal") == null ? null : String.valueOf(body.get("buyerPostal")))
                .businessNotes(body.get("businessNotes") == null ? null : String.valueOf(body.get("businessNotes")))
                .build();
        logisticsDao.saveTmsOrder(order);
        // 新单默认待审核；审核通过后方可出库
        logisticsDao.updateReviewStatus(order.getOrderNo(), "PENDING");
        auditLogService.log("order", "CREATE", order.getOrderNo(),
                "创建履约订单(待审核) 渠道ID=" + channelId + " 运费=" + quote.getFreight() + quote.getCurrency());

        Map<String, Object> result = new java.util.HashMap<>(4);
        result.put("order", logisticsDao.findOrderByNo(order.getOrderNo()));
        result.put("quote", quote);
        return result;
    }

    /* ---------- 聚合工具 ---------- */

    private Aggregate aggregate(Long merchantId, List<JSONObject> items) {
        if (items == null || items.isEmpty()) {
            throw new CommonException("商品明细不能为空");
        }
        BigDecimal weight = BigDecimal.ZERO;
        BigDecimal volume = BigDecimal.ZERO;
        BigDecimal declared = BigDecimal.ZERO;
        JSONArray arr = new JSONArray();
        for (JSONObject item : items) {
            String sku = item.getString("sku");
            Integer qty = item.getInteger("qty");
            if (sku == null || qty == null || qty <= 0) {
                throw new CommonException("商品明细非法: sku/qty 必填且 qty>0");
            }
            Map<String, Object> product = productDao.findByMerchantSku(merchantId, sku);
            if (product == null) {
                throw new CommonException("商品不存在或不属于该商家: " + sku);
            }
            BigDecimal unitWeight = new BigDecimal(String.valueOf(product.get("weight_kg")));
            BigDecimal unitVolume = new BigDecimal(String.valueOf(product.get("volume_l")));
            BigDecimal unitDeclared = new BigDecimal(String.valueOf(product.get("declared_value")));
            weight = weight.add(unitWeight.multiply(BigDecimal.valueOf(qty)));
            volume = volume.add(unitVolume.multiply(BigDecimal.valueOf(qty)));
            declared = declared.add(unitDeclared.multiply(BigDecimal.valueOf(qty)));
            JSONObject row = new JSONObject();
            row.put("sku", sku);
            row.put("product_id", product.get("id"));
            row.put("qty", qty);
            row.put("unit_weight_kg", unitWeight);
            row.put("unit_volume_l", unitVolume);
            row.put("unit_declared_value", unitDeclared);
            row.put("currency", product.get("currency") == null ? CNY : String.valueOf(product.get("currency")));
            arr.add(row);
        }
        Aggregate a = new Aggregate();
        a.weightKg = weight.setScale(3, RoundingMode.HALF_UP);
        a.volumeL = volume.setScale(3, RoundingMode.HALF_UP);
        a.declaredValue = declared.setScale(2, RoundingMode.HALF_UP);
        a.itemsJson = arr.toJSONString();
        return a;
    }

    private List<JSONObject> parseItems(Object items) {
        if (items == null) {
            return null;
        }
        if (items instanceof List) {
            return JSON.parseArray(JSON.toJSONString(items), JSONObject.class);
        }
        return JSON.parseArray(String.valueOf(items), JSONObject.class);
    }

    private Map<String, Object> quoteResult(Aggregate agg, FreightQuote quote) {
        Map<String, Object> result = new java.util.HashMap<>(8);
        result.put("weightKg", agg.weightKg);
        result.put("volumeL", agg.volumeL);
        result.put("declaredValue", agg.declaredValue);
        result.put("quote", quote);
        return result;
    }

    private static Long toLong(Object v) {
        return v == null ? null : Long.valueOf(String.valueOf(v));
    }

    private static final class Aggregate {
        private BigDecimal weightKg;
        private BigDecimal volumeL;
        private BigDecimal declaredValue;
        private String itemsJson;
    }
}
