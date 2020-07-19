local Util = require('testbed.util')
local Exec = require('testbed.exec')
local Inspect = require('inspect')

local function flushOne(stmt)
    if not stmt.latencies then
        return
    end
    table.sort(stmt.latencies)

    local p50 = Util.percentile(stmt.latencies, 0.5)
    local p90 = Util.percentile(stmt.latencies, 0.9)
    local p99 = Util.percentile(stmt.latencies, 0.99)

    io.write(('%d;%d;%d;%d\n'):format(stmt.stmtId, p50, p90, p99))
end

local function flush(stmts, start, stop)
    start = start or 1
    stop = stop or #stmts

    for i = start, stop do
        flushOne(stmts[i])
    end
end

local function workaroundHang(stmt, wtune)
    -- shopizer-60 hangs after several runs, don't know why, workaround for now
    if wtune.app ~= 'shopizer' then
        return true
    end

    if stmt.stmtId ~= 60 and stmt.stmtId ~= 104
      and stmt.stmtId ~= 3 and stmt.stmtId ~= 39 then
        return true
    end

    return not stmt.runs or stmt.runs < 1
end

local function evalStmt(stmt, wtune)
    local status, elapsed, value = Exec(stmt, wtune.paramGen:randomLine(), wtune)
    if not status then
        error(value)
    end

    value = nil
    collectgarbage()

    stmt.runs = 1 + (stmt.runs or 0)
    stmt.latencies = stmt.latencies or {}
    table.insert(stmt.latencies, elapsed)

    if elapsed >= 100000000 then
        -- 0.1s
        stmt.is_slow = math.max(1, stmt.is_slow or 0)
    elseif elapsed >= 1000000000 then
        -- 1s
        stmt.is_slow = math.max(2, stmt.is_slow or 0)
    elseif elapsed >= 10000000000 then
        -- 10s
        stmt.is_slow = math.max(3, stmt.is_slow or 0)
    end
end

local function evalStmts(stmts, wtune)
    local seq = Util.iterate(#stmts)
    local totalTimes = wtune.times
    local waterMarker = 0

    local filter = wtune.stmtFilter

    if wtune.dump then
        Util.log('[Sample] writing to file\n', 5)
        io.output(wtune:appFile('eval.' .. wtune.tag, "w"))
    end

    Util.log(('[Eval] %d statements to eval, %d times for each\n'):format(#stmts, wtune.times), 1)
    Util.log('[Eval] ', 1)

    for pass = 1, totalTimes do
        local curMarker = math.floor((pass / totalTimes) * 10)
        if curMarker ~= waterMarker then
            Util.log('.', 1)
            waterMarker = curMarker
        end

        Util.shuffle(seq)

        for _, i in ipairs(seq) do
            local stmt = stmts[i]

            if (not filter or filter(stmt)) and (not stmt.is_slow
                    or (stmt.is_slow == 1 and stmt.runs <= 20)
                    or (stmt.is_slow == 2 and stmt.runs <= 3)
                    or (stmt.is_slow == 3 and stmt.runs <= 1))
                    and workaroundHang(stmt, wtune) then
                local status, value = pcall(evalStmt, stmt, wtune)

                if not status then
                    Util.log(('error when eval %s-%s (pass=%d)\n'):format(wtune.app, stmt.stmtId, pass), 0)
                    Util.log(Inspect(value) .. '\n', 0)
                    error(value)
                end

            end
        end
    end

    Util.log('\n', 1)

    flush(stmts)
    io.output():flush()
    io.output():close()
    io.output(io.stdout)
end

return evalStmts
