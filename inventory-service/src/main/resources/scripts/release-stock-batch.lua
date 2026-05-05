-- KEYS: list of stock keys
-- ARGV: list of quantities (parallel index)
-- Atomic batch release — prevents partial failure leaving Redis inconsistent
for i, key in ipairs(KEYS) do
    redis.call('INCRBY', key, tonumber(ARGV[i]))
end
return #KEYS
