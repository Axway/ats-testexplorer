#!/usr/bin/env bash

INTERACTIVE_MODE=0
BATCH_MODE=1

DB_NAME=""
MODE=$INTERACTIVE_MODE

echo "Linux/Docker cases do not work with default OS user"

function print_help() {
  echo "The usage is ./install.sh [OPTION]...[VALUE]...
The following script installs a ATS Logging DB to store test execution results. The version is 4.0.7"
  echo "Available options
-H <target_SQL_server_host>, default is: localhost,Might be specified by env variable: MSSQL_HOST
-p <target_SQL_server_port>, default is: 1433, Might be specified by env variable: MSSQL_PORT
-d <target_SQL_database_name>, Might be specified by env variable: MSSQL_DBNAME
-u <target_SQL_user_name>, default is: AtsUser,Might be specified by env variable: MSSQL_USER_NAME
-s <target_SQL_user_password>, default is: AtsPassword,Might be specified by env variable: MSSQL_USER_PASSWORD
-U <target_SQL_admin_name>,use current OS account Might be specified by env variable: MSSQL_ADMIN_NAME
-S <target_SQL_admin_password>, use current OS account  Might be specified by env variable: MSSQL_ADMIN_PASSWORD"
}

if [ -z "$MSSQL_HOST" ]; then
  MSSQL_HOST=localhost
else
  echo MSSQL_HOST enviroment variable is defined with the value: $MSSQL_HOST
fi

if [ -z "$MSSQL_PORT" ]; then
  MSSQL_PORT=1433
else
  echo MSSQL_PORT enviroment variable is defined with the value: $MSSQL_PORT
fi

if [ -n "$MSSQL_DBNAME" ]; then
  echo MSSQL_DBNAME enviroment variable is defined with the value: "$MSSQL_DBNAME"
  MODE=$BATCH_MODE
fi

if [ -n "$MSSQL_ADMIN_NAME" ]; then
  echo MSSQL_ADMIN_NAME enviroment variable is defined with the value: "$MSSQL_ADMIN_NAME"
fi

if [ -n "$MSSQL_ADMIN_PASSWORD" ]; then
  echo MSSQL_ADMIN_PASSWORD enviroment variable is defined with the value:"$MSSQL_ADMIN_PASSWORD"
fi

if [ -z "$MSSQL_USER_NAME" ]; then
  MSSQL_USER_NAME=AtsUser
else
  echo MSSQL_USER_NAME enviroment variable is defined with the value: $MSSQL_USER_NAME
fi

if [ -z "$MSSQL_USER_PASSWORD" ]; then
  MSSQL_USER_PASSWORD=AtsPassword
else
  echo MSSQL_USER_PASSWORD enviroment variable is defined with the value: $MSSQL_USER_PASSWORD
fi

while getopts ":H:p:d:u:s:U:S:h" option; do
  case $option in
  H)
    MSSQL_HOST=$OPTARG
    ;;
  p)
    MSSQL_PORT=$OPTARG
    ;;
  d)

    MSSQL_DBNAME=$OPTARG
    MODE=$BATCH_MODE
    ;;
  u)
    MSSQL_USER_NAME=$OPTARG
    ;;
  s)
    MSSQL_USER_PASSWORD=$OPTARG

    ;;

  U)
    MSSQL_ADMIN_NAME=$OPTARG
    ;;
  S)
    MSSQL_ADMIN_PASSWORD=$OPTARG

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

if [[ -z "$MSSQL_ADMIN_NAME" ]]; then
  echo "Admin user credentials need to be provided in order to create new database"

  exit 1
fi

SQLCMD_LOCATION=""

if [ -n "$(command -v sqlcmd)" ]; then
  SQLCMD_LOCATION="$(command -v sqlcmd)"
elif [ -n "$(command -v /opt/mssql-tools/bin/sqlcmd)" ]; then
  SQLCMD_LOCATION="$(command -v /opt/mssql-tools/bin/sqlcmd)"
else
  echo "Location of command sqlcmd could not be found"

fi

# iterate until proper and db name is selected when in interactive mode
if [ $MODE == $INTERACTIVE_MODE ]; then
  DATABASE_NOT_EXISTS=1
  while [ "$DATABASE_NOT_EXISTS" == 1 ]; do
    read -p -r 'Enter Database name: ' DB_NAME
    MSSQL_DBNAME=$DB_NAME
    DATABASE_NOT_EXISTS=$($SQLCMD_LOCATION -S tcp:"$MSSQL_HOST","$MSSQL_PORT" -U "$MSSQL_ADMIN_NAME" -P "$MSSQL_ADMIN_PASSWORD" -Q "SELECT COUNT(*) FROM master.dbo.sysdatabases WHERE name = '$MSSQL_DBNAME'" | grep-c 0)
    if [ "$DATABASE_NOT_EXISTS" -eq 0 ]; then
      echo "Error. Database with name '$MSSQL_DBNAME' already exist."
    fi
  done
else
  DATABASE_NOT_EXISTS=$($SQLCMD_LOCATION -S tcp:"$MSSQL_HOST","$MSSQL_PORT" -U "$MSSQL_ADMIN_NAME" -P "$MSSQL_ADMIN_PASSWORD" -Q "SELECT COUNT(*) FROM master.dbo.sysdatabases WHERE name = '$MSSQL_DBNAME'" | grep -c 0)
  if [ "$DATABASE_NOT_EXISTS" -eq 0 ]; then
    echo "Error. Database with name '$MSSQL_DBNAME' already exist. install aborted"
    exit 2
  fi
fi

echo USE [master] >tempCreateDBScript.sql
{
  echo GO

  echo CREATE DATABASE "$MSSQL_DBNAME"

  echo GO

  echo "EXEC dbo.sp_dbcmptlevel @dbname='$MSSQL_DBNAME', @new_cmptlevel=100"

  echo GO

  echo USE ["$MSSQL_DBNAME"]

  echo GO

  echo "CREATE LOGIN $MSSQL_USER_NAME WITH PASSWORD='$MSSQL_USER_PASSWORD', DEFAULT_DATABASE=[$MSSQL_DBNAME], DEFAULT_LANGUAGE=[us_english], CHECK_POLICY=OFF"

  echo GO

  echo "EXEC dbo.sp_grantdbaccess @loginame=[$MSSQL_USER_NAME], @name_in_db=[$MSSQL_USER_NAME]"

  echo GO

  echo "EXEC dbo.sp_addrolemember @rolename=[db_owner], @membername=[$MSSQL_USER_NAME]"

  echo GO

  cat TestExplorerDB.sql
} >>tempCreateDBScript.sql

$SQLCMD_LOCATION -S tcp:"$MSSQL_HOST","$MSSQL_PORT" -U "$MSSQL_ADMIN_NAME" -P "$MSSQL_ADMIN_PASSWORD" -i tempCreateDBScript.sql -W

$SQLCMD_LOCATION -S tcp:"$MSSQL_HOST","$MSSQL_PORT" -U "$MSSQL_USER_NAME" -P "$MSSQL_USER_PASSWORD" -d "$MSSQL_DBNAME" -Q "SELECT * FROM tInternal"
