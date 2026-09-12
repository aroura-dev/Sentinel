package com.java3y.austin.web.service.sentinel.tms;

import com.java3y.austin.logistics.dao.tms.MerchantDao;
import com.java3y.austin.logistics.model.tms.Merchant;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 商家主数据服务
 *
 * @author sentinel
 */
@Service
public class MerchantService {

    private final MerchantDao merchantDao;

    public MerchantService(MerchantDao merchantDao) {
        this.merchantDao = merchantDao;
    }

    public Long save(Map<String, Object> body) {
        Long id = body.get("id") == null ? null : Long.valueOf(String.valueOf(body.get("id")));
        Merchant.MerchantBuilder b = Merchant.builder()
                .merchantCode(String.valueOf(body.get("merchantCode") == null ? "MCH-" + System.currentTimeMillis() : body.get("merchantCode")))
                .merchantName(String.valueOf(body.get("merchantName")))
                .userId(body.get("userId") == null ? null : Long.valueOf(String.valueOf(body.get("userId"))))
                .contactName(body.get("contactName") == null ? null : String.valueOf(body.get("contactName")))
                .contactPhone(body.get("contactPhone") == null ? null : String.valueOf(body.get("contactPhone")))
                .contactEmail(body.get("contactEmail") == null ? null : String.valueOf(body.get("contactEmail")))
                .country(body.get("country") == null ? "CN" : String.valueOf(body.get("country")))
                .status(body.get("status") == null ? 1 : Integer.valueOf(String.valueOf(body.get("status"))));
        if (id != null) {
            b.id(id);
            merchantDao.update(b.build());
            return id;
        }
        return merchantDao.insert(b.build());
    }

    public void delete(Long id) {
        merchantDao.delete(id);
    }

    public Map<String, Object> detail(Long id) {
        return merchantDao.findById(id);
    }

    public Map<String, Object> list(String keyword, int page, int perPage) {
        return merchantDao.findPage(keyword, page, perPage);
    }

    public List<Map<String, Object>> listAll() {
        return merchantDao.listAll();
    }

    /**
     * 通过登录用户名解析当前商家（MERCHANT 角色权限隔离用）
     */
    public Map<String, Object> findByUsername(String username) {
        if (username == null) {
            return null;
        }
        List<Map<String, Object>> list = merchantDao.findByUsername(username);
        return list.isEmpty() ? null : list.get(0);
    }
}
