#! /bin/bash

POSTGRESQL='postgresql'
MYSQL='mysql'

appName=
postfix=
dbType=
dbName=
dataDir=

host=
port=
username=
password=

table=

dbType() {
  if [ "$1" = 'discourse' ] || [ "$1" = 'gitlab' ] || [ "$1" = 'homeland' ]; then
    dbType=${POSTGRESQL}
    appName=${1}
  else
    head="${1%:*}"
    tail="${1##*:}"
    if [ "${tail}" != "$1" ]; then
      dbType=${tail}
      appName=${head}
    else
      dbType=${MYSQL}
      appName=${1}
    fi
  fi
  postfix=${2?:"no postfix specified"}
  dbName=${appName}_${postfix}
}

getConnProp() {
  if [ "$dbType" = "$POSTGRESQL" ]; then
    host=${1:-'10.0.0.103'}
    port=${2:-'5432'}
    username=${3:-'root'}
    password=${4}
  elif [ "$dbType" = "$MYSQL" ]; then
    host=${1:-'10.0.0.103'}
    port=${2:-'3306'}
    username=${3:-'root'}
    password=${4:-'admin'}
  fi
}

findDataDir() {
  local path="$appName/$postfix"
  dataDir=$(find . -type d -wholename "*/$path" | head -1)
  if [ ! "$dataDir" ]; then
    dataDir=$(find .. -type d -wholename "*/$path" | head -1)
  fi
  dataDir=${dataDir:?"$path not found"}
}

doTruncateOne() {
  local tableName=${1}
  if [ "$dbType" = "$POSTGRESQL" ]; then
    PGPASSWORD="$password" psql -U "$username" -h "$host" -p "$port" -d "$dbName" \
      -c "truncate table ${tableName} cascade" &>/dev/null || echo "truncate ${tableName} failed"
  fi
}

doImportOne() {
  local tableName=${1}
  local fileName="${tableName}.csv"
  local cwd=

  cwd=$(pwd)

  cd "$dataDir" || exit

  echo "importing ${tableName}"
  if [ "$dbType" = "$MYSQL" ]; then
    mysql -u"$username" -p"$password" -h"$host" -P"$port" 2>/dev/null <<EOF
    set global foreign_key_checks=0;
    set global unique_checks=0;
EOF
    mysqlimport --local --fields-terminated-by=';' --fields-optionally-enclosed-by='"' -d \
      -u"$username" -p"$password" -h"$host" -P"$port" --use-threads=8 \
      "$dbName" "${tableName}.csv" #2>/dev/null
    mysql -u"$username" -p"$password" -h"$host" -P"$port" 2>/dev/null <<EOF
    set global foreign_key_checks=1;
    set global unique_checks=1;
EOF
  else
    PGPASSWORD="$password" psql -U "$username" -h "$host" -p "$port" -d "$dbName" <<EOF
      set session_replication_role='replica';
      \copy ${tableName} from ${fileName} delimiter ';' csv
EOF
  fi

  cd "${cwd}" || exit
}

doImportData() {
  echo "gonna import $(find "$dataDir" -maxdepth 1 -name '*.csv' | wc -l) tables in $dataDir to $dbName@$host:$port"
  for fileName in "$dataDir"/*.csv; do
    fileName=$(basename -- "$fileName")
    local tableName="${fileName%.*}"
    doTruncateOne "$tableName"
  done
  for fileName in "$dataDir"/*.csv; do
    fileName=$(basename -- "$fileName")
    local tableName="${fileName%.*}"
    doImportOne "$tableName"
  done
}

if [ "$1" = '-t' ]; then
  table="$2"
  shift 2
fi

dbType "$1" "$2"
getConnProp "$3" "$4" "$5" "$6"
findDataDir

if [ -z "$table" ]; then
  doImportData
else
  doImportOne "$table"
fi
