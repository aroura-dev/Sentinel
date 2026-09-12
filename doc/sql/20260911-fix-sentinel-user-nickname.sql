-- 修复历史种子数据因导入连接字符集不正确导致的昵称乱码
-- 幂等，可重复执行

SET NAMES utf8mb4;
USE `sentinel`;

UPDATE `sentinel_user` SET `nickname` = '系统管理员' WHERE `username` = 'admin';
UPDATE `sentinel_user` SET `nickname` = '运营专员'   WHERE `username` = 'operator';
UPDATE `sentinel_user` SET `nickname` = '客服'       WHERE `username` = 'cs';
UPDATE `sentinel_user` SET `nickname` = '商家'       WHERE `username` = 'merchant';
UPDATE `sentinel_user` SET `nickname` = '财务'       WHERE `username` = 'finance';
