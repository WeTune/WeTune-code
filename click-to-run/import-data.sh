#! /bin/bash

data_dir="${WETUNE_DATA_DIR:-wtune_data}"
appName="all"
tag="base"

dbName=
appDataDir=
absoluteAppDataPath=
target_table=

host='localhost'
port='1433'
username='SA'
password='mssql2019Admin'


findAppDataDir() {
  local path="$tag/$appName"
  appDataDir=$(find "${data_dir}" -type d -wholename "*/$path" | head -1)
}

getAbsoluteDataPath() {
  local cwd=$(pwd)
  cd "${appDataDir}" || exit
  absoluteAppDataPath=$(pwd)
  cd "${cwd}" || exit
}

doTruncateOne() {
  local tableName=${1}

  echo "truncating ${tableName}"
#  sqlcmd -U "$username" -P "$password" -S "$host","$port" -d "$dbName" -i "${data_dir}/schemas_mssql/${appName}.sql"
  sqlcmd -U "$username" -P "$password" -S "$host","$port" -d "$dbName" <<EOF
    DELETE FROM ${tableName};
    GO
EOF
}

doImportOne() {
  local tableName=${1}
  getAbsoluteDataPath

  echo "importing ${tableName}"
  sqlcmd -U "$username" -P "$password" -S "$host","$port" -d "$dbName" <<EOF
    ALTER TABLE [${tableName}] NOCHECK CONSTRAINT ALL;
    BULK INSERT [${tableName}] FROM '${absoluteAppDataPath}/${tableName}.csv' WITH( FIELDTERMINATOR=';', ROWTERMINATOR='\n' );
    ALTER TABLE [${tableName}] WITH CHECK CHECK CONSTRAINT ALL;
    GO
EOF
}

enableConstraints() {
  local tableName=${1}
  sqlcmd -U "$username" -P "$password" -S "$host","$port" -d "$dbName" <<EOF
    ALTER TABLE [${tableName}] WITH CHECK CHECK CONSTRAINT ALL;
    GO
EOF
}

doImportData() {
  echo "gonna import $(find "$appDataDir" -maxdepth 1 -name '*.csv' | wc -l) target_tables in $appDataDir to $dbName@$host:$port"
  for fileName in "$appDataDir"/*.csv; do
    fileName=$(basename -- "$fileName")
    local tableName="${fileName%.*}"
    doTruncateOne "$tableName"
  done
  for fileName in "$appDataDir"/*.csv; do
    fileName=$(basename -- "$fileName")
    local tableName="${fileName%.*}"
    doImportOne "$tableName"
  done
  for fileName in "$appDataDir"/*.csv; do
    fileName=$(basename -- "$fileName")
    local tableName="${fileName%.*}"
    enableConstraints "$tableName"
  done
}


while [[ $# -gt 0 ]]; do
  case "$1" in
  "-app")
    appName="${2}"
    shift 2
    ;;
  "-tag")
    tag="${2}"
    shift 2
    ;;
  "-t" | "-table")
    target_table="${2}"
    shift 2
    ;;
  *)
    positional_args+=("${1}")
    shift
    ;;
  esac
done

if [ "${appName}" = 'all' ]; then
  for app in 'broadleaf' 'diaspora' 'discourse' 'eladmin' 'fatfreecrm' 'febs' 'forest_blog' 'gitlab' 'guns' 'halo' 'homeland' 'lobsters' 'publiccms' 'pybbs' 'redmine' 'refinerycms' 'sagan' 'shopizer' 'solidus' 'spree'
  do
    dbName=${app}_${tag}
    findAppDataDir
    if [ ! "$appDataDir" ]; then
      continue
    fi

    doImportData
  done
else
  dbName=${appName}_${tag}
  findAppDataDir
  if [ ! "$appDataDir" ]; then
    echo "data not found for ${dbName}."
    exit
  fi

  if [ -z "$target_table" ]; then
    doImportData
  else
    doImportOne "$target_table"
  fi
fi
