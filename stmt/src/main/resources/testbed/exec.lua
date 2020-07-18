local Timer = require('testbed.timer')

local function invoke(con, sql)
    return con:query(sql)
end

local function execute(stmt, lineNum, wtune)
    local sql = stmt.sql
    local params = stmt.params
    local con = wtune.con
    local paramGen = wtune.paramGen

    local args = {}
    for _, param in ipairs(params) do
        table.insert(args, paramGen:produce(param, lineNum))
    end

    assert(#args == #params)
    sql = string.format(sql, table.unpack(args))

    local timer = Timer.timerStart()
    local status, res = pcall(invoke, con, sql)
    local elapsed = Timer.timerStop(timer)

    return status, elapsed, res
end

return execute