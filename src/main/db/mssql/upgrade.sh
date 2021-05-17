#!/bin/bash

NEW_DB_VERSION=4.0.9
CURRENT_DB_VERSION=4.0.8

INTERACTIVE_MODE=0
BATCH_MODE=1
MODE=$INTERACTIVE_MODE
LOG_FILE_LOCATION='upgrade.log'


function print_help {

    echo  "The usage is ${0} [OPTION VALUE]...
       The following script upgrades an ATS Logging DB from version $OLD_DB_VERSION to current version $NEW_DB_VERSION
        If you have older version you should migrate incrementally,
        i.e. from version 4.0.5 first upgrade to 4.0.6 and then to 4.0.7"
    echo "Available options
        -H <target_SQL_server_host>, default is: localhost,Might be specified by env variable: MSSQL_HOST
        -p <target_SQL_server_port>, default is: 1433, Might be specified by env variable: MSSQL_PORT
        -d <target_SQL_database_name>, default: no. Required for non-interactive (batch mode). Might be specified by env variable: MSSQL_DATABASE
        -u <target_SQL_user_name>, default is: AtsUser,Might be specified by env variable: MSSQL_USER_NAME
        -s <target_SQL_user_password>, default is: AtsPassword,Might be specified by env variable: MSSQL_USER_PASSWORD
        -U <target_SQL_admin_name>,default: no. Required for non-interactive (batch mode). Might be specified by env variable: MSSQL_ADMIN_NAME
        -S <target_SQL_admin_password>,default: no. Required for non-interactive (batch mode).  Might be specified by env variable: MSSQL_ADMIN_PASSWORD
        -q <target_SQLCMD_location>, set sqlcmd location.
        -l <target_LOGFILE_location>, set log file location."
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

if [ -z "$MSSQL_HOST" ];
then
    MSSQL_HOST=localhost
else
    echo "MSSQL_HOST environment variable is defined with the value: $MSSQL_HOST"
fi


if [ -z "$MSSQL_PORT" ];
then
  MSSQL_PORT=1433
else
  echo "MSSQL_PORT environment variable is defined with the value: $MSSQL_PORT"
fi

if [ -n "$MSSQL_DATABASE" ];
then
    echo "MSSQL_DATABASE environment variable is defined with the value: $MSSQL_DATABASE"
    MODE=$BATCH_MODE
fi

if [ -n "$MSSQL_ADMIN_NAME" ]; then
  echo "MSSQL_ADMIN_NAME environment variable is defined with the value: $MSSQL_ADMIN_NAME"
fi

if [ -n "$MSSQL_ADMIN_PASSWORD" ]; then
  echo "MSSQL_ADMIN_PASSWORD environment variable is defined and will be with be used"
fi

if [ -z "$MSSQL_USER_NAME" ];
then
    MSSQL_USER_NAME=AtsUser
else
    echo "MSSQL_USER_NAME environment variable is defined with the value: $MSSQL_USER_NAME"
fi


if [ -z "$MSSQL_USER_PASSWORD" ];
then
    MSSQL_USER_PASSWORD=AtsPassword
else
    echo "MSSQL_USER_PASSWORD environment variable is defined and will be used"
fi


while getopts ":H:p:d:u:s:U:S:q:l:h" option; do
    case $option in
        H)
            MSSQL_HOST=$OPTARG
            ;;
        p)
            MSSQL_PORT=$OPTARG
            ;;
        U)
            MSSQL_ADMIN_NAME=$OPTARG
            ;;
        S)
            MSSQL_ADMIN_PASSWORD=$OPTARG
            ;;
        u)
            MSSQL_USER_NAME=$OPTARG
            ;;
        s)
            MSSQL_USER_PASSWORD=$OPTARG
            ;;
        d)
            MSSQL_DATABASE=$OPTARG
            MODE=$BATCH_MODE
            ;;
        q)
            SQLCMD_LOCATION=$OPTARG
            ;;
        l)
            LOG_FILE_LOCATION=$OPTARG
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


SQLCMD_LOCATION=""
if [  -n "$(command -v sqlcmd)" ];
then
    SQLCMD_LOCATION="$(command -v sqlcmd)"
elif [ -n "$(command -v /opt/mssql-tools/bin/sqlcmd)" ];
then
    SQLCMD_LOCATION="$(command -v /opt/mssql-tools/bin/sqlcmd)"
else
    echo "Location of command sqlcmd could not be found"
fi

echo "Upgrading TestExplorer Database from version $CURRENT_DB_VERSION to $NEW_DB_VERSION"
echo "Current upgrade could take more time especially on large databases. Please do not close console before success/fail message is displayed"
echo "It is recommended that you backup your database before continue!"

# delete and recreate clean tempUpgradeDBScript.sql and upgrade.log
[ -e "tempUpgradeDBScript.sql" ] && rm tempUpgradeDBScript.sql
touch tempUpgradeDBScript.sql
[ -e "upgrade.log" ] && rm upgrade.log
touch upgrade.log

if [ "$(which "$SQLCMD_LOCATION" | wc -l)" -le 0 ];
then
    echo "Error. Location to sqlcmd $SQLCMD_LOCATION is wrong"
    exit 2
fi

DATABASE_EXISTS=0
until [ "$DATABASE_EXISTS" != 0 ]; do

  if [ "$MODE" == "$INTERACTIVE_MODE" ]; then
    read -r -p 'Enter Database name: ' MSSQL_DATABASE
  fi

  # see if database exists
  check_db_existence "$MSSQL_DATABASE"
  DATABASE_EXISTS=$?
  if [ "$DATABASE_EXISTS" == "0" ]; then
    if [ "$MODE" == "$BATCH_MODE" ]; then
      echo "Database named $MSSQL_DATABASE does not exists. Upgrade will abort."
      exit 3
    else
      echo "Database named $MSSQL_DATABASE does not exists. Please choose another name."
    fi
  fi
done


# if custom log file location is used, such file must be created empty prior to db upgrade
[ -e "$LOG_FILE_LOCATION" ] && rm "$LOG_FILE_LOCATION"
touch "$LOG_FILE_LOCATION"

# find the version of the provided DB
# Options for the filtering the result: -h -1 --> do not show headers, dashes; set nocount on --> does not show "(x rows selected)"; xargs - trim string, remove spaces after the actual value;
DB_VERSION=$($SQLCMD_LOCATION -S tcp:"$MSSQL_HOST","$MSSQL_PORT" -U "$MSSQL_ADMIN_NAME" -P "$MSSQL_ADMIN_PASSWORD" -d "$MSSQL_DATABASE" -h -1 -Q "set nocount on; SELECT value FROM tInternal WHERE [key]='version'" | xargs)
if [ "$DB_VERSION" ==  "$NEW_DB_VERSION" ];
then
    echo "There is no need to upgrade. The current DB version is $NEW_DB_VERSION"
    exit 4
elif [ "$DB_VERSION" != "$CURRENT_DB_VERSION" ]
then
    echo "This script upgrades only databases with version $CURRENT_DB_VERSION. Your database's version is $DB_VERSION. You must upgrade it incrementally to $CURRENT_DB_VERSION (e.g 4.0.0 - to 4.0.1, 4.0.1 - to 4.0.2, etc)"
    exit 5
fi

echo "use [$MSSQL_DATABASE]"                                                               > tempUpgradeDBScript.sql
{
echo "GO"
echo "PRINT GETDATE()"
echo "GO"
echo "ALTER DATABASE [$MSSQL_DATABASE] SET ANSI_NULLS ON"
echo "GO"
echo "ALTER DATABASE [$MSSQL_DATABASE] SET ANSI_PADDING ON"
echo "GO"
echo "ALTER DATABASE [$MSSQL_DATABASE] SET ANSI_WARNINGS ON"
echo "GO"
echo "ALTER DATABASE [$MSSQL_DATABASE] SET ARITHABORT ON"
echo "GO"
echo "ALTER DATABASE [$MSSQL_DATABASE] SET CONCAT_NULL_YIELDS_NULL ON"
echo "GO"
echo "ALTER DATABASE [$MSSQL_DATABASE] SET QUOTED_IDENTIFIER ON"
echo "GO"
echo "UPDATE tInternal SET value = '$NEW_DB_VERSION_draft' WHERE [key] = 'version'"
echo "GO"
cat TestExplorerDB-Upgrade.sql
echo "-- end of Upgrade script"
echo "GO"
echo "UPDATE tInternal SET value = '$NEW_DB_VERSION' WHERE [key] = 'version'"
echo "GO"
}>> tempUpgradeDBScript.sql


$SQLCMD_LOCATION -S tcp:"$MSSQL_HOST","$MSSQL_PORT" -U "$MSSQL_ADMIN_NAME" -P "$MSSQL_ADMIN_PASSWORD"  -i tempUpgradeDBScript.sql -W >upgrade.log 2>&1

$SQLCMD_LOCATION -S tcp:"$MSSQL_HOST","$MSSQL_PORT" -U "$MSSQL_USER_NAME" -P "$MSSQL_USER_PASSWORD" -d "$MSSQL_DATABASE" -Q "SELECT * FROM tInternal"

	echo "Upgrade Completed. Check the '$LOG_FILE_LOCATION' for details."

