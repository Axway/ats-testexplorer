#!/usr/bin/env bash

function print_help() {
  echo "The usage is ./install.sh [OPTION]...[VALUE]...
The following script installs a ATS Logging DB to store test execution results. The version is 4.0.7"
  echo "Available options
-H <target_SQL_server_host>, default is: localhost,Might be specified by env variable: PGHOST
-p <target_SQL_server_port>, default is: 1433, Might be specified by env variable: PGPORT
-d <target_SQL_database_name>, Might be specified by env variable: PGDATABASE
-u <target_SQL_user_name>, default is: AtsUser,Might be specified by env variable: PSQL_USER_NAME
-s <target_SQL_user_password>, default is: AtsPassword,Might be specified by env variable: PSQL_USER_PASSWORD
-U <target_SQL_admin_name>,use current OS account Might be specified by env variable: PGUSER
-S <target_SQL_admin_password>, use current OS account  Might be specified by env variable: PGPASSWORD"
}

function check_db_existance() {

  PGDATABASE="$1"
  # see if database exists
  # psql -U postgres -h localhost -l | grep $PGDATABASE | wc -l`

  DATABASE_EXISTS=$(psql -h $PGHOST -p $PGPORT -U $PGUSER -l | grep $PGDATABASE | wc -l)

  if [ "$DATABASE_EXISTS" != 0 ]; then
    if [ $MODE == $BATCH_MODE ]; then
      echo Database name "$PGDATABASE" already exist. Install abort
      exit 3
    else
      echo Database name "$PGDATABASE" already exist. Please choose another name
    fi
  fi

  return "$DATABASE_EXISTS"
}

INTERACTIVE_MODE=0
BATCH_MODE=1

MODE=$INTERACTIVE_MODE
# save the starting folder location
START_FOLDER="$PWD"

# navigate to the install file directory
cd $(dirname $0)

# delete previous tmpInstallDbScript.sql if one exists
rm -rf tmpInstallDbScript.sql
touch tmpInstallDbScript.sql

# delete previous install.log if one exists
rm -rf install.log
touch install.log

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

if ! [ -z "$PGDATABASE" ]; then
  echo PGDATABASE enviroment variable is defined with the value: $PGDATABASE
  MODE=$BATCH_MODE
fi

if ! [ -z "$PGUSER" ]; then
  echo PGUSER enviroment variable is defined with the value: $PGUSER
fi

if ! [ -z "$PGPASSWORD" ]; then
  echo PGPASSWORD enviroment variable is defined with the value: $PGPASSWORD
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

    exit 0
    ;;
  \?)
    echo "Invallid option: -$OPTARG"
    print_help

    exit 1
    ;;
  esac
done

if [[ -z "$PGUSER" ]]; then
  echo "Admin user credentials need to be provided in order to create new database"

  exit 1
fi

until [ "$DATABASE_EXISTS" == 0 ]; do

  if [ $MODE == $INTERACTIVE_MODE ]; then
    read -p 'Enter Database name: ' PGDATABASE
  fi

  # see if database exists
  # psql -U postgres -h localhost -l | grep $PGDATABASE | wc -l`
  check_db_existance "$PGDATABASE"
  DATABASE_EXISTS=$?
done

echo "Installing \"$PGDATABASE ..."
echo "CREATE DATABASE \"$PGDATABASE\";" >>tmpInstallDbScript.sql
echo " " >>tmpInstallDbScript.sql
echo "\connect $PGDATABASE" >>tmpInstallDbScript.sql
echo " " >>tmpInstallDbScript.sql
cat TestExplorerDb_PostgreSQL.sql >>tmpInstallDbScript.sql

db=$PGDATABASE
unset PGDATABASE

psql -h $PGHOST -p $PGPORT -U $PGUSER -a -f tmpInstallDbScript.sql | grep 'ERROR:' >install.log
NUM_OF_ERRORS=$(cat install.log | grep 'ERROR:' | wc -l)

export PGDATABASE=$db
psql -U $PSQL_USER_NAME -h $PGHOST -p $PGPORT password=$PSQL_USER_PASSWORD -c 'SELECT * FROM "tInternal";'

if [[ "$NUM_OF_ERRORS" == 0 ]]; then
  echo "Installing of \"$PGDATABASE\" completed. Logs are located in install.log file"
  if [ $MODE == $BATCH_MODE ]; then
    exit 2
  fi
else
  echo "Errors during install: $NUM_OF_ERRORS"
  echo "Installing of \"PGDATABASE\" was not successful. Logs are located in install.log file"
  if [ $MODE == $BATCH_MODE ]; then
    exit 0
  fi
fi

# back to the starting folder location
cd $START_FOLDER
