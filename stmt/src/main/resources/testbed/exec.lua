local Timer = require('testbed.timer')
local Util = require("testbed.util")
local Inspect = require('inspect')

local function invoke(con, sql)
    return con:query(sql)
end

local function setTimeout(timeout, con, dbType)
    if dbType == 'pgsql' then
        con:query('SET statement_timeout=' .. timeout)
    else
        con:query('SET max_execution_time=' .. timeout)
    end
end

local function execute(stmt, lineNum, wtune, timeout, providedParams)
    local sql = stmt.sql
    local params = stmt.params
    local con = wtune.con
    local paramGen = wtune.paramGen

    local args = {}
    local indexedArgs = {}

    for i, param in pairs(params) do
        local value
        if providedParams and providedParams[i] then
            value = providedParams[i]
        else
            local warning
            value, warning = paramGen:produce(param, lineNum)
            if warning then
                Util.log(('error when gen %d-th param for %s-%s\n'):format(i, wtune.app, stmt.stmtId), 1)
                if value then
                    Util.log(Inspect(value) .. '\n', 1)
                    error(value)
                end
            end
        end
        indexedArgs[i] = value
        table.insert(args, Util.normalizeParam(value))
    end

    -- assert(#args == #params)
    sql = string.format(sql, unpack(args))

    if timeout then
        setTimeout(timeout, con, wtune.dbType)
    end

    local timer = Timer.timerStart()
    local status, res = pcall(invoke, con, sql)
    local elapsed = Timer.timerStop(timer)

    return status, elapsed, res, indexedArgs
end

return execute