package com.aroura.sentinel.web.service.sentinel.tms;

import com.aroura.sentinel.logistics.dao.tms.WarehouseDao;
import com.aroura.sentinel.logistics.model.tms.Warehouse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 仓库主数据服务
 *
 * @author sentinel
 */
@Service
public class WarehouseService {

    private final WarehouseDao warehouseDao;

    public WarehouseService(WarehouseDao warehouseDao) {
        this.warehouseDao = warehouseDao;
    }

    public Long save(Map<String, Object> body) {
        Long id = body.get("id") == null ? null : Long.valueOf(String.valueOf(body.get("id")));
        Warehouse.WarehouseBuilder b = Warehouse.builder()
                .warehouseCode(String.valueOf(body.get("warehouseCode") == null ? "WH-" + System.currentTimeMillis() : body.get("warehouseCode")))
                .warehouseName(String.valueOf(body.get("warehouseName")))
                .country(body.get("country") == null ? "CN" : String.valueOf(body.get("country")))
                .city(body.get("city") == null ? null : String.valueOf(body.get("city")))
                .address(body.get("address") == null ? null : String.valueOf(body.get("address")))
                .status(body.get("status") == null ? 1 : Integer.valueOf(String.valueOf(body.get("status"))));
        if (id != null) {
            b.id(id);
            warehouseDao.update(b.build());
            return id;
        }
        return warehouseDao.insert(b.build());
    }

    public void delete(Long id) {
        warehouseDao.delete(id);
    }

    public Map<String, Object> detail(Long id) {
        return warehouseDao.findById(id);
    }

    public Map<String, Object> list(String keyword, int page, int perPage) {
        return warehouseDao.findPage(keyword, page, perPage);
    }

    public List<Map<String, Object>> listAll() {
        return warehouseDao.listAll();
    }
}
