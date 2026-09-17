package com.aroura.sentinel.web.service.sentinel.tms;

import com.aroura.sentinel.web.dao.SentinelUserDao;
import com.aroura.sentinel.web.exception.CommonException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 用户管理服务：固定角色分配、账号资料维护与状态管理。
 *
 * @author sentinel
 */
@Service
public class UserService {

    private static final String STATUS_ENABLED = "1";
    private static final String STATUS_DISABLED = "0";
    private static final Set<String> ROLE_CODES = new HashSet<>(Arrays.asList(
            "ADMIN", "OPERATOR", "CUSTOMER_SERVICE", "MERCHANT", "FINANCE"));
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[\\p{IsHan}A-Za-z][\\p{IsHan}A-Za-z0-9._-]{1,31}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

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

    public Map<String, Object> create(String username, String password, String nickname,
                                      String phone, String email, String role, String status) {
        String normalizedUsername = required(username, "用户名不能为空").trim();
        if (!USERNAME_PATTERN.matcher(normalizedUsername).matches()) {
            throw new CommonException("用户名需以中文或字母开头，支持中文、字母、数字、点号、下划线和短横线，长度 2-32 位");
        }
        String normalizedPassword = password == null ? "" : password;
        if (normalizedPassword.length() < 6 || normalizedPassword.length() > 64) {
            throw new CommonException("密码长度需为 6-64 位");
        }
        String normalizedNickname = normalizeNickname(nickname);
        String normalizedPhone = normalizePhone(phone);
        String normalizedEmail = normalizeEmail(email);
        String normalizedRole = normalizeRole(role);
        String normalizedStatus = normalizeStatus(status);

        if (userDao.existsByUsername(normalizedUsername)) {
            throw new CommonException("用户名已存在: " + normalizedUsername);
        }
        if (normalizedPhone != null && userDao.existsByPhone(normalizedPhone)) {
            throw new CommonException("手机号已被其他账号使用");
        }
        if (normalizedEmail != null && userDao.existsByEmail(normalizedEmail)) {
            throw new CommonException("邮箱已被其他账号使用");
        }

        Long id = userDao.insert(normalizedUsername, normalizedPhone, normalizedEmail,
                passwordEncoder.encode(normalizedPassword), normalizedNickname, normalizedRole, normalizedStatus);
        auditLogService.log("user", "CREATE", normalizedUsername, "创建用户 角色=" + normalizedRole);
        return userDao.findById(id);
    }

    public Map<String, Object> update(Long id, String nickname, String phone, String email,
                                      String role, String status) {
        Map<String, Object> user = userDao.findById(id);
        if (user == null) {
            throw new CommonException("用户不存在: " + id);
        }
        String normalizedNickname = normalizeNickname(nickname);
        String normalizedPhone = normalizePhone(phone);
        String normalizedEmail = normalizeEmail(email);
        String normalizedRole = normalizeRole(role);
        String nextStatus = normalizeStatus(status);

        if (normalizedPhone != null && userDao.existsByPhoneExcluding(normalizedPhone, id)) {
            throw new CommonException("手机号已被其他账号使用");
        }
        if (normalizedEmail != null && userDao.existsByEmailExcluding(normalizedEmail, id)) {
            throw new CommonException("邮箱已被其他账号使用");
        }

        guardDisableAdmin(user, nextStatus);
        userDao.update(id, normalizedNickname, normalizedRole, nextStatus, normalizedPhone, normalizedEmail);
        auditLogService.log("user", "UPDATE", String.valueOf(user.get("username")),
                "修改用户资料 角色=" + normalizedRole + " 状态=" + nextStatus);
        return userDao.findById(id);
    }

    /** 重置密码（管理员操作，保留审计）。 */
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

    private String normalizeNickname(String nickname) {
        String value = required(nickname, "昵称不能为空").trim();
        if (value.length() < 2 || value.length() > 32) {
            throw new CommonException("昵称长度需为 2-32 位");
        }
        return value;
    }

    private String normalizePhone(String phone) {
        String value = trimToNull(phone);
        if (value != null && !PHONE_PATTERN.matcher(value).matches()) {
            throw new CommonException("请输入有效的 11 位手机号");
        }
        return value;
    }

    private String normalizeEmail(String email) {
        String value = trimToNull(email);
        if (value != null) {
            value = value.toLowerCase();
            if (!EMAIL_PATTERN.matcher(value).matches()) {
                throw new CommonException("请输入有效的邮箱地址");
            }
        }
        return value;
    }

    private String normalizeRole(String role) {
        String value = required(role, "角色不能为空").trim().toUpperCase();
        if (!ROLE_CODES.contains(value)) {
            throw new CommonException("角色不合法: " + role);
        }
        return value;
    }

    private static String normalizeStatus(String status) {
        if ("1".equals(status) || "true".equalsIgnoreCase(String.valueOf(status))) {
            return STATUS_ENABLED;
        }
        if ("0".equals(status) || "false".equalsIgnoreCase(String.valueOf(status))) {
            return STATUS_DISABLED;
        }
        throw new CommonException("状态必须为启用或停用");
    }

    private static String required(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new CommonException(message);
        }
        return value;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** 保护：不允许停用最后一个启用的 ADMIN。 */
    private void guardDisableAdmin(Map<String, Object> user, String nextStatus) {
        boolean disabling = STATUS_DISABLED.equals(nextStatus)
                && "ADMIN".equals(String.valueOf(user.get("role")))
                && "1".equals(String.valueOf(user.get("status")));
        if (disabling && userDao.countEnabledAdminExcluding(((Number) user.get("id")).longValue()) <= 0) {
            throw new CommonException("至少需要保留一个启用的管理员账号");
        }
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