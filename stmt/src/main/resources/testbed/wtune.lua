local Schema = require("testbed.schema")
local RandGen = require("testbed.randgen")
local RandSeq = require("testbed.randseq")
local Prepare = require("testbed.prepare")
local Util = require("testbed.util")
local Workload = require("testbed.workload")
local ParamGen = require("testbed.paramgen")
local Sample = require("testbed.sample")

local WTune = {}

local PROFILES = {
    base = {
        schema = "base",
        rows = 100,
        randDist = "uniform",
        randSeq = "random",
        workload = "base",
    },
    opt = {
        schema = "opt",
        rows = 100,
        randDist = "uniform",
        randSeq = "random",
        workload = "opt",
    },
    sample = {
        schema = "opt",
        rows = 10000,
        randDist = "uniform",
        randSeq = "typed",
        workload = "base"
    },
}

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

function WTune:loadSchema()
    local fileName = string.format("%s.%s_schema", self.app, self.schemaTag)
    local schemaDesc = Util.tryRequire(fileName)
    if schemaDesc then
        return Schema(self.app):buildFrom(schemaDesc)
    else
        return ni
    end
end

function WTune:loadWorkload()
    local fileName = string.format("%s.%s_workload", self.app, self.workloadTag)
    local workloadDesc = Util.tryRequire(fileName)
    if workloadDesc then
        return Workload(self.app):buildFrom(workloadDesc)
    else
        return nil
    end
end

function WTune:enableProfile(profileName)
    local profile = PROFILES[profileName]
    if profile then
        for k, v in pairs(profile) do
            self[k] = v
        end
    end
end

function WTune:cleanOptions(options)
    for k, v in pairs(options) do
        if v == "" then
            options[k] = nil
        end
    end
    return options
end

function WTune:initOptions(options)
    options = self:cleanOptions(options)
    if options.profile then
        self:enableProfile(options.profile)
    end

    self.app = options.app
    self.dbType = options.dbType or (sysbench and sysbench.opt.db_driver or "mysql")
    self.tag = options.tag or self.tag or "notag"
    self.schemaTag = options.schema or self.schema or "base"
    self.schema = self:loadSchema()
    self.workloadTag = options.workload or self.workload or "base"
    self.workload = self:loadWorkload()
    self.rows = tonumber(options.rows or self.rows or 100)
    self.randDist = RandGen(options.ranDdist or self.randDist or "uniform")
    self.randSeq = RandSeq(self.randDist, self.rows, options.randSeq or self.randSeq or "typed")
    self.paramGen = ParamGen(self.rows, self)
    self.dump = options.dump == 'true' or options.dump == 'yes'

    if options.continue then
        self.tableFilter = tableFilter("continue", tonumber(options.continue))
    elseif options.tables then
        self.tableFilter = tableFilter("target", options.tables)
    end
end

function WTune:initConn()
    if not sysbench then
        return
    end

    local drv = sysbench.sql.driver()
    local con = drv:connect()

    self.sysbench = sysbench
    self.drv = drv
    self.con = con

    if sysbench.opt.db_driver ~= "pgsql" then
        con:query("SET FOREIGN_KEY_CHECKS=0")
        con:query("SET UNIQUE_CHECKS=0")
        self.db = sysbench.opt.mysql_db
    else
        con:query("SET session_replication_role='replica'")
        self.db = sysbench.opt.pgsql_db
    end

end

local predefined = {
    app = "broadleaf",
    profile = "base",
    dbType = "mysql",
    tables = "blc_order_item_adjustment,",
    rows = 3
}

function WTune:init()
    local options = sysbench and sysbench.opt or predefined
    self:initOptions(options)
    self:initConn()
    return self
end

function WTune:make()
    local o = {}
    setmetatable(o, self)
    self.__index = self
    return o
end

function WTune:getTable(tableName)
    return self.schema:getTable(tableName)
end

function WTune:getColumn(tableName, columnName)
    return self:getTable(tableName):getColumn(columnName)
end

function WTune:getColumnValue(tableName, columnName, lineNum)
    return self:getColumn(tableName, columnName)
               :valueAt(lineNum, self.randSeq, self.rows, self.dbType)
end

function WTune:redirect(lineNum)
    return self.schema:redirect(lineNum, self.rows)
end

function WTune:appFile(fileName, flag)
    return io.open(("%s/%s"):format(self.app, fileName), flag)
end

function WTune:doPrepare()
    Util.log(("[Prepare] Start to prepare database for %s\n"):format(self.app), 1)
    Util.log(("[Prepare] app: %s, schema: %s, db: %s\n"):format(self.app, self.schemaTag, self.db), 2)
    Util.log(("[Prepare] rows: %d, dist: %s, seq: %s\n"):format(self.rows, self.randDist.type, self.randSeq.type), 2)
    Prepare(self.schema.tables, self)
end

function WTune:doSample()
    Util.log(("[Sample] Start to sample %s\n"):format(self.app), 1)
    Util.log(("[Sample] app: %s, schema: %s, db: %s\n"):format(self.app, self.schemaTag, self.db), 2)
    Util.log(("[Sample] rows: %d, dist: %s, seq: %s, workload: %s\n"):format(self.rows, self.randDist.type, self.randSeq.type, self.workloadTag), 2)

    Sample(self.workload.stmts, self)
end

local function doPrepare()
    WTune:make():init():doPrepare()
end

local function doSample()
    WTune:make():init():doSample()
end

if sysbench then
    sysbench.cmdline.options = {
        app = { "app name" },
        profile = { "profile name" },
        tag = { "tag" },
        schema = { "schema file" },
        workload = { "workload file" },
        rows = { "#rows" },
        randDist = { "random distribution", "uniform" },
        randSeq = { "type of random sequence", "typed" },
        continue = { "continue populate tables from given index" },
        tables = { "populate given tables" },
        dump = { "whether to dump to file" }
    }
    sysbench.cmdline.commands = {
        prepare = { doPrepare },
        sample = { doSample }
    }
end

return WTune
