#!/usr/bin/env bash

OLD_DB_VERSION=4.0.6
NEW_DB_VERSION=4.0.7
NEEDS_UPGRADE=false

INTERACTIVE_MODE=0
BATCH_MODE=1
MODE=$INTERACTIVE_MODE

# save the starting folder location
START_FOLDER="$PWD"

# delete previous tmpUpgradeDbScript.sql if one exists
rm -rf tmpUpgradeDbScript.sql
touch tmpUpgradeDbScript.sql

# delete previous upgrade.log if one exists
rm -rf upgrade.log
touch upgrade.log

if [ -z "$PGHOST" ]; then
  PGHOST=localhost
else
  echo PGHOST enviroment variable is defined with the value: $PGHOST
fi

if [ -z "$PGPORT" ]; then
  PGPORT=5433
else
  echo PGPORT enviroment variable is defined with the value: $PGPORT
fi

if [ -n "$PGDATABASE" ]; then
  echo PGDATABASE enviroment variable is defined with the value: "$PGDATABASE"
  MODE=$BATCH_MODE
fi

if [ -n "$PGUSER" ]; then
  echo PGUSER enviroment variable is defined with the value: "$PGUSER"
fi

if [ -n "$PGPASSWORD" ]; then
  echo PGPASSWORD enviroment variable is defined with the value: "$PGPASSWORD"
fi

export PGPASSWORD=$PGPASSWORD

if [ -z "$PSQL_USER_NAME" ]; then
  PSQL_USER_NAME=AtsUser
else
  echo PSQL_USER_NAME enviroment variable is defined with the value: $PSQL_USER_NAME
fi

if [ -z "$PSQL_USER_PASSWORD" ]; then
  PSQL_USER_PASSWORD=AtsPassword
else
  echo PSQL_USER_PASSWORD enviroment variable is defined with the value: $PSQL_USER_PASSWORD
fi

while getopts ":H:p:d:u:s:U:S:h" option; do
  case $option in
  H)
    PGHOST=$OPTARG
    export PGHOST=$PGHOST
    ;;
  p)
    PGPORT=$OPTARG
    export PGPORT=$PGPORT
    ;;
  d)

    PGDATABASE=$OPTARG
    export PGDATABASE=$PGDATABASE
    MODE=$BATCH_MODE
    ;;
  u)
    PSQL_USER_NAME=$OPTARG
    ;;
  s)
    PSQL_USER_PASSWORD=$OPTARG

    ;;

  U)
    PGUSER=$OPTARG
    export PGUSER=$PGUSER
    ;;
  S)
    PGPASSWORD=$OPTARG
    export PGPASSWORD=$PGPASSWORD
    ;;

  h)
    print_help

    exit 1
    ;;
  \?)
    echo "Invalid option: -$OPTARG"
    print_help
    exit 1
    ;;
  esac
done

if [[ -z "$PGUSER" ]]; then
  echo "Admin user credentials need to be provided in order to create new database"

  exit 2
fi

function check_db_existance() {

  PGDATABASE="$1"
  # see if database exists
  # psql -U postgres -h localhost -l | grep $PGDATABASE | wc -l`

  DATABASE_EXISTS=$(psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -l | grep -c "$PGDATABASE")

  if [ "$DATABASE_EXISTS" == 0 ]; then
    if [ $MODE == $BATCH_MODE ]; then
      echo "No database exist with the given name. Now will exit"
      exit 3
    else
      echo "No database exist with the given name. Plese select another name"
    fi
  fi

  return "$DATABASE_EXISTS"
}

DATABASE_EXISTS=0
until [ "$DATABASE_EXISTS" == 1 ]; do

  if [ $MODE == $INTERACTIVE_MODE ]; then
    read -r -p 'Enter Database name: ' PGDATABASE
  fi
  # see if database exists
  check_db_existance "$PGDATABASE"
  DATABASE_EXISTS=$?
done

# get database version and change NEEDS_UPGRADE flag if needed
# DB_VERSION=`psql -U AtsUser -h localhost -d $PGDATABASE -t -c "SELECT \"value\" FROM \"tInternal\" WHERE \"key\" = 'version'" | xargs` # | xargs is used to trim the db version string
DB_VERSION=$(psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" -t -c "SELECT \"value\" FROM \"tInternal\" WHERE \"key\" = 'version'" | xargs) # | xargs is used to trim the db version string

if [ "$DB_VERSION" = "$OLD_DB_VERSION" ]; then
  NEEDS_UPGRADE=true
else
  NEEDS_UPGRADE=false
fi

if [ "$NEEDS_UPGRADE" = true ]; then
  echo "UPGRADING \"$PGDATABASE\" from version \"$DB_VERSION\" to \"$NEW_DB_VERSION\""
  echo "\connect $PGDATABASE" >>tmpUpgradeDbScript.sql
  echo " " >>tmpUpgradeDbScript.sql
  echo "UPDATE \"tInternal\" SET value = '${NEW_DB_VERSION}_draft' WHERE key = 'version';" >>tmpUpgradeDbScript.sql
  echo " " >>tmpUpgradeDbScript.sql
  cat TestExplorerDb_PostgreSQL_Upgrade.sql >>tmpUpgradeDbScript.sql
  psql -U "$PGUSER" -h "$PGHOST" -p "$PGPORT" -a -f tmpUpgradeDbScript.sql | grep 'ERROR:\|WARNING:' >upgrade.log

  NUM_OF_ERRORS=$(cat upgrade.log | grep -c 'ERROR:')
  if [[ "$NUM_OF_ERRORS" == 0 ]]; then
    psql -U "$PGUSER" -h "$PGHOST" -d "$PGDATABASE" -t -c "UPDATE \"tInternal\" SET value = '$NEW_DB_VERSION' WHERE key = 'version'"
  else
    echo "Errors during install: $NUM_OF_ERRORS. See upgrade.log file for errors"
    if [ $MODE == $BATCH_MODE ]; then
      exit 4
    fi
  fi

  echo "Installing of \"$PGDATABASE\" completed. See install.log file for potential errors"
  if [ $MODE == $BATCH_MODE ]; then
    exit 0
  fi
  # back to the starting folder location
  cd "$START_FOLDER" || {
    echo "Failed to navigate to starting directory"
    exit 5
  }

else
  echo "Could not upgrade \"$PGDATABASE\" from \"$DB_VERSION\" to \"$NEW_DB_VERSION\""
  if [ $MODE == $BATCH_MODE ]; then
    exit 6
  fi
fi
