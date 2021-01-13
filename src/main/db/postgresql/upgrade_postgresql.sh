#!/usr/bin/env bash

OLD_DB_VERSION=4.0.6
NEW_DB_VERSION=4.0.7
NEEDS_UPGRADE=false

# Environment variables to use if non-default DB environment is used. Below are listed default values
#   PGHOST=localhost
#   PGPORT=5432
#   PGUSER=postgres - 'postgres' admin DB user is used to create the new DB and create regular DB user

# set host to connect to
if [ -z "$PGHOST" ];
then
    PGHOST=localhost
else
    echo PGHOST environment variable is defined with value: $PGHOST
fi

# set port to connect to
if [ -z "$PGPORT" ];
then
    PGPORT=5432
else
    echo PGPORT environment variable is defined with value: $PGPORT
fi

# save the starting folder location
START_FOLDER="$PWD"

# navigate to the upgrade file directory
cd `dirname $0`


# delete previous tmpUpgradeDbScript.sql if one exists
rm -rf tmpUpgradeDbScript.sql
touch tmpUpgradeDbScript.sql

# delete previous upgrade.log if one exists
rm -rf upgrade.log
touch upgrade.log

FIRST_CMD_ARGUMENT="$1"
SECOND_CMD_ARGUMENT="$2"

if [[ "$FIRST_CMD_ARGUMENT" == "/?" || "$FIRST_CMD_ARGUMENT" == "--help" ]] ;
then
    echo "Please specify the database name as first parameter and admin DB user (postgres) password as second parameter for silent upgrade"
    echo "Alternatively admin DB password could be defined in exported PGPASSWORD variable in current shell session"
    exit
fi

# change password if needed
if [ ! -z "$SECOND_CMD_ARGUMENT" ];
then
  export PGPASSWORD="$SECOND_CMD_ARGUMENT"
fi

# password could have been provided externally from env
if [ -z "$PGPASSWORD" ];
then
  # reads silently the value without echo to the terminal
  read -sp 'Enter admin DB (postgres) password and press enter (input is hidden): ' PGPASSWORD
  export PGPASSWORD
  # new line
  echo ' '
fi

if [ -z "$PGPASSWORD" ];
then
    echo "PGPASSWORD env variable not found/set. Aborting upgrade"
    # TODO: optionally check for ~/.pgpass but complex parsing is needed to check if there is line for desired host:user
    exit 1
fi

DATABASE_EXISTS=0
until [ "$DATABASE_EXISTS" == 1 ]; do

  if [ -z "$FIRST_CMD_ARGUMENT" ];
  then
      read -p 'Enter Database name: ' DB_NAME
  else
      DB_NAME="$FIRST_CMD_ARGUMENT"
  fi
  # see if database exists
  # DATABASE_EXISTS=`psql -U AtsUser -h localhost -l | grep $DB_NAME | wc -l`
  DATABASE_EXISTS=`psql -U postgres -l | grep $DB_NAME | wc -l`

  if [ "$DATABASE_EXISTS" == 0 ];
  then
    if [ ! -z "$FIRST_CMD_ARGUMENT" ];
    then
        echo "No database exist with the given name. Now will exit"
        exit 2
    else
        echo "No database exist with the given name. Please select another name"
    fi
  fi
done

# get database version and change NEEDS_UPGRADE flag if needed
# DB_VERSION=`psql -U AtsUser -h localhost -d $DB_NAME -t -c "SELECT \"value\" FROM \"tInternal\" WHERE \"key\" = 'version'" | xargs` # | xargs is used to trim the db version string
DB_VERSION=`psql -U postgres -d $DB_NAME -t -c "SELECT \"value\" FROM \"tInternal\" WHERE \"key\" = 'version'" | xargs` # | xargs is used to trim the db version string

if [ "$DB_VERSION" = "$OLD_DB_VERSION"  ];
then
    NEEDS_UPGRADE=true
else
    NEEDS_UPGRADE=false
fi

if [ "$NEEDS_UPGRADE" = true ]; then
  echo "UPGRADING \"$DB_NAME\" from version \"$DB_VERSION\" to \"$NEW_DB_VERSION\""
  echo "\connect $DB_NAME" >> tmpUpgradeDbScript.sql
  echo " " >> tmpUpgradeDbScript.sql
  echo "UPDATE \"tInternal\" SET value = '${NEW_DB_VERSION}_draft' WHERE key = 'version';" >> tmpUpgradeDbScript.sql
  echo " " >> tmpUpgradeDbScript.sql
  cat TestExplorerDb_PostgreSQL_Upgrade.sql >> tmpUpgradeDbScript.sql
  psql -U postgres -a -f tmpUpgradeDbScript.sql | grep 'ERROR:\|WARNING:' > upgrade.log
  NUM_OF_ERRORS=`cat upgrade.log | grep 'ERROR:' | wc -l`
  if [[ "$NUM_OF_ERRORS" == 0 ]];
  then
      psql -U postgres -d $DB_NAME -t -c "UPDATE \"tInternal\" SET value = '$NEW_DB_VERSION' WHERE key = 'version'"
  else
      echo "Errors during upgrade: $NUM_OF_ERRORS. See upgrade.log file for errors"
      if [ ! -z "$FIRST_CMD_ARGUMENT" ];
      then
          exit 3
      fi
  fi

  echo "Upgrade of \"$DB_NAME\" completed. See upgrade.log file for potential errors"
  if [ ! -z "$FIRST_CMD_ARGUMENT" ];
  then
      exit 0
  fi
  # back to the starting folder location
  cd $START_FOLDER

else
    echo "Could not upgrade \"$DB_NAME\" from \"$DB_VERSION\" to \"$NEW_DB_VERSION\""
    echo "Only incremental upgrades (of adjacent versions) are supported and expected current version $OLD_DB_VERSION is not detected"
    if [ ! -z "$FIRST_CMD_ARGUMENT" ];
    then
        exit 4
    fi
fi
