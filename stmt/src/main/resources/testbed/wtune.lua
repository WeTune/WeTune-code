local Schema = require("testbed.schema")
local RandGen = require("testbed.randgen")
local RandSeq = require("testbed.randseq")
local Prepare = require("testbed.prepare")

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

function WTune:initOptions(options)
    if options.profile then
        self:enableProfile(options.profile)
    end

    self.app = options.app
    self.schemaTag = options.schema or self.schema or "base"
    self.schema = Schema:make(self.app):buildFrom(require(string.format("%s.%s_schema", self.app, self.schemaTag)))
    self.rows = options.rows or self.rows or 100
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
    local con = self.drv:connect()

    if sysbench.opt.db_driver ~= "pgsql" then
        con:query("SET FOREIGN_KEY_CHECKS=0")
        con:query("SET UNIQUE_CHECKS=0")
    else
        con:query("SET session_replication_role='replica'")
    end

    self.sysbench = sysbench
    self.drv = drv
    self.con = con
end

local predefined = {
    app = "test",
    profile = "base",
    dbType = "mysql",
    --tables = "blc_email_tracking_opens,",
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
        rows = { "#rows", 100 },
        randdist = { "random distribution", "uniform" },
        randseq = { "type of random sequence", "typed" },
        continue = { "continue populate tables from given index" },
        tables = { "populate given tables" }
    }
    sysbench.cmdline.commands = {
        prepare = doPrepare
    }
end

return WTune