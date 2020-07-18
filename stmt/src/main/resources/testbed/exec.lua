local Timer = require('testbed.timer')
local Util = require("testbed.util")
local Inspect = require('inspect')

local function invoke(con, sql)
    return con:query(sql)
end

local function execute(stmt, lineNum, wtune)
    local sql = stmt.sql
    local params = stmt.params
    local con = wtune.con
    local paramGen = wtune.paramGen

    local args = {}
    for i, param in ipairs(params) do
        local value, warning = paramGen:produce(param, lineNum)
        if warning then
            Util.log(('error when gen %d-th param for %s-%s\n'):format(i, wtune.app, stmt.stmtId), 0)
            if value then
                print(Inspect(value))
                error(value)
            end
        end
        table.insert(args, Util.normalizeParam(value))
    end

    assert(#args == #params)
    sql = string.format(sql, unpack(args))

    local timer = Timer.timerStart()
    local status, res = pcall(invoke, con, sql)
    local elapsed = Timer.timerStop(timer)

    return status, elapsed, res
end

return execute