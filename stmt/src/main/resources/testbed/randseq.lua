local TypedRandSeq = {}
local TYPE_OFFSET = {
    integral = 1,
    fraction = 2,
    boolean = 3,
    enum = 4,
    string = 5,
    bit_string = 6,
    time = 7,
    blob = 8,
    json = 9,
    geo = 10,
    interval = 11,
    net = 12,
    monetary = 13,
    uuid = 14,
    xml = 15,
    range = 16,
    unclassified = 17
}
local MAX_VALUE = 1000000

function TypedRandSeq:make(rand, max)
    local gen = { type = 'typed', rand = rand, max = max,
                  nonUniqueCaches = {}, uniqueCaches = {} }
    setmetatable(gen, self)
    self.__index = self
    return gen
end

function TypedRandSeq:getNonUniqueSeq(dataTypeCat)
    local seq = self.nonUniqueCaches[dataTypeCat]
    if seq then
        return seq
    end

    seq = {}
    self.nonUniqueCaches[dataTypeCat] = seq

    local offset = TYPE_OFFSET[dataTypeCat]
    local rand = self.rand
    for i = 1, self.max do
        rand:setSeed(i)
        rand:forward(offset)
        table.insert(seq, rand:nextInt(1, MAX_VALUE))
    end

    return seq
end

function TypedRandSeq:getUniqueSeq(dataTypeCat)
    local seq = self.uniqueCaches[dataTypeCat]
    if seq then
        return seq
    end

    seq = {}
    self.uniqueCaches[dataTypeCat] = seq

    for i = 1, self.max do
        table.insert(seq, i)
    end

    local nonUniqueSeq = self:getNonUniqueSeq(dataTypeCat)
    for i = self.max, 1, -1 do
        local j = (nonUniqueSeq[i] % i) + 1
        seq[i], seq[j] = seq[j], seq[i]
    end

    return seq
end

function TypedRandSeq:getSeq(dataTypeCat, isUnique)
    if isUnique then
        return self:getUniqueSeq(dataTypeCat)
    else
        return self:getNonUniqueSeq(dataTypeCat)
    end
end

function TypedRandSeq:gen(column, lineNum)
    local dataTypeCat = column.dataType.category:lower()
    local seq = self:getSeq(dataTypeCat, column:uniqueIndex())
    assert(seq)
    return seq[lineNum]
end

local function makeSeq(rand, max, type)
    if type == "typed" then
        return TypedRandSeq:make(rand, max)
    else
        assert(false)
        return nil
    end
end

return {
    make = makeSeq
}