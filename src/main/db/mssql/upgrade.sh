#!/bin/bash

NEW_DB_VERSION=4.0.7
CURRENT_DB_VERSION=4.0.6

INTERACTIVE_MODE=0
BATCH_MODE=1

DB_NAME=""
MODE=$INTERACTIVE_MODE
LOG_FILE_LOCATION='upgrade.log'


function print_help {

    echo  "The usage is ./script.sh -d (database name) [OPTION VALUE]...
        This scripts upgrades ATS Logging DB to store test execution results from $CURRENT_DB_VERSION to current
        one ($NEW_DB_VERSION).
        If you have older version you should migrate incrementally,
        i.e. from version 4.0.5 first upgrade to 4.0.6 and then to 4.0.7"
    echo "Available options
        -H <target_SQL_server_host>, default is: localhost,Might be specified by env variable: MSSQL_HOST
        -p <target_SQL_server_port>, default is: 1433, Might be specified by env variable: MSSQL_PORT
        -d <target_SQL_database_name>, Might be specified by env variable: MSSQL_DBNAME
        -u <target_SQL_user_name>, default is: AtsUser,Might be specified by env variable: MSSQL_USER_NAME
        -c <target_SQL_user_password>, default is: AtsPassword,Might be specified by env variable: MSSQL_USER_PASSWORD
        -s <target_SQLCMD_location>, set sqlcmd location.
        -l <target_LOGFILE_location>, set log file location."
}


if [ -z "$MSSQL_HOST" ];
then
    MSSQL_HOST=localhost
else
    echo MSSQL_HOST enviroment variable is defined with the value: $MSSQL_HOST
fi


if [ -z "$MSSQL_PORT" ];
then
  MSSQL_PORT=1433
else
  echo MSSQL_PORT enviroment variable is defined with the value: $MSSQL_PORT
fi

if [ -n "$MSSQL_DBNAME" ];
then
    echo MSSQL_DBNAME enviroment variable is defined with the value: "$MSSQL_DBNAME"
    MODE=$BATCH_MODE
fi


if [ -z "$MSSQL_USER_NAME" ];
then
    MSSQL_USER_NAME=AtsUser
else
    echo MSSQL_USER_NAME enviroment variable is defined with the value: $MSSQL_USER_NAME
fi


if [ -z "$MSSQL_USER_PASSWORD" ];
then
    MSSQL_USER_PASSWORD=AtsPassword
else
    echo MSSQL_USER_PASSWORD enviroment variable is defined with the value: $MSSQL_USER_PASSWORD
fi


while getopts ":H:p:d:U:S:d:s:l:h" option; do
    case $option in
        H)
            MSSQL_HOST=$OPTARG
            ;;
        p)
            MSSQL_PORT=$OPTARG
            ;;
        U)
            MSSQL_USER_NAME=$OPTARG
            ;;
        S)
            MSSQL_USER_PASSWORD=$OPTARG
            ;;
        d)
            DB_NAME=$OPTARG
            MODE=$BATCH_MODE
            ;;
        s)
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
    echo Error. Location to sqlcmd "$SQLCMD_LOCATION" is wrong
    exit 2
fi

# iterate until proper DB name is selected when in interactive mode
if [ $MODE == $INTERACTIVE_MODE ];
then
    DATABASE_NOT_EXISTS=1
    while [ "$DATABASE_NOT_EXISTS" == 1 ]; do
        read -r -p 'Enter Database name: ' DB_NAME

        DATABASE_NOT_EXISTS=$($SQLCMD_LOCATION -S tcp:"$MSSQL_HOST","$MSSQL_PORT" -U "$MSSQL_USER_NAME" -P "$MSSQL_USER_PASSWORD" -Q "SELECT COUNT(*) FROM master.dbo.sysdatabases WHERE name = '$DB_NAME'"  | grep -c 0)
        if [ "$DATABASE_NOT_EXISTS" -ne 0 ];
        then
            echo "Error. Database with name '$DB_NAME' does not exist."
        fi
    done
else
    DATABASE_NOT_EXISTS=$($SQLCMD_LOCATION -S tcp:"$MSSQL_HOST","$MSSQL_PORT" -U "$MSSQL_USER_NAME" -P "$MSSQL_USER_PASSWORD" -Q "SELECT COUNT(*) FROM master.dbo.sysdatabases WHERE name = '$DB_NAME'" | grep -c 0 )
    if [ "$DATABASE_NOT_EXISTS" -ne 0 ];
    then
        echo "Error. Database with name '$DB_NAME' does not exist. Upgrade aborted"
        exit 3
    fi
fi

 MSSQL_DBNAME=$DB_NAME

# if custom log file location is used, such file must be created empty prior to db upgrade
[ -e "$LOG_FILE_LOCATION" ] && rm $LOG_FILE_LOCATION
touch "$LOG_FILE_LOCATION"

# find the version of the provided DB
# Options for the filtering the result: -h -1 --> do not show headers, dashes; set nocount on --> does not show "(x rows selected)"; xargs - trim string, remove spaces after the actual value;
DB_VERSION=$($SQLCMD_LOCATION -S tcp:"$MSSQL_HOST","$MSSQL_PORT" -U "$MSSQL_USER_NAME" -P "$MSSQL_USER_PASSWORD" -d "$DB_NAME" -h -1 -Q "set nocount on; SELECT value FROM tInternal WHERE [key]='version'" | xargs)
if [ "$DB_VERSION" ==  $NEW_DB_VERSION ];
then
    echo "There is no need to upgrade. The current DB version is $NEW_DB_VERSION"
    exit 4
elif [ "$DB_VERSION" != "$CURRENT_DB_VERSION" ]
then
    echo "This script upgrades only databases with version $CURRENT_DB_VERSION. Your database's version is $DB_VERSION. You must upgrade it incrementally to $CURRENT_DB_VERSION (e.g 4.0.0 - to 4.0.1, 4.0.1 - to 4.0.2, etc)"
    exit 5
fi

echo "use [$DB_NAME]"                                                               > tempUpgradeDBScript.sql
{
echo "GO"
echo "PRINT GETDATE()"
echo "GO"
echo "ALTER DATABASE [$DB_NAME] SET ANSI_NULLS ON"
echo "GO"
echo "ALTER DATABASE [$DB_NAME] SET ANSI_PADDING ON"
echo "GO"
echo "ALTER DATABASE [$DB_NAME] SET ANSI_WARNINGS ON"
echo "GO"
echo "ALTER DATABASE [$DB_NAME] SET ARITHABORT ON"
echo "GO"
echo "ALTER DATABASE [$DB_NAME] SET CONCAT_NULL_YIELDS_NULL ON"
echo "GO"
echo "ALTER DATABASE [$DB_NAME] SET QUOTED_IDENTIFIER ON"
echo "GO"
echo "UPDATE tInternal SET value = '$NEW_DB_VERSION_draft' WHERE [key] = 'version'"
echo "GO"
cat TestExplorerDB-Upgrade.sql
echo "-- end of Upgrade script"
echo "GO"
echo "UPDATE tInternal SET value = '$NEW_DB_VERSION' WHERE [key] = 'version'"
echo "GO"
}>> tempUpgradeDBScript.sql


$SQLCMD_LOCATION -S tcp:"$MSSQL_HOST","$MSSQL_PORT" -U "$MSSQL_USER_NAME" -P "$MSSQL_USER_PASSWORD"  -i tempUpgradeDBScript.sql -W


echo "Upgrade Completed. Check the '$LOG_FILE_LOCATION' for details."
