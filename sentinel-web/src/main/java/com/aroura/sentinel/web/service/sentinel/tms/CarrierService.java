package com.aroura.sentinel.web.service.sentinel.tms;

import com.aroura.sentinel.logistics.dao.tms.CarrierDao;
import com.aroura.sentinel.logistics.model.tms.Carrier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 承运商主数据服务
 *
 * @author sentinel
 */
@Service
public class CarrierService {

    private final CarrierDao carrierDao;

    public CarrierService(CarrierDao carrierDao) {
        this.carrierDao = carrierDao;
    }

    public Long save(Map<String, Object> body) {
        Long id = body.get("id") == null ? null : Long.valueOf(String.valueOf(body.get("id")));
        Carrier.CarrierBuilder b = Carrier.builder()
                .carrierCode(String.valueOf(body.get("carrierCode") == null ? "CARR-" + System.currentTimeMillis() : body.get("carrierCode")))
                .carrierName(String.valueOf(body.get("carrierName")))
                .type(String.valueOf(body.get("type") == null ? "air" : body.get("type")))
                .country(body.get("country") == null ? "CN" : String.valueOf(body.get("country")))
                .apiEndpoint(body.get("apiEndpoint") == null ? null : String.valueOf(body.get("apiEndpoint")))
                .apiKey(body.get("apiKey") == null ? null : String.valueOf(body.get("apiKey")))
                .status(body.get("status") == null ? 1 : Integer.valueOf(String.valueOf(body.get("status"))));
        if (id != null) {
            b.id(id);
            carrierDao.update(b.build());
            return id;
        }
        return carrierDao.insert(b.build());
    }

    public void delete(Long id) {
        carrierDao.delete(id);
    }

    public Map<String, Object> detail(Long id) {
        return carrierDao.findById(id);
    }

    public Map<String, Object> list(String keyword, int page, int perPage) {
        return carrierDao.findPage(keyword, page, perPage);
    }

    public List<Map<String, Object>> listAll() {
        return carrierDao.listAll();
    }
}
