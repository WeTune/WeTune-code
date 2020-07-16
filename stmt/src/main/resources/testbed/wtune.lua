local Schema = require("testbed.schema")
local RandGen = require("testbed.randgen")
local RandSeq = require("testbed.randseq")
local Prepare = require("testbed.prepare")
local Util = require("testbed.util")

local WTune = {}

local PROFILES = {
    eval = {
        schema = "base",
        rows = 100,
        randdist = "uniform",
        randseq = "random",
    },
    base = {
        schema = "base",
        rows = 10000,
        randdist = "uniform",
        randseq = "typed",
    },
    large = {
        schema = "base",
        rows = 1000000,
        randdist = "uniform",
        randseq = "typed",
    },
    zipf = {
        schema = "base",
        rows = 10000,
        randdist = "zipf",
        randseq = "typed",
    },
    large_zipf = {
        schema = "base",
        rows = 1000000,
        randdist = "zipf",
        randseq = "typed",
    }

}

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
    self.schemaTag = options.schema or self.schema or "base"
    self.schema = Schema:make(self.app):buildFrom(require(string.format("%s.%s_schema", self.app, self.schemaTag)))
    self.rows = tonumber(options.rows or self.rows or 100)
    self.randdist = RandGen.make(options.randdist or self.randdist or "uniform")
    self.randseq = RandSeq.make(self.randdist, self.rows, options.randseq or self.randseq or "typed")
    self.dbType = options.dbType or (sysbench and sysbench.opt.db_driver or "mysql")

    if options.continue then
        self.tableFilter = Prepare.tableFilter("continue", options.continue)
    elseif options.tables then
        self.tableFilter = Prepare.tableFilter("target", options.tables)
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

function WTune:doPrepare()
    Util.log(string.format("[Prepare] Start to prepare database for %s\n", self.app))
    Util.log(("[Prepare] app: %s, schema: %s, db: %s\n"):format(self.app, self.schemaTag, self.db))
    Util.log(("[Prepare] rows: %d, dist: %s, seq: %s\n"):format(self.rows, self.randdist.type, self.randseq.type))
    Prepare.populateDb(self.con, self.schema, self.rows, self.randseq, self.dbType, self.tableFilter)
end

local function doPrepare()
    WTune:make():init():doPrepare()
end

if sysbench then
    sysbench.cmdline.options = {
        app = { "app name" },
        profile = { "profile name" },
        schema = { "schema file" },
        rows = { "#rows" },
        randdist = { "random distribution", "uniform" },
        randseq = { "type of random sequence", "typed" },
        continue = { "continue populate tables from given index" },
        tables = { "populate given tables" }
    }
    sysbench.cmdline.commands = {
        prepare = { doPrepare }
    }
end

return WTune