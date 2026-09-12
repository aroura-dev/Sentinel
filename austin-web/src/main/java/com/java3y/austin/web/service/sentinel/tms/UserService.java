package com.java3y.austin.web.service.sentinel.tms;

import com.java3y.austin.web.dao.SentinelUserDao;
import com.java3y.austin.web.exception.CommonException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户管理服务：5 个固定角色分配（复用前端角色矩阵，不建 role 表）
 *
 * @author sentinel
 */
@Service
public class UserService {

    private static final String STATUS_ENABLED = "1";
    private static final String STATUS_DISABLED = "0";

    private static final List<Map<String, Object>> ROLES = buildRoles();

    private final SentinelUserDao userDao;
    private final AuditLogService auditLogService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(SentinelUserDao userDao, AuditLogService auditLogService) {
        this.userDao = userDao;
        this.auditLogService = auditLogService;
    }

    public Map<String, Object> list(String keyword, String role, String status, int page, int perPage) {
        return userDao.findPage(keyword, role, status, page, perPage);
    }

    public List<Map<String, Object>> roles() {
        return ROLES;
    }

    public Map<String, Object> create(String username, String password, String nickname, String role, String status) {
        if (username == null || username.trim().isEmpty()) {
            throw new CommonException("用户名不能为空");
        }
        if (password == null || password.isEmpty()) {
            throw new CommonException("密码不能为空");
        }
        if (userDao.existsByUsername(username.trim())) {
            throw new CommonException("用户名已存在: " + username);
        }
        Long id = userDao.insert(username.trim(), passwordEncoder.encode(password),
                nickname == null ? username.trim() : nickname,
                role, normalizeStatus(status));
        auditLogService.log("user", "CREATE", username.trim(), "创建用户 角色=" + role);
        return userDao.findById(id);
    }

    public Map<String, Object> update(Long id, String nickname, String role, String status) {
        Map<String, Object> user = userDao.findById(id);
        if (user == null) {
            throw new CommonException("用户不存在: " + id);
        }
        String next = normalizeStatus(status);
        guardDisableAdmin(user, next);
        userDao.update(id, nickname, role, next);
        auditLogService.log("user", "UPDATE", String.valueOf(user.get("username")),
                "修改用户 角色=" + role + " 状态=" + next);
        return userDao.findById(id);
    }

    /** 重置密码（管理员操作，保留审计） */
    public Map<String, Object> resetPassword(Long id, String password) {
        Map<String, Object> user = userDao.findById(id);
        if (user == null) {
            throw new CommonException("用户不存在: " + id);
        }
        if (password == null || password.trim().length() < 6) {
            throw new CommonException("新密码长度不能少于 6 位");
        }
        userDao.updatePassword(id, passwordEncoder.encode(password));
        auditLogService.log("user", "RESET_PWD", String.valueOf(user.get("username")), "重置登录密码");
        return userDao.findById(id);
    }

    public Map<String, Object> toggle(Long id) {
        Map<String, Object> user = userDao.findById(id);
        if (user == null) {
            throw new CommonException("用户不存在: " + id);
        }
        String cur = String.valueOf(user.get("status"));
        String next = STATUS_ENABLED.equals(cur) ? STATUS_DISABLED : STATUS_ENABLED;
        guardDisableAdmin(user, next);
        userDao.updateStatus(id, next);
        auditLogService.log("user", "TOGGLE", String.valueOf(user.get("username")),
                "启停用户 → " + (STATUS_ENABLED.equals(next) ? "启用" : "停用"));
        return userDao.findById(id);
    }

    /** 保护：不允许停用最后一个启用的 ADMIN */
    private void guardDisableAdmin(Map<String, Object> user, String nextStatus) {
        boolean disabling = STATUS_DISABLED.equals(nextStatus)
                && "ADMIN".equals(String.valueOf(user.get("role")))
                && "1".equals(String.valueOf(user.get("status")));
        if (disabling && userDao.countEnabledAdminExcluding(((Number) user.get("id")).longValue()) <= 0) {
            throw new CommonException("至少需要保留一个启用的管理员账号");
        }
    }

    private static String normalizeStatus(String status) {
        return "1".equals(status) || "true".equalsIgnoreCase(String.valueOf(status)) ? STATUS_ENABLED : STATUS_DISABLED;
    }

    private static List<Map<String, Object>> buildRoles() {
        String[][] data = {
                {"ADMIN", "管理员"},
                {"OPERATOR", "运营"},
                {"CUSTOMER_SERVICE", "客服"},
                {"MERCHANT", "商家"},
                {"FINANCE", "财务"}
        };
        List<Map<String, Object>> list = new ArrayList<>();
        for (String[] d : data) {
            Map<String, Object> m = new HashMap<>(4);
            m.put("code", d[0]);
            m.put("name", d[1]);
            list.add(m);
        }
        return list;
    }
}
