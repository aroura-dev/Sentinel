package com.aroura.sentinel.web.service.sentinel.tms;

import org.springframework.transaction.annotation.Transactional;
import com.aroura.sentinel.web.dao.RoleDao;
import com.aroura.sentinel.web.exception.CommonException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 角色权限服务：角色 CRUD + 角色-菜单授权 + 当前用户可见菜单
 *
 * @author sentinel
 */
@Service
public class RoleService {

    private static final String STATUS_ENABLED = "1";
    private static final String STATUS_DISABLED = "0";

    private final RoleDao roleDao;
    private final AuditLogService auditLogService;

    public RoleService(RoleDao roleDao, AuditLogService auditLogService) {
        this.roleDao = roleDao;
        this.auditLogService = auditLogService;
    }

    public List<Map<String, Object>> list() {
        return roleDao.findAll();
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> create(String code, String name, String description) {
        if (code == null || code.trim().isEmpty() || name == null || name.trim().isEmpty()) {
            throw new CommonException("角色编码和名称不能为空");
        }
        String c = code.trim().toUpperCase();
        if (roleDao.existsByCode(c)) {
            throw new CommonException("角色编码已存在: " + c);
        }
        Long id = roleDao.insert(c, name.trim(), description);
        auditLogService.log("role", "CREATE", c, "创建角色 " + name.trim());
        return roleDao.findById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> update(Long id, String name, String description) {
        Map<String, Object> role = roleDao.findById(id);
        if (role == null) {
            throw new CommonException("角色不存在: " + id);
        }
        roleDao.update(id, name, description);
        auditLogService.log("role", "UPDATE", String.valueOf(role.get("code")), "修改角色 " + name);
        return roleDao.findById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggle(Long id) {
        Map<String, Object> role = roleDao.findById(id);
        if (role == null) {
            throw new CommonException("角色不存在: " + id);
        }
        String cur = String.valueOf(role.get("status"));
        String next = STATUS_ENABLED.equals(cur) ? STATUS_DISABLED : STATUS_ENABLED;
        roleDao.updateStatus(id, next);
        auditLogService.log("role", "TOGGLE", String.valueOf(role.get("code")),
                "启停角色 → " + (STATUS_ENABLED.equals(next) ? "启用" : "停用"));
        return roleDao.findById(id);
    }

    public Map<String, Object> menus(String roleCode) {
        Map<String, Object> role = roleDao.findByCode(roleCode);
        if (role == null) {
            throw new CommonException("角色不存在: " + roleCode);
        }
        Map<String, Object> result = new HashMap<>(4);
        result.put("role", role);
        result.put("paths", roleDao.findMenuPaths(roleCode));
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> saveMenus(String roleCode, List<String> paths) {
        if (roleDao.findByCode(roleCode) == null) {
            throw new CommonException("角色不存在: " + roleCode);
        }
        roleDao.saveMenus(roleCode, paths);
        auditLogService.log("role", "SAVE_MENUS", roleCode, "保存菜单权限 " + (paths == null ? 0 : paths.size()) + " 项");
        Map<String, Object> result = new HashMap<>(4);
        result.put("roleCode", roleCode);
        result.put("paths", roleDao.findMenuPaths(roleCode));
        return result;
    }

    /**
     * 当前用户可访问菜单路径：按角色授权；未配置授权时 configured=false（前端回退硬编码过滤）
     */
    public Map<String, Object> myPermissions(String roleCode) {
        List<String> paths = roleDao.findMenuPaths(roleCode);
        Map<String, Object> result = new HashMap<>(4);
        result.put("role", roleCode);
        result.put("paths", paths);
        result.put("configured", !paths.isEmpty());
        return result;
    }
}
