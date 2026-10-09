-- 滑动窗口去重：窗口内达到阈值返回 1，否则写入当前记录并返回 0
-- KEYS[1] 去重 key
-- ARGV[1] 窗口毫秒数
-- ARGV[2] 当前时间戳毫秒
-- ARGV[3] 窗口内容许的最大次数
-- ARGV[4] 当前记录的唯一 score member
local key = KEYS[1]
local windowMs = tonumber(ARGV[1])
local nowMs = tonumber(ARGV[2])
local maxCount = tonumber(ARGV[3])
local member = ARGV[4]

if not windowMs or not nowMs or not maxCount or not member then
  return redis.error_reply('invalid arguments')
end

-- 清理窗口外的历史记录
redis.call('ZREMRANGEBYSCORE', key, 0, nowMs - windowMs)

local count = redis.call('ZCARD', key)
if count >= maxCount then
  return 1
end

redis.call('ZADD', key, nowMs, member)
redis.call('PEXPIRE', key, windowMs)
return 0
