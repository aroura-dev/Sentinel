-- 修复历史种子数据因导入连接字符集不正确导致的昵称乱码
-- 幂等，可重复执行

SET NAMES utf8mb4;
USE `sentinel`;

UPDATE `sentinel_user` SET `nickname` = '张伟' WHERE `username` = '张伟';
UPDATE `sentinel_user` SET `nickname` = '刘洋' WHERE `username` = '刘洋';
UPDATE `sentinel_user` SET `nickname` = '王芳' WHERE `username` = '王芳';
UPDATE `sentinel_user` SET `nickname` = '陈浩' WHERE `username` = '陈浩';
UPDATE `sentinel_user` SET `nickname` = '赵敏' WHERE `username` = '赵敏';
