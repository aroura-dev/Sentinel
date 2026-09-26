-- -----------------------------------------------------------
-- 移库盘点（补建）
--
-- 背景：StocktakeController / StocktakeService / 前端 Stocktake.vue 以及
--       RBAC 菜单 /tms/stocktake 都已就绪，但仓库内从未提供过建表语句，
--       导致 /api/tms/stocktake/** 全部报 “Table 'sentinel.stocktake' doesn't exist”。
--       本脚本按 StocktakeService 实际引用的列补齐，可重复执行。
--
-- 依赖：warehouse / product / inventory（见 sentinel-tms.sql），故排在最后应用。
-- -----------------------------------------------------------

CREATE TABLE IF NOT EXISTS `stocktake` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `stocktake_no` VARCHAR(32) NOT NULL COMMENT '盘点单号',
    `warehouse_id` BIGINT NOT NULL COMMENT '仓库ID',
    `scope` VARCHAR(16) NOT NULL DEFAULT 'ALL' COMMENT '盘点范围：ALL全部 MERCHANT按商家 MANUAL手动选SKU',
    `merchant_id` BIGINT DEFAULT NULL COMMENT '商家ID，scope=MERCHANT 时生效',
    `remark` VARCHAR(256) DEFAULT NULL COMMENT '备注',
    `status` VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT草稿 DONE已完成 CANCELED已取消',
    `total_sku` INT NOT NULL DEFAULT 0 COMMENT '已录入实盘的SKU数',
    `diff_sku` INT NOT NULL DEFAULT 0 COMMENT '存在差异的SKU数',
    `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `finished_at` DATETIME DEFAULT NULL COMMENT '完成时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stocktake_no` (`stocktake_no`),
    KEY `idx_stocktake_warehouse` (`warehouse_id`),
    KEY `idx_stocktake_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='移库盘点单';

-- 明细：inventory 上有 uk_inventory_sku_wh(sku, warehouse_id)，
-- 故同一盘点单内 (stocktake_id, sku) 天然唯一，唯一键安全。
CREATE TABLE IF NOT EXISTS `stocktake_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `stocktake_id` BIGINT NOT NULL COMMENT '盘点单ID',
    `sku` VARCHAR(64) NOT NULL COMMENT 'SKU编码',
    `product_name` VARCHAR(256) DEFAULT NULL COMMENT '商品名称（快照）',
    `merchant_id` BIGINT DEFAULT NULL COMMENT '商家ID',
    `expected` INT NOT NULL DEFAULT 0 COMMENT '账面数量（快照）',
    `counted` INT DEFAULT NULL COMMENT '实盘数量，未盘为 NULL',
    `diff` INT DEFAULT NULL COMMENT '差异 = counted - expected',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stocktake_item_sku` (`stocktake_id`, `sku`),
    KEY `idx_stocktake_item_sku` (`sku`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='移库盘点明细';
