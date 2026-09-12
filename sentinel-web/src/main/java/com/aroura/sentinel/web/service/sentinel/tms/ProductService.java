package com.aroura.sentinel.web.service.sentinel.tms;

import com.aroura.sentinel.logistics.dao.tms.ProductDao;
import com.aroura.sentinel.logistics.model.tms.Product;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 商品 SKU 服务（MERCHANT 角色仅能操作自己商品）
 *
 * @author sentinel
 */
@Service
public class ProductService {

    private final ProductDao productDao;

    public ProductService(ProductDao productDao) {
        this.productDao = productDao;
    }

    public Long save(Map<String, Object> body, Long forceMerchantId) {
        Long merchantId = forceMerchantId != null ? forceMerchantId
                : Long.valueOf(String.valueOf(body.get("merchantId")));
        Long id = body.get("id") == null ? null : Long.valueOf(String.valueOf(body.get("id")));
        Product.ProductBuilder b = Product.builder()
                .merchantId(merchantId)
                .sku(String.valueOf(body.get("sku")))
                .name(String.valueOf(body.get("name")))
                .hsCode(body.get("hsCode") == null ? null : String.valueOf(body.get("hsCode")))
                .declaredValue(body.get("declaredValue") == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(body.get("declaredValue"))))
                .currency(body.get("currency") == null ? "CNY" : String.valueOf(body.get("currency")))
                .weightKg(new BigDecimal(String.valueOf(body.get("weightKg") == null ? "0" : body.get("weightKg"))))
                .volumeL(body.get("volumeL") == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(body.get("volumeL"))))
                .originCountry(body.get("originCountry") == null ? "CN" : String.valueOf(body.get("originCountry")))
                .status(body.get("status") == null ? 1 : Integer.valueOf(String.valueOf(body.get("status"))));
        if (id != null) {
            b.id(id);
            productDao.update(b.build());
            return id;
        }
        return productDao.insert(b.build());
    }

    public void delete(Long id) {
        productDao.delete(id);
    }

    public Map<String, Object> detail(Long id) {
        return productDao.findById(id);
    }

    /**
     * 列表：MERCHANT 强制限定自己商家
     */
    public Map<String, Object> list(Long merchantId, Long forceMerchantId, String keyword, int page, int perPage) {
        Long scope = forceMerchantId != null ? forceMerchantId : merchantId;
        return productDao.findPage(scope, keyword, page, perPage);
    }

    public List<Map<String, Object>> listByMerchant(Long merchantId) {
        return productDao.listByMerchant(merchantId);
    }
}
