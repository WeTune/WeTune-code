#! env python3

import argparse
import subprocess

base_profile = {
    'db': 'base',
    'schema': 'base',
    'workload': 'base',
    'tag': 'base',
    'rows': '10000',
    'dist': 'uniform',
    'seq': 'typed'
}

opt_profile = {
    'db': 'indexed',
    'schema': 'indexed',
    'workload': 'opt',
    'rows': '10000',
    'dist': 'uniform',
    'seq': 'typed'
}

profiles = { 'base': base_profile, 'opt': opt_profile }

mysql_conn_params = { 'user': 'root', 'password': 'admin', 'port': '3306' }
pgsql_conn_params = { 'user': 'zxd', 'port': '5432' }
conn_params = { 'mysql': mysql_conn_params, 'pgsql': pgsql_conn_params }

hosts = { 'cube2': '10.0.0.102', 'cube3': '10.0.0.103', 'cube5': '10.0.0.105' }

pg_apps = { 'discourse', 'gitlab', 'homeland' }

def prepare_args(args, app):
  pair = app.split(':')
  app = pair[0]
  db_type = pair[1] if len(pair) >= 2 else 'pgsql' if app in pg_apps else 'mysql'

  pack = profiles[args['profile']] if 'profile' in args else {}

  pack['db_type'] = db_type
  pack['conn'] = conn_params[db_type]
  pack['app'] = app
  pack['cmd'] = args['cmd']

  def set_arg(key, default):
    if key in args and args[key]: pack[key] = args[key]
    elif key not in pack and default: pack[key] = default

  set_arg('db', 'base')
  set_arg('tag', 'notag')
  set_arg('schema', 'base')
  set_arg('workload', 'base')
  set_arg('rows', '10000')
  set_arg('dist', 'uniform')
  set_arg('seq', 'typed')
  set_arg('host', '10.0.0.102')
  set_arg('continue', None)
  set_arg('tables', None)

  tables = pack.get('tables')
  if tables and tables[-1] != ',':
    pack['tables'] = tables + ','

  if pack['host'] in hosts:
    pack['host'] = hosts[pack['host']]

  pack['conn']['host'] = pack['host']
  pack['conn']['db'] = pack['app'] + '_' + pack['db']

  return pack


def echo_args(args):
  print("[Exec] app: {}, db: {}, host: {}, tag: {}".format(args['app'], args['app'], args['host'], args['db'], args['tag']))
  print("[Exec] schema.sql: {}, schema.lua: {}, workload.lua: {}".format(args['schema'], args['schema'], args['workload']))
  print("[Exec] #rows: {}, dist: {}, seq: {}".format(args['rows'], args['dist'], args['seq']))


def invoke_sysbench(args):
  real_args = ['sysbench', '--app=' + args['app'], # '--tag=' + args['tag'],
               '--schema=' + args['schema'], # '--workload=' + args['workload'],
               '--rows=' + args['rows'], '--randdist=' + args['dist'], '--randseq=' + args['seq']]

  if 'continue' in args: real_args.append('--continue' + args['continue'])
  if 'table' in args: real_args.append('--tables' + args['tables'])

  db_type = args['db_type']
  real_args.append("--db-driver=" + db_type)

  conn_param = args['conn']
  for k, v in conn_param.items():
    real_args.append("--" + db_type + "-" + k + "=" + v)

  real_args += ["testbed/wtune.lua", args['cmd']]

  print("[Exec] " + " ".join(real_args))

  subprocess.run(real_args)


def invoke_mysql(args, cmd, inFile=None):
  real_args = ['mysql', '-u', args['user'], '-p' + args['password'], '-h', args['host']]
  real_args += cmd
  print(real_args)
  subprocess.run(real_args, stdin=inFile)


def recreate_mysql(args):
  app = args['app']
  db = args['db']
  db_name = '{}_{}'.format(app, db)
  schema_sql = '{}/{}.{}.schema.sql'.format(app, app, db)
  conn_param = args['conn']
  invoke_mysql(conn_param, ['-e', "drop database if exists `{}`".format(db_name)])
  invoke_mysql(conn_param, ['-e', 'create database `{}`'.format(db_name)])
  with open(schema_sql) as inFile:
    invoke_mysql(conn_param, ['-D', db_name], inFile)


def recreate(args):
  if args['db_type'] == 'mysql':
    recreate_mysql(args)
  else:
    assert False


parser = argparse.ArgumentParser()
parser.add_argument('-c', '--cmd', required=True)
parser.add_argument('-p', '--profile')
parser.add_argument('-d', '--db')
parser.add_argument('-t', '--tag')
parser.add_argument('-s', '--schema')
parser.add_argument('-w', '--workload')
parser.add_argument('-r', '--rows')
parser.add_argument('-D', '--dist')
parser.add_argument('-S', '--seq')
parser.add_argument('-H', '--host')
parser.add_argument('--continue')
parser.add_argument('--tables')
parser.add_argument('apps', action='append')

if __name__ == '__main__':
  args = vars(parser.parse_args())
  for app in args['apps']:
    args = prepare_args(args, app)
    echo_args(args)
    if args['cmd'] == 'recreate':
      recreate(args)
    else:
      invoke_sysbench(args)

