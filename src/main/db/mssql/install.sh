#!/bin/bash

INTERACTIVE_MODE=0
BATCH_MODE=1

DB_NAME=""
MODE=$INTERACTIVE_MODE
SQLCMD_LOCATION="/opt/mssql-tools/bin/sqlcmd"
LOG_FILE_LOCATION='install.log'

function print_help {
		echo "usage : install.sh [arguments]
-h   | show this help text
-d   | set the database name. Example -d <SOME_DB_NAME>
-s   | set sqlcmd location. Example -s /home/user/tools/sqlcmd
-l   | set log file location. Example -l /home/atsuser/mssql/install.log
"
}


# process user input arguments
while getopts "d:hs:l:" option; do
	case $option in
		d)
			DB_NAME=$OPTARG
			MODE=$BATCH_MODE
		;;
		h)
			print_help
			exit 0
		;;
		s)
			SQLCMD_LOCATION=$OPTARG
		;;
		l)
			LOG_FILE_LOCATION=$OPTARG
		;;
		\?)
			echo "Invallid option: -$OPTARG"
			print_help
			exit 1
		;;
	esac
done

# log message about installing database
echo INSTALLING Test Explorer Database


# delete and recreate clean tempCreateDBScript.sql and install.log
[ -e "tempCreateDBScript.sql" ] && rm tempCreateDBScript.sql
touch tempCreateDBScript.sql
[ -e "install.log" ] && rm install.log
touch install.log


if [ `which $SQLCMD_LOCATION | wc -l` -le 0 ];
then
	echo Error. Location to sqlcmd "'"$SQLCMD_LOCATION"'" is wrong
	exit 2
fi


# iterate until proper and free db name is selected when in interactive mode
if [ $MODE == $INTERACTIVE_MODE ];
then
	DATABASE_NOT_EXISTS=0
	while [ "$DATABASE_NOT_EXISTS" == 0 ]; do
		read -p 'Enter Database name: ' DB_NAME
		DATABASE_NOT_EXISTS=`$SQLCMD_LOCATION -Q "SELECT COUNT(*) FROM master.dbo.sysdatabases WHERE name = '$DB_NAME'" -S localhost -U SA -P $SA_PASSWORD | grep 0 | wc -l`
		if [ $DATABASE_NOT_EXISTS -le 0 ];
		then
			echo "Error. Database with name '$DB_NAME' already exists."
		fi
	done
else
	DATABASE_NOT_EXISTS=`$SQLCMD_LOCATION -Q "SELECT COUNT(*) FROM master.dbo.sysdatabases WHERE name = '$DB_NAME'" -S localhost -U SA -P $SA_PASSWORD | grep 0 | wc -l`
		if [ $DATABASE_NOT_EXISTS -le 0 ];
		then
			echo "Error. Database with name '$DB_NAME' already exists. Install aborted"
			exit 3
		fi
fi

# generate new tempCreateDBScript.sql
echo USE [master] > tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql

echo CREATE DATABASE "$DB_NAME"  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql

echo "EXEC dbo.sp_dbcmptlevel @dbname='"$DB_NAME"', @new_cmptlevel=100" >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql

echo USE ["$DB_NAME"] >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql

echo "CREATE LOGIN AtsUser WITH PASSWORD='AtsPassword', DEFAULT_DATABASE=["$DB_NAME"], DEFAULT_LANGUAGE=[us_english], CHECK_POLICY=OFF" >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql

echo "EXEC dbo.sp_grantdbaccess @loginame=[AtsUser], @name_in_db=[AtsUser]" >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql

echo "EXEC dbo.sp_addrolemember @rolename=[db_owner], @membername=[AtsUser]" >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql

cat TestExplorerDB.sql>> tempCreateDBScript.sql

# if custom log file location is used, such file must be created empty prior to db install
[ -e "$LOG_FILE_LOCATION" ] && rm $LOG_FILE_LOCATION
touch $LOG_FILE_LOCATION

# install the database
$SQLCMD_LOCATION -i tempCreateDBScript.sql -S localhost -U SA -P $SA_PASSWORD -o $LOG_FILE_LOCATION


# TODO get internal/initial version from that database to ensure that install was successful
if [ `cat $LOG_FILE_LOCATION | wc -l` -le 0 ];
then
	echo Installation of ATS Log database "'"$DB_NAME"'" completed
else
	echo Installation of ATS Log database "'"$DB_NAME"'" completed
fi

cat $LOG_FILE_LOCATION

