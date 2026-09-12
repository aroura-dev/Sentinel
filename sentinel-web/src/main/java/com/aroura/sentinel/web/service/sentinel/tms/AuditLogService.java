package com.aroura.sentinel.web.service.sentinel.tms;

import com.aroura.sentinel.logistics.dao.tms.OperationLogDao;
import com.aroura.sentinel.web.config.AuthInterceptor;
import com.aroura.sentinel.web.vo.CurrentUserVO;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 操作审计服务：记录关键业务动作（谁在何时对哪个单做了什么）
 * <p>
 * 从 {@code RequestContextHolder} 读取当前登录用户（异步任务无请求上下文时记 SYSTEM），
 * 由各业务服务在关键动作点调用，支撑合规审计与问题追溯。
 *
 * @author sentinel
 */
@Service
public class AuditLogService {

    private final OperationLogDao operationLogDao;

    public AuditLogService(OperationLogDao operationLogDao) {
        this.operationLogDao = operationLogDao;
    }

    /**
     * 当前登录用户角色（无请求上下文返回 null，视为系统内部流转）
     */
    public String currentRole() {
        CurrentUserVO user = currentUser();
        return user == null ? null : user.getRole();
    }

    public void log(String module, String action, String targetNo, String detail) {
        CurrentUserVO user = currentUser();
        String operator = user == null ? "SYSTEM" : user.getUsername();
        String role = user == null ? null : user.getRole();
        operationLogDao.insert(operator, role, module, action, targetNo, detail);
    }

    public Map<String, Object> list(String module, String operator, String targetNo, String action,
                                    String start, String end, Long merchantId, int page, int perPage) {
        return operationLogDao.findPage(module, operator, targetNo, action, start, end, merchantId, page, perPage);
    }

    private CurrentUserVO currentUser() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        Object attr = request.getAttribute(AuthInterceptor.CURRENT_USER_ATTR);
        return attr instanceof CurrentUserVO ? (CurrentUserVO) attr : null;
    }
}
