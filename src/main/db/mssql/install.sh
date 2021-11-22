#!/usr/bin/env bash

INTERACTIVE_MODE=0
BATCH_MODE=1
MODE=$INTERACTIVE_MODE

echo "Linux/Docker cases do not work with default OS user"

function print_help() {
  echo "The usage is ${0} [OPTION]...[VALUE]...
The following script installs an ATS Logging DB to store test execution results. The script is for version 4.0.11"
  echo "Available options
-H <target_SQL_server_host>, default is: localhost,Might be specified by env variable: MSSQL_HOST
-p <target_SQL_server_port>, default is: 1433, Might be specified by env variable: MSSQL_PORT
-d <target_SQL_database_name>, default: no. Required for non-interactive (batch mode). Might be specified by env variable: MSSQL_DATABASE
-u <target_SQL_user_name>, default is: AtsUser,Might be specified by env variable: MSSQL_USER_NAME
-s <target_SQL_user_password>, default is: AtsPassword,Might be specified by env variable: MSSQL_USER_PASSWORD
-U <target_SQL_admin_name>,default: no. Required for non-interactive (batch mode). Might be specified by env variable: MSSQL_ADMIN_NAME
-S <target_SQL_admin_password>,default: no. Required for non-interactive (batch mode).  Might be specified by env variable: MSSQL_ADMIN_PASSWORD"
}

function check_db_existence() {
  # return number of existing DBs with provided name;
  # $MSSQL_DATABASE is read as first argument
  MSSQL_DATABASE="$1"

  # Make sure PGPASSWORD is already set
  DBS_OUTPUT=$($SQLCMD_LOCATION -S $MSSQL_HOST,$MSSQL_PORT -U $MSSQL_ADMIN_NAME -P $MSSQL_ADMIN_PASSWORD -Q "EXEC sp_databases")

  if [ $? != 0 ]; then
    echo "List of installed databases could not be retrieved. Possible cause is wrong host or port parameter, DB admin user or password"
    echo "Use option \"-h\" for help"
    exit 6
  fi
  DATABASE_EXISTS=$(echo "$DBS_OUTPUT" | grep -c --regexp="^$MSSQL_DATABASE ")
  return "$DATABASE_EXISTS"
}

if [ -z "$MSSQL_HOST" ]; then
  MSSQL_HOST=localhost
else
  echo "MSSQL_HOST enviroment variable is defined with the value: $MSSQL_HOST"
fi

if [ -z "$MSSQL_PORT" ]; then
  MSSQL_PORT=1433
else
  echo "MSSQL_PORT environment variable is defined with the value: $MSSQL_PORT"
fi

if [ -n "$MSSQL_DATABASE" ]; then
  echo "MSSQL_DATABASE environment variable is defined with the value: $MSSQL_DATABASE"
  MODE=$BATCH_MODE
fi

if [ -n "$MSSQL_ADMIN_NAME" ]; then
  echo "MSSQL_ADMIN_NAME environment variable is defined with the value: $MSSQL_ADMIN_NAME"
fi

if [ -n "$MSSQL_ADMIN_PASSWORD" ]; then
  echo "MSSQL_ADMIN_PASSWORD environment variable is defined and will be with be used"
fi

if [ -z "$MSSQL_USER_NAME" ]; then
  MSSQL_USER_NAME=AtsUser
else
  echo "MSSQL_USER_NAME environment variable is defined with the value: $MSSQL_USER_NAME"
fi

if [ -z "$MSSQL_USER_PASSWORD" ]; then
  MSSQL_USER_PASSWORD=AtsPassword
else
  echo "MSSQL_USER_PASSWORD environment variable is defined and will be with be used"
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

    MSSQL_DATABASE=$OPTARG
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
    echo "Invalid option: -$OPTARG"
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
  exit 11
fi

until [ "$DATABASE_EXISTS" == 0 ]; do

  if [ "$MODE" == "$INTERACTIVE_MODE" ]; then
    read -r -p 'Enter Database name: ' MSSQL_DATABASE
  fi

  # see if database exists
  check_db_existence "$MSSQL_DATABASE"
  DATABASE_EXISTS=$?
  if [ "$DATABASE_EXISTS" != 0 ]; then
    if [ "$MODE" == "$BATCH_MODE" ]; then
      echo "Database named $MSSQL_DATABASE already exists. Installation will abort."
      exit 3
    else
      echo "Database named $MSSQL_DATABASE already exists. Please choose another name."
    fi
  fi
done

# delete previous install.log if one exists
rm -rf install.log
touch install.log

echo USE [master] >tempCreateDBScript.sql
{
  echo GO

  echo CREATE DATABASE "$MSSQL_DATABASE"

  echo GO

  echo "EXEC dbo.sp_dbcmptlevel @dbname='$MSSQL_DATABASE', @new_cmptlevel=100"

  echo GO

  echo USE ["$MSSQL_DATABASE"]

  echo GO

  echo "IF NOT EXISTS ( SELECT name FROM master.sys.server_principals WHERE name = 'AtsUser' )"
  echo "  BEGIN"
  echo "     EXEC master.dbo.sp_addlogin @loginame = N'AtsUser', @passwd = 'AtsPassword', @defdb = N'$MSSQL_DBNAME', @deflanguage = N'us_english'"
  echo "  END"
  echo GO
  echo "EXEC dbo.sp_grantdbaccess @loginame=[$MSSQL_USER_NAME], @name_in_db=[$MSSQL_USER_NAME]"
  echo GO
  echo "       EXEC dbo.sp_addrolemember @rolename=[db_owner], @membername=[$MSSQL_USER_NAME]"
  echo GO

  cat TestExplorerDB.sql
} >>tempCreateDBScript.sql

sed -i "s/AtsUser/$MSSQL_USER_NAME/g" tempCreateDBScript.sql
sed -i "s/AtsPassword/$MSSQL_USER_PASSWORD/g" tempCreateDBScript.sql

$SQLCMD_LOCATION -S tcp:"$MSSQL_HOST","$MSSQL_PORT" -U "$MSSQL_ADMIN_NAME" -P "$MSSQL_ADMIN_PASSWORD" -i tempCreateDBScript.sql -W >install.log 2>&1

NUM_OF_ERRORS=$(grep -ci --regex='^Msg [0-9]*, Level [1-9]*, State' install.log)

$SQLCMD_LOCATION -S tcp:"$MSSQL_HOST","$MSSQL_PORT" -U "$MSSQL_USER_NAME" -P "$MSSQL_USER_PASSWORD" -d "$MSSQL_DATABASE" -Q "SELECT * FROM tInternal"

if [ "$NUM_OF_ERRORS" == 0 ]; then
  echo "Installing of \"$MSSQL_DATABASE\" completed successfully. Logs are located in install.log file"
  if [ $MODE == $BATCH_MODE ]; then
    exit 0
  fi
else
  echo "Errors during install: $NUM_OF_ERRORS"
  echo "Installing of \"$MSSQL_DATABASE\" was not successful. Logs are located in install.log file"
  if [ $MODE == $BATCH_MODE ]; then
    exit 4
  fi
fi

# back to the starting folder location
cd "$START_FOLDER" || {
  echo "Failed to navigate back to the last working directory"
  exit 5
}
