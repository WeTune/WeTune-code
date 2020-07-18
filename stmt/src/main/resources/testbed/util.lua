local date = require("date")

local BASE_TS = date("2020-06-15 00:00:00")
local TS_FORMAT = "'%Y-%m-%d %T'"

local function timeBase()
    return BASE_TS
end

local function timeFmt(dateObj, dbType)
    local str = dateObj:fmt(TS_FORMAT)
    if dbType == 'pgsql' then
        str = str .. '::timestamp'
    end
    return str
end

local function timeParse(str)
    str = str:gsub("'(.+)'", '%1')
    return date(str)
end

local function timeCompare(str1, str2)
    str1 = str1:gsub("'(.+)'", '%1')
    str2 = str2:gsub("'(.+)'", '%1')
    return str1 < str2
end

local function timeNow()
    return date(false)
end

local function unquote(str, quotation)
    if #str < 2 then
        return str
    end

    if not quotation then
        return unquote(unquote(unquote(str, '`'), '"'), "'")
    end

    local first = str:sub(1, 1)
    local last = str:sub(#str, #str)
    if first == quotation and last == quotation then
        return str:sub(2, #str - 1)
    end
end

local function log(_content, level)
    level = level or 1
    if not sysbench or sysbench.opt.verbosity >= level then
        io.write(_content)
    end
end

local function tryRequire(path)
    log("[TRACE] try to find " .. path, 5)
    local status, result = pcall(require, path)
    if status then
        return result
    else
        log("[TRACE] not found " .. path, 1)
        return nil
    end
end

local function processResultSet(rs)
    local numRows = rs.nrows
    local numFields = rs.nfields
    local ret = {}

    for _ = 1, numRows do
        local row = rs:fetch_row()
        local r = {}
        for j = 1, numFields do
            table.insert(r, row[j] or " ")
        end
        table.insert(ret, table.concat(r, "|"))
    end

    return numRows, table.concat(ret, "\n")
end

local Stack = {}

function Stack:make()
    local stack = { top = 0, data = {} }
    setmetatable(stack, self)
    self.__index = self
    return stack
end

function Stack:push(o)
    self.top = self.top + 1
    self.data[self.top] = o
end

function Stack:peek()
    if self.top <= 0 then
        error("empty stack")
    end
    return self.data[self.top]
end

function Stack:pop()
    local top = self:peek()
    self.top = self.top - 1
    return top
end

function Stack:size()
    return self.top
end

return {
    timeBase = timeBase,
    timeFmt = timeFmt,
    timeParse = timeParse,
    timeCompare = timeCompare,
    timeNow = timeNow,
    tryRequire = tryRequire,
    unquote = unquote,
    processResultSet = processResultSet,
    log = log,
    Stack = Stack
}