local Util = require("testbed.util")
local Exec = require("testbed.exec")
local Inspect = require("inspect")

local POINTS = { 57, 783, 3722, 6951, 8704 }

local function flush(stmts, start, stop)
    for i = start, stop do
        local stmt = stmts[i]
        for point, sample in pairs(stmt.samples) do
            io.write('>%d;%d\n', point, stmt.stmtId, point)
            io.write(sample)
            io.write('\n')
        end
    end
end

local function sampleStmtAtPoint(stmt, point, wtune)
    local status, _, res = Exec(stmt, point, wtune)

    if not status then
        print(Inspect(res))
        error(res)
    end

    local numRows
    numRows, stmt.samples[point] = Util.processResultSet(res)
    return numRows
end

local function sampleStmt(stmt, wtune)
    local numRows = 0
    for _, point in ipairs(POINTS) do
        numRows = numRows + sampleStmtAtPoint(stmt, point, wtune)
    end
    return numRows
end

local function sampleStmts(stmts, wtune)
    if wtune.dump then
        io.output(wtune:appFile("sample"))
    end

    local numRows = 0
    local next = 1
    for i, stmt in ipairs(stmts) do
        numRows = numRows + sampleStmt(stmt, wtune)
        if numRows >= 1000000 then
            flush(stmts, next, i)
            numRows = 0
            next = i + 1
        end
    end
    flush(stmts, next, #stmts)

    io.output():flush()
    io.output(io.stdout)
end

return sampleStmts