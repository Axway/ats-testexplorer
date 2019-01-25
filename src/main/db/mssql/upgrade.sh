#!/bin/bash

NEW_DB_VERSION=4.1.0
CURRENT_DB_VERSION=4.0.6

INTERACTIVE_MODE=0
BATCH_MODE=1

DB_NAME=""
MODE=$INTERACTIVE_MODE
SQLCMD_LOCATION="/opt/mssql-tools/bin/sqlcmd"
LOG_FILE_LOCATION='upgrade.log'


function print_help {
		echo "usage : install.sh [arguments]
-h   | show this help text
-d   | set the database name. Example -d <SOME_DB_NAME>
-s   | set sqlcmd location. Example -s /home/user/tools/sqlcmd
-l   | set log file location. Example -l /home/atsuser/mssql/upgrade.log
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

echo "Upgrading TestExplorer Database from version $CURRENT_DB_VERSION to $NEW_DB_VERSION"
echo "Current upgrade could take more time especially on large databases. Please do not close console before success/fail message is displayed"
echo "It is recommended that you backup your database before continue!"

# delete and recreate clean tempUpgradeDBScript.sql and upgrade.log
[ -e "tempUpgradeDBScript.sql" ] && rm tempUpgradeDBScript.sql
touch tempUpgradeDBScript.sql
[ -e "upgrade.log" ] && rm upgrade.log
touch upgrade.log

if [ `which $SQLCMD_LOCATION | wc -l` -le 0 ];
then
	echo Error. Location to sqlcmd "'"$SQLCMD_LOCATION"'" is wrong
	exit 2
fi

# iterate until proper and db name is selected when in interactive mode
if [ $MODE == $INTERACTIVE_MODE ];
then
	DATABASE_NOT_EXISTS=1
	while [ "$DATABASE_NOT_EXISTS" == 1 ]; do
		read -p 'Enter Database name: ' DB_NAME
		DATABASE_NOT_EXISTS=`$SQLCMD_LOCATION -Q "SELECT COUNT(*) FROM master.dbo.sysdatabases WHERE name = '$DB_NAME'" -S localhost -U SA -P $SA_PASSWORD | grep 0 | wc -l`
		if [ $DATABASE_NOT_EXISTS -ge 1 ];
		then
			echo "Error. Database with name '$DB_NAME' does not exist."
		fi
	done
else
	DATABASE_NOT_EXISTS=`$SQLCMD_LOCATION -Q "SELECT COUNT(*) FROM master.dbo.sysdatabases WHERE name = '$DB_NAME'" -S localhost -U SA -P $SA_PASSWORD | grep 0 | wc -l`
		if [ $DATABASE_NOT_EXISTS -ge 1 ];
		then
			echo "Error. Database with name '$DB_NAME' does not exist. Upgrade aborted"
			exit 3
		fi
fi

# if custom log file location is used, such file must be created empty prior to db upgrade
[ -e "$LOG_FILE_LOCATION" ] && rm $LOG_FILE_LOCATION
touch $LOG_FILE_LOCATION

# find the version of the provided db
DB_VERSION=`$SQLCMD_LOCATION -S localhost -U SA -P $SA_PASSWORD -d $DB_NAME -Q "SELECT value FROM tInternal WHERE [key]='version'" | grep '[0-9]\{1,2\}\.[0-9]\{1,2\}\.[0-9]\{1,2\}.*'`
if [ "$DB_VERSION" ==  $NEW_DB_VERSION ];
then
	echo "There is no need to upgrade. The current DB version is $NEW_DB_VERSION"
	exit 4
elif [ "$DB_VERSION" != "$CURRENT_DB_VERSION" ]
	echo "This script upgrades only databases with version $CURRENT_DB_VERSION. Your database's version is $DB_VERSION. You must upgrade it incrementally to $CURRENT_DB_VERSION (e.g 4.0.0 - to 4.0.1, 4.0.1 - to 4.0.2, etc)"
	exit 5
fi

echo "use [$DB_NAME]"                                                               >> tempUpgradeDBScript.sql
echo "GO"                                                                           >> tempUpgradeDBScript.sql
echo "PRINT GETDATE()"                                                              >> tempUpgradeDBScript.sql
echo "GO"                                                                           >> tempUpgradeDBScript.sql
echo "ALTER DATABASE [$DB_NAME] SET ANSI_NULLS ON"                                  >> tempUpgradeDBScript.sql
echo "GO"                                                                           >> tempUpgradeDBScript.sql
echo "ALTER DATABASE [$DB_NAME] SET ANSI_PADDING ON"                                >> tempUpgradeDBScript.sql
echo "GO"                                                                           >> tempUpgradeDBScript.sql
echo "ALTER DATABASE [$DB_NAME] SET ANSI_WARNINGS ON"                               >> tempUpgradeDBScript.sql
echo "GO"                                                                           >> tempUpgradeDBScript.sql
echo "ALTER DATABASE [$DB_NAME] SET ARITHABORT ON"                                  >> tempUpgradeDBScript.sql
echo "GO"                                                                           >> tempUpgradeDBScript.sql
echo "ALTER DATABASE [$DB_NAME] SET CONCAT_NULL_YIELDS_NULL ON"                     >> tempUpgradeDBScript.sql
echo "GO"                                                                           >> tempUpgradeDBScript.sql
echo "ALTER DATABASE [$DB_NAME] SET QUOTED_IDENTIFIER ON"                           >> tempUpgradeDBScript.sql
echo "GO"                                                                           >> tempUpgradeDBScript.sql
echo "UPDATE tInternal SET value = '$NEW_DB_VERSION_draft' WHERE [key] = 'version'" >> tempUpgradeDBScript.sql
echo "GO"                                                                           >> tempUpgradeDBScript.sql
cat TestExplorerDB-Upgrade.sql                                                      >> tempUpgradeDBScript.sql
echo "-- end of Upgrade script"                                                     >> tempUpgradeDBScript.sql
echo "GO"                                                                           >> tempUpgradeDBScript.sql
echo "UPDATE tInternal SET value = '$NEW_DB_VERSION' WHERE [key] = 'version'"       >> tempUpgradeDBScript.sql
echo "GO"                                                                           >> tempUpgradeDBScript.sql

echo "Upgrade Completed. Check the '$LOG_FILE_LOCATION' for details."

