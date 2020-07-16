-- local inspect = require("inspect")

local Uniform = {}

function Uniform:make()
    local rand = { type = 'uniform' }
    setmetatable(rand, self);
    self.__index = self
    return rand
end

function Uniform:setSeed(seed)
    math.randomseed(seed)
end

function Uniform:forward(step)
    for i = 1, step do
        math.random()
    end
end

function Uniform:nextInt(lower, upper)
    return math.random(lower, upper)
end

local function makeGen(type)
    if type == "uniform" then
        return Uniform:make()
    else
        return assert(false)
    end
end

return {
    make = makeGen
}