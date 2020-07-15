local BASE_TS = { 2020, 7, 15, 0, 0, 0 }
local TS_FORMAT = "'%d-%02d-%02d %02d:%02d:%02d'"

local function ts2Str(ts)
    local _ret = string.format(TS_FORMAT, ts[1], ts[2], ts[3], ts[4], ts[5], ts[6])
    return _ret
end

local function ts2Int(ts)
    return os.time({ year = ts[1], month = ts[2], day = ts[3], hour = ts[4], min = ts[5], sec = ts[6] })
end

local function int2Ts(i)
    local _date = os.date("*t", i)
    return { _date.year, _date.month, _date.day, _date.hour, _date.min, _date.sec, 0 }
end
local function baseTs()
    return ts2Int(BASE_TS)
end

local function log(_content)
    if not sysbench or sysbench.opt.verbosity ~= 0 then
        io.write(_content)
    end
end

return {
    baseTs = baseTs,
    ts2Int = ts2Int,
    ts2Str = ts2Str,
    int2Ts = int2Ts,
    log = log
}