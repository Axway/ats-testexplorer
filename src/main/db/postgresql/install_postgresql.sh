#!/usr/bin/env bash

DATABASE_NAME=""
SILENT_INSTALL=false
SCRIPT_LOCATION_DIRECTORY=`dirname $0`
TEMP_INSTALL_SCRIPTS_FILE="$SCRIPT_LOCATION_DIRECTORY/tmpInstallDbScript.sql"
INSTALL_LOG_FILE="$SCRIPT_LOCATION_DIRECTORY/install.log"


function print_help {
	echo "usage: $0 -n <DATABASE_NAME> -p <PGPASSWORD> -h print this text"
}


# process user input arguments
while getopts "n:p:h" option; do
	case $option in
		n)
			DATABASE_NAME=$OPTARG
			SILENT_INSTALL=true
		;;
		p)
			export PGPASSWORD=$OPTARG
		;;
		h)
			print_help
			exit 0
		;;
		\?)
			echo "Invallid option: -$option"
			print_help
			exit 1
		;;
	esac
done

if [ "$SILENT_INSTALL" == true ];
then
	# check if password is correct
	PASSWORD_CORRECT=`psql -U postgres -h localhost -w -c "SELECT 1" | grep 1 | wc -l`
	if [ "$PASSWORD_CORRECT" -ge 1 ];
	then
		echo "Password for postgres user is CORRECT!"
		DATABASE_EXISTS=`psql -U postgres -h localhost -l -w | grep $DATABASE_NAME | wc -l`
		if [ "$DATABASE_EXISTS" -ge "1" ];
		then
		    echo "Database with name \"$DATABASE_NAME\" already exists. Aborting silent install."
			exit 3
		fi
	else
		echo "Password for postgres user is NOT CORRECT or other error occurred!. Aborting silent install"
		exit 2
	fi
else
    # check if password is correct
	for i in 1 2 3
	do
	    read -p "Enter postgresql password [$i/3]: " PASSWORD
	    export PGPASSWORD=$PASSWORD
		PASSWORD_CORRECT=`psql -U postgres -h localhost -w -c "SELECT 1" | grep 1 | wc -l`
		if [ "$PASSWORD_CORRECT" -ge 1 ];
		then
			echo "Password for postgres user is CORRECT!"
			break
		else
			echo "Password for postgres user is NOT CORRECT or other error occurred!"
			export PGPASSWORD=""
		fi
	done
	if [ -z "$PGPASSWORD" ];
	then
		echo "Password authentication for user 'postgres' failed after 3 attempts. Aborting install"
		exit 4
	fi
	DATABASE_EXISTS=1
	while [ "$DATABASE_EXISTS" -ge "1" ]
	do
	    read -p "Enter postgresql database name: " DATABASE_NAME
		DATABASE_EXISTS=`psql -U postgres -h localhost -l -w | grep $DATABASE_NAME | wc -l`
		[ "$DATABASE_EXISTS" -ge "1" ] && echo "Database with name \"$DATABASE_NAME\" already exists"
	done
fi

echo "Begin installation of ATS LOG database \"$DATABASE_NAME\" ..."

[ -f $TEMP_INSTALL_SCRIPTS_FILE ] && rm $TEMP_INSTALL_SCRIPTS_FILE
touch $TEMP_INSTALL_SCRIPTS_FILE
if [ ! -f $TEMP_INSTALL_SCRIPTS_FILE ];
then
	echo "Could not create file \"$TEMP_INSTALL_SCRIPTS_FILE\" in directory [$SCRIPT_LOCATION_DIRECTORY]. Error code is: [$?]. Aborting install"
	exit 5
fi
[ -f $INSTALL_LOG_FILE ] && rm $INSTALL_LOG_FILE
touch $INSTALL_LOG_FILE
if [ ! -f $INSTALL_LOG_FILE ];
then
	echo "Could not create file \"$INSTALL_LOG_FILE\" in directory [$SCRIPT_LOCATION_DIRECTORY]. Error code is: [$?]. Aborting install"
	exit 6
fi

echo "CREATE DATABASE \"$DATABASE_NAME\";" >> $TEMP_INSTALL_SCRIPTS_FILE
echo " " >> $TEMP_INSTALL_SCRIPTS_FILE
echo "\connect $DATABASE_NAME" >> $TEMP_INSTALL_SCRIPTS_FILE
echo " " >> $TEMP_INSTALL_SCRIPTS_FILE
if [ ! -f "$SCRIPT_LOCATION_DIRECTORY/TestExplorerDb_PostgreSQL.sql" ];
then
	echo "File \"TestExplorerDb_PostgreSQL.sql\" does not exist in directory [$SCRIPT_LOCATION_DIRECTORY]. Aborting install"
	exit 7
fi
cat "$SCRIPT_LOCATION_DIRECTORY/TestExplorerDb_PostgreSQL.sql" >> $TEMP_INSTALL_SCRIPTS_FILE
if [ $? -gt 0 ];
then
	echo "Could not write install content to \"$TEMP_INSTALL_SCRIPTS_FILE\". Aborting install"
	exit 8
fi

psql -U postgres -h localhost -w -a -f $TEMP_INSTALL_SCRIPTS_FILE > $INSTALL_LOG_FILE
if [ $? -gt 0 ];
then
	echo "There were errors while installing database. See $INSTALL_LOG_FILE file for information"
	exit 9
else
	echo "Successfull installation of database \"$DATABASE_NAME\""
fi

