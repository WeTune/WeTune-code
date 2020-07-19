local date = require("date")

local BASE_TS = "2020-06-15 00:00:00"
local TS_FORMAT = "'%Y-%m-%d %T'"

local function timeBase()
    return date(BASE_TS)
end

local function timeFmt(dateObj, dbType)
    local str = dateObj:fmt(TS_FORMAT)
    if dbType == 'pgsql' then
        str = str .. '::timestamp'
    end
    return str
end

local function timeParse(str)
    str = str:gsub("'(.+)'.*", '%1')
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
    else
        return str
    end
end

local function log(content, level)
    level = level or 1
    if not sysbench or (sysbench.opt and sysbench.opt.verbosity >= level) then
        io.stdout:write(content)
    end
end

local function tryRequire(path)
    log(("[TRACE] try to find %s\n"):format(path), 5)
    local status, result = pcall(require, path)
    if status then
        return result
    else
        log(("[TRACE] not found %s\n"):format(path), 1)
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

local function normalizeParam(p)
    if type(p) == 'number' then
        if p < 0 then
            return ('(%s)'):format(p)
        elseif p == 0 then
            return 0 -- handle wired '-0'
        end
    end

    return p
end

local function iterate(len)
    local arr = {}
    for i = 1, len do
        table.insert(arr, i)
    end
    return arr
end

local function shuffle(list)
    for i = #list, 1, -1 do
        local index = math.random(1, i)
        list[index], list[i] = list[i], list[index]
    end
    return list
end

local function percentile(list, p)
    p = p <= 1 and p or 1
    p = p >= 0 and p or 0

    local index = math.ceil(#list * p)
    return list[index]
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
    iterate = iterate,
    shuffle = shuffle,
    percentile = percentile,
    normalizeParam = normalizeParam,
    unquote = unquote,
    processResultSet = processResultSet,
    log = log,
    Stack = Stack
}
