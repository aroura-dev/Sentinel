package com.java3y.austin.web.service.sentinel.tms;

import com.java3y.austin.logistics.dao.tms.CarrierChannelDao;
import com.java3y.austin.logistics.model.tms.CarrierChannel;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 物流渠道服务
 *
 * @author sentinel
 */
@Service
public class CarrierChannelService {

    private final CarrierChannelDao channelDao;

    public CarrierChannelService(CarrierChannelDao channelDao) {
        this.channelDao = channelDao;
    }

    public Long save(Map<String, Object> body) {
        Long id = body.get("id") == null ? null : Long.valueOf(String.valueOf(body.get("id")));
        CarrierChannel.CarrierChannelBuilder b = CarrierChannel.builder()
                .carrierId(Long.valueOf(String.valueOf(body.get("carrierId"))))
                .channelCode(String.valueOf(body.get("channelCode") == null ? "CH-" + System.currentTimeMillis() : body.get("channelCode")))
                .channelName(String.valueOf(body.get("channelName")))
                .type(String.valueOf(body.get("type") == null ? "air" : body.get("type")))
                .destCountry(String.valueOf(body.get("destCountry")))
                .transitDaysMin(body.get("transitDaysMin") == null ? 1 : Integer.valueOf(String.valueOf(body.get("transitDaysMin"))))
                .transitDaysMax(body.get("transitDaysMax") == null ? 10 : Integer.valueOf(String.valueOf(body.get("transitDaysMax"))))
                .trackingPrefix(body.get("trackingPrefix") == null ? null : String.valueOf(body.get("trackingPrefix")))
                .minBillableWeightKg(body.get("minBillableWeightKg") == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(body.get("minBillableWeightKg"))))
                .volDivisor(body.get("volDivisor") == null ? 5000 : Integer.valueOf(String.valueOf(body.get("volDivisor"))))
                .status(body.get("status") == null ? 1 : Integer.valueOf(String.valueOf(body.get("status"))))
                .remark(body.get("remark") == null ? null : String.valueOf(body.get("remark")));
        if (id != null) {
            b.id(id);
            channelDao.update(b.build());
            return id;
        }
        return channelDao.insert(b.build());
    }

    public void delete(Long id) {
        channelDao.delete(id);
    }

    public Map<String, Object> detail(Long id) {
        return channelDao.findById(id);
    }

    public Map<String, Object> list(Long carrierId, String destCountry, String type, int page, int perPage) {
        return channelDao.findPage(carrierId, destCountry, type, page, perPage);
    }

    public List<Map<String, Object>> listByDestCountry(String destCountry) {
        return channelDao.listByDestCountry(destCountry);
    }

    public List<Map<String, Object>> listAll() {
        return channelDao.listAll();
    }
}
