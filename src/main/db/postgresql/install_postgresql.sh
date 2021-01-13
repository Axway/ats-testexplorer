#!/usr/bin/env bash
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

# navigate to the install file directory
cd `dirname $0`

# delete previous tmpInstallDbScript.sql if one exists
rm -rf tmpInstallDbScript.sql
touch tmpInstallDbScript.sql

# delete previous install.log if one exists
rm -rf install.log
touch install.log

CMD_ARGUMENT="$1"

if [[ "$CMD_ARGUMENT" == "/?" || "$CMD_ARGUMENT" == "--help" ]] ; then
  echo "Please specify the database name as first parameter and admin DB password as second parameter for silent install"
  exit
fi

# change password if needed
if [ ! -z "$2" ];
then
  export PGPASSWORD="$2"
else
  # reads silently the value without echo to the terminal
  read -sp 'Enter admin DB (postgres) password and press enter (input is hidden): ' PGPASSWORD
  # new line
  echo ' '
fi


DATABASE_EXISTS=1
until [ "$DATABASE_EXISTS" == 0 ]; do

  if [ -z "$CMD_ARGUMENT" ];
  then
    read -p 'Enter Database name: ' DB_NAME
  else
    DB_NAME="$CMD_ARGUMENT"
  fi

  # see if database exists
  # psql -U postgres -h localhost -l | grep $DB_NAME | wc -l`
  DATABASE_EXISTS=`psql -U postgres -l | grep $DB_NAME | wc -l`

  if [ "$DATABASE_EXISTS" != 0 ];
  then
    if [ ! -z "$CMD_ARGUMENT" ];
    then
      echo Database name "$CMD_ARGUMENT" already exist. Install abort
      exit 1
    else
      echo Database name "$CMD_ARGUMENT" already exist. Please choose another name
    fi
  fi
done


echo "Installing \"$DB_NAME ..."
echo "CREATE DATABASE \"$DB_NAME\";" >> tmpInstallDbScript.sql
echo " " >> tmpInstallDbScript.sql
echo "\connect $DB_NAME" >> tmpInstallDbScript.sql
echo " " >> tmpInstallDbScript.sql
cat TestExplorerDb_PostgreSQL.sql >> tmpInstallDbScript.sql

psql -U postgres -a -f tmpInstallDbScript.sql | grep 'ERROR:' > install.log
NUM_OF_ERRORS=`cat install.log | grep 'ERROR:' | wc -l`


if [[ "$NUM_OF_ERRORS" == 0 ]]; then
  echo "Installing of \"$DB_NAME\" completed. Logs are located in install.log file"
  if [ ! -z "$CMD_ARGUMENT" ];
  then
    exit 2
  fi
else
  echo "Errors during install: $NUM_OF_ERRORS"
  echo "Installing of \"$DB_NAME\" was not successful. Logs are located in install.log file"
  if [ ! -z "$CMD_ARGUMENT" ];
  then
    exit 0
  fi
fi

# back to the starting folder location
cd $START_FOLDER
