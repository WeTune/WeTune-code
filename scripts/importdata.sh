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

doImportData() {
  cd "$dataDir" || exit
  echo "gonna import $(find . -maxdepth 1 -name '*.csv' | wc -l) tables in $dataDir to $dbName@$host:$port"

  if [ "$dbType" = "$MYSQL" ]; then
    mysql -u"$username" -p"$password" -h"$host" -P"$port" -e 'SET GLOBAL FOREIGN_KEY_CHECKS=0'
    mysqlimport --local --fields-terminated-by=';' -d -u"$username" -p"$password" -h"$host" -P"$port" "$dbName" ./*.csv
    mysql -u"$username" -p"$password" -h"$host" -P"$port" -e 'SET GLOBAL FOREIGN_KEY_CHECKS=1'
  else
    for fileName in ./*.csv; do
      fileName=$(basename -- "$fileName")
      local tableName="${fileName%.*}"
      PGPASSWORD="$password" psql -U "$username" -h "$host" -p "$port" -d "$dbName" \
        -c "truncate table ${tableName} cascade"
    done
    for fileName in ./*.csv; do
      fileName=$(basename -- "$fileName")
      local tableName="${fileName%.*}"
      echo "importing ${tableName}"
      PGPASSWORD="$password" psql -U "$username" -h "$host" -p "$port" -d "$dbName" <<EOF
      set session_replication_role='replica';
      \copy ${tableName} from ${fileName} delimiter ';' csv
EOF
    done
  fi
}

dbType "$1" "$2"
getConnProp "$3" "$4" "$5" "$6"
findDataDir
doImportData
