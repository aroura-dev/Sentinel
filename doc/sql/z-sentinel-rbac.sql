-- ============================================================
-- Sentinel 企业级角色权限（P0）：role + role_menu
-- 用法：mysql -h127.0.0.1 -P3307 -uroot -proot123_A austin < doc/sql/z-sentinel-rbac.sql
-- 说明：只跑一次。seed 授权与前端 menu.js 原硬编码角色一致，保证不破坏现有登录。
-- ============================================================

CREATE TABLE IF NOT EXISTS role (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(24) NOT NULL COMMENT '角色编码',
  name VARCHAR(50) NOT NULL COMMENT '角色名',
  description VARCHAR(200) DEFAULT NULL COMMENT '描述',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  is_deleted TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

CREATE TABLE IF NOT EXISTS role_menu (
  id BIGINT NOT NULL AUTO_INCREMENT,
  role_code VARCHAR(24) NOT NULL COMMENT '角色编码',
  menu_path VARCHAR(100) NOT NULL COMMENT '可访问菜单路径(对应前端路由 path)',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_menu (role_code, menu_path)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-菜单授权表';

-- seed 5 角色
INSERT IGNORE INTO role (code, name, description, status) VALUES
('ADMIN', '管理员', '平台全部权限', 1),
('OPERATOR', '运营', '订单履约/运单/售后/运营日常', 1),
('CUSTOMER_SERVICE', '客服', '售后/工单/物流跟踪/智能客服', 1),
('MERCHANT', '商家', '下单/查单/自己商品', 1),
('FINANCE', '财务', '账单/对账/报价', 1);

-- ADMIN：全部菜单
INSERT IGNORE INTO role_menu (role_code, menu_path) VALUES
('ADMIN', '/workbench'),('ADMIN', '/dashboard'),('ADMIN', '/tms/seller-dashboard'),
('ADMIN', '/tms/fulfillment'),('ADMIN', '/tms/order'),('ADMIN', '/tms/order-review'),('ADMIN', '/tms/order-merge'),
('ADMIN', '/tms/waybill'),('ADMIN', '/logistics/track'),('ADMIN', '/tms/sign-back'),
('ADMIN', '/tms/after-sale'),('ADMIN', '/workorder'),('ADMIN', '/tms/claim'),
('ADMIN', '/tms/bill'),('ADMIN', '/tms/rate'),('ADMIN', '/tms/reconcile'),
('ADMIN', '/tms/inventory'),('ADMIN', '/tms/stock-in-out'),('ADMIN', '/tms/stocktake'),
('ADMIN', '/tms/risk-alert'),('ADMIN', '/notification/list'),('ADMIN', '/tms/sla-analysis'),('ADMIN', '/tms/carrier-kpi'),
('ADMIN', '/tms/merchant'),('ADMIN', '/tms/carrier'),('ADMIN', '/tms/channel'),('ADMIN', '/tms/warehouse'),('ADMIN', '/tms/product'),
('ADMIN', '/sys/user'),('ADMIN', '/sys/role'),('ADMIN', '/audit'),('ADMIN', '/tms/api-console'),
('ADMIN', '/ai/chat'),('ADMIN', '/ai/analytics'),('ADMIN', '/agent/cs'),('ADMIN', '/config/knowledge'),('ADMIN', '/agent/log'),('ADMIN', '/agent/trace');

-- OPERATOR
INSERT IGNORE INTO role_menu (role_code, menu_path) VALUES
('OPERATOR', '/workbench'),('OPERATOR', '/dashboard'),('OPERATOR', '/tms/seller-dashboard'),
('OPERATOR', '/tms/fulfillment'),('OPERATOR', '/tms/order'),('OPERATOR', '/tms/order-review'),('OPERATOR', '/tms/order-merge'),
('OPERATOR', '/tms/waybill'),('OPERATOR', '/logistics/track'),('OPERATOR', '/tms/sign-back'),
('OPERATOR', '/tms/after-sale'),('OPERATOR', '/workorder'),('OPERATOR', '/tms/claim'),
('OPERATOR', '/tms/rate'),
('OPERATOR', '/tms/inventory'),('OPERATOR', '/tms/stock-in-out'),('OPERATOR', '/tms/stocktake'),
('OPERATOR', '/tms/risk-alert'),('OPERATOR', '/notification/list'),('OPERATOR', '/tms/sla-analysis'),('OPERATOR', '/tms/carrier-kpi'),
('OPERATOR', '/tms/merchant'),('OPERATOR', '/tms/carrier'),('OPERATOR', '/tms/channel'),('OPERATOR', '/tms/warehouse'),('OPERATOR', '/tms/product'),
('OPERATOR', '/tms/api-console'),
('OPERATOR', '/ai/chat'),('OPERATOR', '/ai/analytics');

-- CUSTOMER_SERVICE
INSERT IGNORE INTO role_menu (role_code, menu_path) VALUES
('CUSTOMER_SERVICE', '/workbench'),
('CUSTOMER_SERVICE', '/logistics/track'),('CUSTOMER_SERVICE', '/tms/sign-back'),
('CUSTOMER_SERVICE', '/tms/after-sale'),('CUSTOMER_SERVICE', '/workorder'),('CUSTOMER_SERVICE', '/tms/claim'),
('CUSTOMER_SERVICE', '/tms/inventory'),
('CUSTOMER_SERVICE', '/tms/risk-alert'),('CUSTOMER_SERVICE', '/notification/list'),
('CUSTOMER_SERVICE', '/ai/chat'),('CUSTOMER_SERVICE', '/agent/cs');

-- MERCHANT
INSERT IGNORE INTO role_menu (role_code, menu_path) VALUES
('MERCHANT', '/workbench'),('MERCHANT', '/tms/seller-dashboard'),
('MERCHANT', '/tms/fulfillment'),('MERCHANT', '/tms/order'),
('MERCHANT', '/tms/waybill'),('MERCHANT', '/logistics/track'),
('MERCHANT', '/tms/after-sale'),('MERCHANT', '/workorder'),
('MERCHANT', '/tms/inventory'),
('MERCHANT', '/tms/risk-alert'),('MERCHANT', '/notification/list'),
('MERCHANT', '/tms/product');

-- FINANCE
INSERT IGNORE INTO role_menu (role_code, menu_path) VALUES
('FINANCE', '/workbench'),('FINANCE', '/dashboard'),
('FINANCE', '/tms/order'),
('FINANCE', '/tms/waybill'),('FINANCE', '/logistics/track'),
('FINANCE', '/tms/after-sale'),('FINANCE', '/workorder'),
('FINANCE', '/tms/bill'),('FINANCE', '/tms/rate'),('FINANCE', '/tms/reconcile'),
('FINANCE', '/tms/inventory'),
('FINANCE', '/tms/risk-alert'),('FINANCE', '/notification/list');

-- Java AgentOps 页面授权（幂等追加）
INSERT IGNORE INTO role_menu (role_code, menu_path) VALUES
('ADMIN', '/agent/ops'),
('OPERATOR', '/agent/ops');
