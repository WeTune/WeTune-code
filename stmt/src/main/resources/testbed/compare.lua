local Util = require('testbed.util')
local Exec = require('testbed.exec')

local function shouldEarlyStop(stmt, wtune)
    -- shopizer-60 hangs after several runs, don't know why, workaround for now
    if wtune.app ~= 'shopizer' then
        return false
    end

    if stmt.stmtId ~= 60 and stmt.stmtId ~= 104
            and stmt.stmtId ~= 3 and stmt.stmtId ~= 39 then
        return false
    end

    return true
end

local function throttleSlowStmt(elapsed)
    if elapsed >= 100000000 then
        -- 0.1s
        return 20
    elseif elapsed >= 1000000000 then
        -- 1s
        return 3
    elseif elapsed >= 10000000000 then
        -- 10s
        return 1
    end
end

local function evalStmt(stmt, wtune, times)
    local paramGen = wtune.paramGen
    local args = paramGen:produce(stmt, paramGen:randomLine())

    local timing = {}

    local status, elapsed = Exec(stmt, args, wtune)
    if not status then
        return nil
    end

    times = (throttleSlowStmt(elapsed) or times) - 1
    table.insert(timing, elapsed)

    for _ = 1, times do
        args = paramGen:produce(stmt, paramGen:randomLine())
        status, elapsed = Exec(stmt, args, wtune)
        if not status then
            return nil
        end
        table.insert(timing, elapsed)
    end
    return timing
end

local function doCompare(stmts, wtune)
    local baseStmt = stmts[1]
    local times = wtune.times
    local filter = wtune.indexFilter

    if shouldEarlyStop(baseStmt, wtune) then
        times = 1
    end

    local timing = evalStmt(baseStmt, wtune, times)

    table.sort(timing)
    local timeout = math.ceil((timing[#timing] / 1000000) * 1.1)
    Util.setTimeout(timeout, wtune.con, wtune.dbType)

    local baseP50 = Util.percentile(timing, 0.5)

    local optimized = {}
    for i = 2, #stmts do
        local stmt = stmts[i]
        if not filter or filter(stmt) then
            timing = evalStmt(stmt, wtune, times, timeout)
            if timing then
                table.sort(timing)
                local p50 = Util.percentile(timing, 0.5)
                if p50 < baseP50 then
                    table.insert(optimized, { index = stmt.index, p50 = p50 })
                else
                    Util.log(('[Compare] slow %d: %d\n'):format(stmt.index, p50), 5)
                end
            end
        end
    end

    table.sort(optimized, function(x, y)
        return x.p50 < y.p50
    end)

    Util.log(('%s-%s\n'):format(wtune.app, baseStmt.stmtId), 3)
    Util.log("0 0 " .. baseP50 .. "\n", 3)
    for i = 1, #optimized do
        local opt = optimized[i]
        Util.log(("%d %d %d\n"):format(i, opt.index, opt.p50), 3)
        optimized[i] = opt.index .. ';' .. opt.p50
    end

    print(('>0;%d,%s'):format(baseP50, table.concat(optimized, ',')))
end

return doCompare