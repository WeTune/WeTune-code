local Exec = require('testbed.exec')
local Util = require('testbed.util')

local function check(stmt, wtune, baseResult, baseArgs)
    for lineNum = 1, 100, 4 do
        local status, _, rs = Exec(stmt, lineNum, wtune, nil, baseArgs[lineNum])
        if not status then
            return false
        end

        local _, str = Util.processResultSet(rs)
        if str ~= baseResult[lineNum] then
            return false
        end
    end
    return true
end

local function doVerify(stmts, wtune)
    local baseStmt = stmts[1]
    local baseResult = {}
    local baseArgs = {}
    local filter = wtune.indexFilter

    for lineNum = 1, 100, 4 do
        local _, _, rs, args = Exec(baseStmt, lineNum, wtune)
        local _, str = Util.processResultSet(rs)
        baseArgs[lineNum] = args
        baseResult[lineNum] = str
    end

    local checked = {}
    for i = 2, #stmts do
        local stmt = stmts[i]
        if (not filter or filter(stmt)) and check(stmt, wtune, baseResult, baseArgs) then
            table.insert(checked, stmt.index)
        end
    end

    print('>' .. table.concat(checked, ',') .. ',')
end

return doVerify