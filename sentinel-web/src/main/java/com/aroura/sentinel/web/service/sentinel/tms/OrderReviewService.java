package com.aroura.sentinel.web.service.sentinel.tms;

import com.aroura.sentinel.logistics.dao.LogisticsDao;
import com.aroura.sentinel.web.config.AuthInterceptor;
import com.aroura.sentinel.web.exception.CommonException;
import com.aroura.sentinel.web.vo.CurrentUserVO;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 订单审核服务：待审核/已驳回列表 + 通过/驳回（状态写 logistics_order.review_status）
 *
 * @author sentinel
 */
@Service
public class OrderReviewService {

    private final LogisticsDao logisticsDao;
    private final AuditLogService auditLogService;

    public OrderReviewService(LogisticsDao logisticsDao, AuditLogService auditLogService) {
        this.logisticsDao = logisticsDao;
        this.auditLogService = auditLogService;
    }

    public Map<String, Object> list(String status, String keyword, int page, int perPage) {
        return logisticsDao.findReviewPage(status, keyword, page, perPage);
    }

    public Map<String, Object> review(String orderNo, boolean approve, String reason) {
        Map<String, Object> order = logisticsDao.findOrderByNo(orderNo);
        if (order == null) {
            throw new CommonException("订单不存在: " + orderNo);
        }
        String operator = currentUsername();
        if (approve) {
            logisticsDao.updateReviewResult(orderNo, "APPROVED", operator, null);
            auditLogService.log("order", "REVIEW_APPROVE", orderNo, "订单审核通过");
        } else {
            if (reason == null || reason.trim().isEmpty()) {
                throw new CommonException("驳回需填写原因");
            }
            logisticsDao.updateReviewResult(orderNo, "REJECTED", operator, reason.trim());
            auditLogService.log("order", "REVIEW_REJECT", orderNo, "订单驳回: " + reason.trim());
        }
        return logisticsDao.findOrderByNo(orderNo);
    }

    private String currentUsername() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return "SYSTEM";
        }
        HttpServletRequest request = attrs.getRequest();
        Object attr = request.getAttribute(AuthInterceptor.CURRENT_USER_ATTR);
        if (attr instanceof CurrentUserVO) {
            return ((CurrentUserVO) attr).getUsername();
        }
        return "SYSTEM";
    }
}
