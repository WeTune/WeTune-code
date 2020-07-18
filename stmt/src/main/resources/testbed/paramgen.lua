local Util = require("testbed.util")
local ParamGen = {}

function ParamGen:randomLine()
    return math.random(self.maxLine)
end

function ParamGen:produce(paramDesc, lineNum)
    local stack = Util.Stack:make()
    local warning = false

    for _, modifier in ipairs(paramDesc) do
        local status, err = pcall(modifier, self.wtune, lineNum, stack)
        if not status then
            return err, true
        end
    end

    if stack:size() ~= 1 then
        warning = true
    end

    local value = stack:pop()
    if type(value) == 'table' then
        if value.asTuple then
            value = '(' .. table.concat(value, ', ') .. ')'
        else
            value = table.concat(value, ', ')
        end
    end

    return value, warning
end

function ParamGen:make(maxLine, wtune)
    local gen = { wtune = wtune, maxLine = maxLine }
    setmetatable(gen, self)
    self.__index = self
    return gen
end

local function makeGen(maxLine, wtune)
    return ParamGen:make(maxLine, wtune)
end

return makeGen