package com.aroura.sentinel.web.service.sentinel.tms;

import com.aroura.sentinel.logistics.dao.tms.CarrierRateDao;
import com.aroura.sentinel.logistics.model.tms.CarrierRate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 运费价卡服务
 *
 * @author sentinel
 */
@Service
public class CarrierRateService {

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd");

    private final CarrierRateDao rateDao;

    public CarrierRateService(CarrierRateDao rateDao) {
        this.rateDao = rateDao;
    }

    public Long save(Map<String, Object> body) {
        Long id = body.get("id") == null ? null : Long.valueOf(String.valueOf(body.get("id")));
        CarrierRate.CarrierRateBuilder b = CarrierRate.builder()
                .channelId(Long.valueOf(String.valueOf(body.get("channelId"))))
                .zone(String.valueOf(body.get("zone") == null ? "DEFAULT" : body.get("zone")))
                .minWeightKg(new BigDecimal(String.valueOf(body.get("minWeightKg") == null ? "0" : body.get("minWeightKg"))))
                .maxWeightKg(body.get("maxWeightKg") == null ? null : new BigDecimal(String.valueOf(body.get("maxWeightKg"))))
                .mode(String.valueOf(body.get("mode") == null ? "PER_KG" : body.get("mode")))
                .firstWeightKg(body.get("firstWeightKg") == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(body.get("firstWeightKg"))))
                .firstPrice(body.get("firstPrice") == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(body.get("firstPrice"))))
                .continuedWeightKg(body.get("continuedWeightKg") == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(body.get("continuedWeightKg"))))
                .continuedPrice(body.get("continuedPrice") == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(body.get("continuedPrice"))))
                .price(body.get("price") == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(body.get("price"))))
                .currency(String.valueOf(body.get("currency") == null ? "CNY" : body.get("currency")))
                .effectiveFrom(parseDate(body.get("effectiveFrom")))
                .effectiveTo(parseDate(body.get("effectiveTo")))
                .status(body.get("status") == null ? 1 : Integer.valueOf(String.valueOf(body.get("status"))));
        if (id != null) {
            b.id(id);
            rateDao.update(b.build());
            return id;
        }
        return rateDao.insert(b.build());
    }

    public void delete(Long id) {
        rateDao.delete(id);
    }

    public Map<String, Object> detail(Long id) {
        return rateDao.findById(id);
    }

    public Map<String, Object> list(Long channelId, String zone, int page, int perPage) {
        return rateDao.findPage(channelId, zone, page, perPage);
    }

    public List<Map<String, Object>> listByChannel(Long channelId) {
        return rateDao.listByChannel(channelId);
    }

    private Date parseDate(Object v) {
        if (v == null || String.valueOf(v).trim().isEmpty()) {
            return null;
        }
        try {
            return DATE_FMT.parse(String.valueOf(v));
        } catch (Exception e) {
            return null;
        }
    }
}
