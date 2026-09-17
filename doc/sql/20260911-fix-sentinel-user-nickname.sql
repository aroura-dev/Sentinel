-- 修复历史种子数据因导入连接字符集不正确导致的昵称乱码
-- 幂等，可重复执行

SET NAMES utf8mb4;
USE `sentinel`;

UPDATE `sentinel_user` SET `nickname` = '张伟' WHERE `username` = 'zhangwei';
UPDATE `sentinel_user` SET `nickname` = '刘洋' WHERE `username` = 'liuyang';
UPDATE `sentinel_user` SET `nickname` = '王芳' WHERE `username` = 'wangfang';
UPDATE `sentinel_user` SET `nickname` = '陈浩' WHERE `username` = 'chenhao';
UPDATE `sentinel_user` SET `nickname` = '赵敏' WHERE `username` = 'zhaomin';
