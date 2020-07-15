local Util = require("testbed.util")
local TableGen = require("testbed.tablegen")

local function doInsert(con, numLines)
    return function(insertSql, tableName, ordinal, rowGen)
        Util.log(string.format("[Prepare] %03d. %s", ordinal, tableName))

        con:bulk_insert_init(insertSql)

        for i = 1, numLines do
            if i % 2000 == 0 and i % 10000 ~= 0 then
                Util.log(".")
            end

            if i % 10000 == 0 then
                Util.log(i)
                con:bulk_insert_done()
                con:bulk_insert_init(insertSql)
            end

            con:bulk_insert_next(string.format('(%s)', table.concat(rowGen(i), ', ')))
        end

        con:bulk_insert_done()
        Util.log(" done\n")
    end
end

local function doDump(con, numLines)
    return function(insertSql, tableName, ordinal, rowGen)
        for i = 1, numLines do
            Util.log(string.format('%s (%s)\n', insertSql, table.concat(rowGen(i), ', ')))
        end
    end
end

local function populateTable(con, t, ordinal, numLines, randSeq, dbType)
    local func = con and doInsert or doDump
    TableGen:make(numLines, randSeq, dbType):genTable(t, ordinal, func(con, numLines))
end

local function populateDb(con, schema, numLines, randSeq, dbType, filter)
    local ordinal = 0
    for _, t in pairs(schema.tables) do
        ordinal = ordinal + 1
        if not filter or filter(ordinal, t) then
            populateTable(con, t, ordinal, numLines, randSeq, dbType)
        end
    end
end

local function tableFilter(type, value)
    if type == "continue" then
        return function(ordinal, t)
            return ordinal >= value
        end

    elseif type == "target" then
        local target = {}
        for tableName in value:gmatch("(.-),") do
            target[tableName:lower()] = true
        end
        return function(ordinal, t)
            return target[t.tableName]
        end

    end
end

return {
    populateTable = populateTable,
    populateDb = populateDb,
    tableFilter = tableFilter
}