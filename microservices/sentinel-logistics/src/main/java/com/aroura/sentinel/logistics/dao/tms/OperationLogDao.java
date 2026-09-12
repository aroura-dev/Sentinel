package com.aroura.sentinel.logistics.dao.tms;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 操作审计日志 DAO（落库 operation_log）
 * <p>
 * 记录"谁在何时对哪个业务对象做了什么"，支撑企业合规审计与问题追溯。
 *
 * @author sentinel
 */
@Repository
public class OperationLogDao {

    private final JdbcTemplate jdbcTemplate;

    public OperationLogDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(String operator, String operatorRole, String module, String action,
                       String targetNo, String detail) {
        jdbcTemplate.update(
                "INSERT INTO operation_log (operator, operator_role, module, action, target_no, detail) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                operator, operatorRole, module, action, targetNo, detail);
    }

    public Map<String, Object> findPage(String module, String operator, String targetNo, String action,
                                        String start, String end, Long merchantId, int page, int perPage) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (module != null && !module.trim().isEmpty()) {
            where.append(" AND module = ?");
            args.add(module.trim());
        }
        if (operator != null && !operator.trim().isEmpty()) {
            where.append(" AND operator LIKE ?");
            args.add("%" + operator.trim() + "%");
        }
        if (targetNo != null && !targetNo.trim().isEmpty()) {
            where.append(" AND target_no = ?");
            args.add(targetNo.trim());
        }
        if (action != null && !action.trim().isEmpty()) {
            where.append(" AND action = ?");
            args.add(action.trim());
        }
        if (start != null && !start.trim().isEmpty()) {
            where.append(" AND created_at >= ?");
            args.add(start.trim());
        }
        if (end != null && !end.trim().isEmpty()) {
            where.append(" AND created_at < DATE_ADD(?, INTERVAL 1 DAY)");
            args.add(end.trim());
        }
        if (merchantId != null) {
            // 商家作用域：审计目标单号必须属于该商家的订单或运单，防止越权查阅他商家操作记录
            where.append(" AND (target_no IN (SELECT order_no FROM logistics_order WHERE merchant_id = ? AND is_deleted = 0) "
                    + "OR target_no IN (SELECT w.waybill_no FROM waybill w "
                    + "JOIN logistics_order o ON w.order_no = o.order_no WHERE o.merchant_id = ? AND w.is_deleted = 0))");
            args.add(merchantId);
            args.add(merchantId);
        }
        String whereSql = where.toString();
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM operation_log" + whereSql, Integer.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(perPage);
        pageArgs.add((page - 1) * perPage);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM operation_log" + whereSql + " ORDER BY id DESC LIMIT ? OFFSET ?", pageArgs.toArray());
        Map<String, Object> result = new HashMap<>(4);
        result.put("count", count == null ? 0 : count);
        result.put("rows", rows);
        return result;
    }
}
