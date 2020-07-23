local Util = require("testbed.util")
local date = require("date")

local Modifiers = {}
local Funcs = {}

local function shiftValue(value, shift)
    shift = shift or 2
    if type(value) == 'number' then
        return value + shift

    elseif type(value) == 'string' then
        if value == 'NULL' then
            return value
        end
        local num = tonumber(Util.unquote(value, "'"))
        if num then
            return ("'%0" .. #value .. "d'"):format(num + shift)
        end
    end

    return nil
end

function Modifiers.column_value(tableName, columnName, position)
    return function(wtune, lineNum, stack)
        for _ = 1, position do
            lineNum = wtune:redirect(lineNum)
        end

        local value = wtune:getColumnValue(tableName, columnName, lineNum)
        stack:push(value)
    end
end

function Modifiers.inverse()
    return function(_, _, stack)
        stack:push(-stack:pop())
    end
end

function Modifiers.subtract()
    return function(_, _, stack)
        local right = stack:pop()
        local left = stack:pop()
        stack:push(left - right)
    end
end

function Modifiers.divide()
    return function(_, _, stack)
        local right = stack:pop()
        local left = stack:pop()
        stack:push(left / right)
    end
end

function Modifiers.add()
    return function(_, _, stack)
        stack:push(stack:pop() + stack:pop())
    end
end

function Modifiers.times()
    return function(_, _, stack)
        stack:push(stack:pop() * stack:pop())
    end
end

function Modifiers.decrease()
    return function(wtune, _, stack)
        local value = stack:pop()
        if type(value) == 'number' then
            stack:push(value - 10)
        elseif value:len() < 19 then
            if value:len() == 1 then
                stack:push(value == "'0'" and "'1'" or "'0'")
            else
                stack:push(shiftValue(value, -10) or ("'0" .. value:sub(2)))
            end
        else
            stack:push(Util.timeFmt(Util.timeParse(value):addhours(-24), wtune.dbType))
        end
    end
end

function Modifiers.increase()
    return function(wtune, _, stack)
        local value = stack:pop()
        if type(value) == 'number' then
            stack:push(value + 10)
        elseif value:len() < 19 then
            if value:len() == 1 then
                stack:push(value == "'0'" and "'1'" or "'0'")
            else
                stack:push(shiftValue(value, 10) or ("'0" .. value:sub(2)))
            end
        else
            stack:push(Util.timeFmt(Util.timeParse(value):addhours(24), wtune.dbType))
        end
    end
end

function Modifiers.like(wildcardPrefix, wildcardSuffix)
    return function(_, _, stack)
        local value = stack:pop()
        if wildcardPrefix then
            value = "'%" .. value:sub(2)
        end
        if wildcardSuffix then
            value = value:sub(1, #value - 1) .. "%'"
        end
        stack:push(value)
    end
end

function Modifiers.regex()
    return function(_, _, stack)
        local value = stack:pop()
        stack:push(value:sub(1, #value - 1) .. ".*'")
    end
end

function Modifiers.check_null()
    return function(_, _, stack)
        local value = stack:pop()
        if value ~= "NULL" then
            value = "NOT NULL"
        end
        stack:push(value)
    end
end

function Modifiers.check_bool()
    return function(_, _, stack)
        local value = stack:pop()
        if value == 'TRUE' or value == 1 then
            stack:push('TRUE')
        else
            stack:push('FALSE')
        end
    end
end

function Modifiers.check_null_not()
    return function(_, _, stack)
        local value = stack:pop()
        if value ~= "NULL" then
            value = "NULL"
        else
            value = "NOT NULL"
        end
        stack:push(value)
    end
end

function Modifiers.check_bool_not()
    return function(_, _, stack)
        local value = stack:pop()
        if value == 'TRUE' or value == 1 then
            stack:push('FALSE')
        else
            stack:push('TRUE')
        end
    end
end

function Modifiers.neq()
    return Modifiers.decrease()
end

function Modifiers.direct_value(value)
    return function(_, _, stack)
        stack:push(value)
    end
end

function Modifiers.make_tuple(count)
    return function(_, _, stack)
        local tuple = {}
        for i = count, 1, -1 do
            tuple[i] = stack:pop()
        end
        tuple.asTuple = true
        stack:push(tuple)
    end
end

function Modifiers.array_element()
    return function(_, _, stack)
        --local value = stack:pop()
        --local shifted = shiftValue(value)
        --if shifted then
        --    stack:push({ value, shifted })
        --else
        --    stack:push(value)
        --end
    end
end

function Modifiers.tuple_element()
    return function(_, _, stack)
        local value = stack:pop()
        local shifted = shiftValue(value)
        if shifted then
            stack:push({ value, shifted })
        else
            stack:push(value)
        end
    end
end

function Modifiers.matching()
    return function(_, _, _)
        -- don't need to do anything
    end
end

function Modifiers.gen_offset()
    return function(_, _, stack)
        stack:push(0)
    end
end

function Modifiers.invoke_agg(funcName)
    return function(_, _, stack)
        stack:push(1000)
    end
end

function Modifiers.invoke_func(funcName, argCount)
    return function(_, _, stack)
        local args = {}
        for i = argCount, 1, -1 do
            args[i] = stack:pop()
        end
        funcName = Util.unquote(funcName)
        local ret = Funcs[funcName](args)
        if ret then
            stack:push(ret)
        end
    end
end

function Funcs.upper(values)
    return values[1]:upper()
end

function Funcs.lower(values)
    return values[1]:lower()
end

function Funcs.extract(values)
    local unit = values[1]
    local value = values[2]
    local time = Util.timeParse(value)
    return time["get" .. unit:lower()](time)
end

function Funcs.coalesce(values)
    for _, value in ipairs(values) do
        if value ~= "NULL" then
            return value
        end
    end
    return "NULL"
end

function Funcs.string_to_array(values)
    return values[1] -- cheat based on current workload
end

function Funcs.length(values)
    return values[1]:len()
end

function Funcs.greatest(values)
    table.sort(values, Util.timeCompare)
    return values[#values]
end

function Funcs.now(values)
    return Util.timeNow()
end

function Funcs.datediff(values)
    local left = Util.timeParse(values[1])
    local right = values[2] -- cheat based on current workload
    return date.diff(left, right):spandays()
end

function Funcs.year(values)
    return Util.timeParse(values[1]):getyear()
end

function Funcs.month(values)
    return Util.timeParse(values[1]):getmonth()
end

function Funcs.dayofmonth(values)
    return Util.timeParse(values[1]):getday()
end

function Funcs.minute(values)
    return Util.timeParse(values[1]):getminutes()
end

function Funcs.weekday(values)
    return Util.timeParse(values[1]):getweekday()
end

function Funcs.second(values)
    return Util.timeParse(values[1]):getseconds()
end

function Funcs.date_format(values)
    return "'" .. Util.timeParse(values[1]):fmt(values[2]) .. "'"
end

return Modifiers