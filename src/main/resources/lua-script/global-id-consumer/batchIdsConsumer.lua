--[[
    从 ID 池中批量消费指定数量的 ID，
    对于 7.0.0+ 版本的 Redis，采用 LMPOP 命令，反之降级到循环 + RPOP 策略

    KEYS:
        galobalIdKey 全局 ID 池键名

    ARGV:
        batchSize 消费的 ID 数量
]]

-- 获取当前时间戳
local function getTimestamp()
    local redisTime = redis.call('TIME')

    return tonumber(redisTime[1]) * 1000 +
           math.floor(tonumber(redisTime[2]) / 1000)
end

-- 获取版本号的通用函数
local function getRedisVersion()
    -- Redis >= 7.0 有内置变量，直接返回
    if redis.REDIS_VERSION_NUM then
        return redis.REDIS_VERSION
    end

    -- Redis < 7.0 才走老办法：调用 INFO 并解析
    local info = redis.call('INFO', 'server')
    return string.match(info, 'redis_version:([0-9%.]+)')
end

-- 检查是否为高版本
local isLatestVersion
    = tonumber(string.match(getRedisVersion(), '^(%d+)') or '0') >= 7

local galobalIdKey = KEYS[1]

local batchSize = tonumber(ARGV[1])

local status, result = pcall(
        function()
            if isLatestVersion then
                --[[
                    LMPOP 命令的返回如下所示：
                        > LMPOP 1 ids:buffer:global RIGHT COUNT 5
                        1) "ids:buffer:global"
                        2) 1) "\"2029019344279015596\""
                           2) "\"2029019344279015597\""
                           3) "\"2029019344279015598\""
                           4) "\"2029019344279015599\""
                           5) "\"2029019344279015600\""

                    这在 Lua 中会被映射为一个 table, [1] 为键名，[2] 为 ID 数组
                ]]
                local popResult
                    = redis.call('LMPOP', 1, galobalIdKey, 'RIGHT', 'COUNT', batchSize)

                -- 获取 ID 数组
                local ids = {}

                if popResult and popResult[2] then
                    ids = popResult[2]
                end

                local countPoped = #ids -- 实际的批大小

                return {
                    status  = "SUCCESS",
                    message = "Consume batch ids complete!" ..
                              "(batch size = " .. batchSize .. ", poped count = " .. countPoped .. ").",
                    data    = cjson.encode(ids),
                    timestamp = getTimestamp()
                }
            else
                -- Redis 7 以上的版本有 LMPOP 命令，
                -- Redis 6.2.0 RPOP 命令支持批量，
                -- 但咱们公司的版本是沟槽的 3.2.12，所以只能用循环代替
                local bufferLen     = redis.call('LLEN', galobalIdKey)
                local realBatchSize = math.min(batchSize, bufferLen)
                local ids           = {}

                for _ = 1, realBatchSize do
                    local id = redis.call('RPOP', galobalIdKey)
                    if id then
                        table.insert(ids, id)
                    end
                end

                return {
                    status  = "SUCCESS",
                    message = "Consume batch ids complete!" ..
                            "(batch size = " .. batchSize .. ", poped count = " .. realBatchSize .. ").",
                    data    = cjson.encode(ids),
                    timestamp = getTimestamp()
                }
            end
        end
)

if status then
    -- result 是 table（成功或业务失败）
    return cjson.encode(result)
else
    -- Redis 服务端级别的错误或 Lua 运行时错误
    return cjson.encode({
        status    = "UNKNOWN_ERROR",
        message   = tostring(result),
        timestamp = getTimestamp()
    })
end