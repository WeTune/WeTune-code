POSTGRESQL='postgresql'
MYSQL='mysql'

recreate=

appName=
dbType=
dbName=

schemaFile=

host=
port=
username=
password=

getDbName() {
  if [ "$1" = 'discourse' ] || [ "$1" = 'gitlab' ] || [ "$1" = 'homeland' ]; then
    dbType=${POSTGRESQL}
    appName=${1}
  else
    head=${1%:*}
    tail=${1##*:}
    if [ "${tail}" != "$1" ]; then
      dbType=${tail}
      appName=${head}
    else
      dbType=${MYSQL}
      appName=${1}
    fi
  fi
  postfix=${2:-'base'}
  dbName=${appName}_${postfix}
}

findSchema() {
  local fileName=${appName}.base.schema.sql
  schemaFile=$(find . -name "$fileName" | head -1)
  if [ ! "$schemaFile" ]; then
    schemaFile=$(find .. -name "$fileName" | head -1)
  fi
  schemaFile=${schemaFile:?"$fileName not found"}
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

doMakeTable() {
  if [ "$dbType" = "$POSTGRESQL" ]; then
    if [ "$recreate" ]; then
      echo "drop db $dbName"
      PGPASSWORD="$password" dropdb -h "$host" -p "$port" -U "$username" "$dbName" >/dev/null 2>&1
    fi
    echo "create db $dbName"
    PGPASSWORD="$password" createdb -h "$host" -p "$port" -U "$username" "$dbName" >/dev/null 2>&1
    PGPASSWORD="$password" psql -h "$host" -p "$port" -U "$username" "$dbName" -f "$schemaFile" >/dev/null 2>&1
  else
    if [ "$recreate" ]; then
      echo "drop db $dbName"
      mysql -u"$username" -p"$password" -h"$host" -P"$port" -e "drop database $dbName" >/dev/null 2>&1
    fi
    echo "create db $dbName"
    mysql -u"$username" -p"$password" -h"$host" -P"$port" -e "create database $dbName" >/dev/null 2>&1
    mysql -u"$username" -p"$password" -h"$host" -P"$port" -D"$dbName" <"$schemaFile" >/dev/null 2>&1
  fi
}

if [ "$1" = "-r" ]; then
  recreate='true'
  shift
fi

getDbName "$1" "$2"
getConnProp "$3" "$4" "$5" "$6"
findSchema
doMakeTable
