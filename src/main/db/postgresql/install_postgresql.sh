#!/usr/bin/env bash


function print_help() {
  echo "The usage is ${0} [OPTION] [VALUE] ...
The following script installs an ATS Logging DB for storing test execution results. The current version is 4.0.8"
  echo "Available options
  -H <target_SQL_server_host>, default is: localhost; Might be specified by env variable: PGHOST
  -p <target_SQL_server_port>, default is: 5432; Might be specified by env variable: PGPORT
  -d <target_SQL_database_name>; default: no. Required for non-interactive (batch mode). Might be specified by env variable: PGDATABASE
  -U <target_SQL_admin_name>, default: use current OS account;  Might be specified by env variable: PGUSER
  -S <target_SQL_admin_password>, default: no; Might be specified by env variable: PGPASSWORD
  -u <target_SQL_user_name>, default is: AtsUser; Might be specified by env variable: PSQL_USER_NAME
  -s <target_SQL_user_password>; Might be specified by env variable: PSQL_USER_PASSWORD"
}

function check_db_existence() {
  # return number of existing DBs with provided name;
  # PGDATABASE is read as first argument
  PGDATABASE="$1"

  # Make sure PGPASSWORD (of admin) is already set
  DBS_OUTPUT=$(psql -h $PGHOST -p $PGPORT -U $PGUSER -l)
  if [ $? != 0 ]; then
      echo "List of installed databases could not be retrieved. Possible cause is wrong host or port parameter, DB admin user or password"
      echo "Use option \"-h\" for help"
      exit 6
  fi
  DATABASE_EXISTS=$(echo "$DBS_OUTPUT" | grep -c --regexp="^ $PGDATABASE ")
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

# Setting defaults if not already set Postgres DB connection env. variables
if [ -z "$PGHOST" ]; then
  PGHOST=localhost
else
  echo "PGHOST environment variable is defined with the value: $PGHOST"
fi

if [ -z "$PGPORT" ]; then
  PGPORT=5432
else
  echo "PGPORT environment variable is defined with the value: $PGPORT"
fi

if [ -n "$PGDATABASE" ]; then
  echo "PGDATABASE environment variable is defined with the value: $PGDATABASE"
  MODE=$BATCH_MODE
fi

if [ -n "$PGUSER" ]; then
  echo "PGUSER environment variable is defined with the value: $PGUSER"
fi

if [ -n "$PGPASSWORD" ]; then
  echo "PGPASSWORD environment variable is defined and will be used"
  export PGPASSWORD
fi

if [ -z "$PSQL_USER_NAME" ]; then
  PSQL_USER_NAME=AtsUser
else
  echo "PSQL_USER_NAME environment variable is defined with the value: $PSQL_USER_NAME"
fi

if [ -z "$PSQL_USER_PASSWORD" ]; then
  PSQL_USER_PASSWORD=AtsPassword
else
  echo "PSQL_USER_PASSWORD environment variable is defined and will be used"
fi

while getopts ":H:p:d:u:s:U:S:h" option; do
  case $option in
  H)
    PGHOST=$OPTARG
    export PGHOST
    ;;
  p)
    PGPORT=$OPTARG
    export PGPORT
    ;;
  d)
    PGDATABASE=$OPTARG
    export PGDATABASE
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
    export PGUSER
    ;;
  S)
    PGPASSWORD=$OPTARG
    export PGPASSWORD
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

if [[ -z "$PGUSER" ]]; then
  echo "DB admin user credentials need to be provided in order to create a new database. Use option -h for help"
  exit 2
fi

# PGPASSWORD could have been provided and externally in env
#if interactive mode:
#if [ -z "$PGPASSWORD" ];
#then
#  # reads silently the value without echo to the terminal
#  read -sp 'Enter admin DB (postgres) password and press enter (input is hidden): ' PGPASSWORD
#  export PGPASSWORD
#  # new line
#  echo ' '
#fi


until [ "$DATABASE_EXISTS" == 0 ]; do

  if [ $MODE == $INTERACTIVE_MODE ]; then
    read -r -p 'Enter Database name: ' PGDATABASE
  fi

  # see if database exists
  check_db_existence "$PGDATABASE"
  DATABASE_EXISTS=$?
  if [ "$DATABASE_EXISTS" != 0 ]; then
    if [ "$MODE" == "$BATCH_MODE" ]; then
      echo "Database named $PGDATABASE already exists. Installation will abort."
      exit 3
    else
      echo "Database named $PGDATABASE already exists. Please choose another name."
    fi
  fi
done

echo "Installing \"$PGDATABASE ..."
{
echo "CREATE DATABASE \"$PGDATABASE\";"
echo " "
echo "\connect $PGDATABASE"
echo " "
cat TestExplorerDb_PostgreSQL.sql
}>>tmpInstallDbScript.sql

db=$PGDATABASE
#unset PGDATABASE and switch to system DB
PGDATABASE=postgres # default system database

sed -i "s/AtsUser/$PSQL_USER_NAME/g" tmpInstallDbScript.sql
sed -i "s/AtsPassword/$PSQL_USER_PASSWORD/g" tmpInstallDbScript.sql

psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -a -f tmpInstallDbScript.sql >install.log 2>&1
NUM_OF_ERRORS=$(grep -ci --regex='ERROR:\|FATAL:' install.log)

# Check for access and TE versions of installed DB
export PGDATABASE=$db
PGPASSWORD="$PSQL_USER_PASSWORD" psql -U "$PSQL_USER_NAME" -h "$PGHOST" -p "$PGPORT" -c 'SELECT * FROM "tInternal";'

if [[ "$NUM_OF_ERRORS" == 0 ]]; then
  echo "Installation of database \"$PGDATABASE\" completed. Logs are located in install.log file"
  if [ $MODE == $BATCH_MODE ]; then
    exit 0
  fi
else
  echo "Errors found during install: $NUM_OF_ERRORS"
  echo "Installation of database \"$PGDATABASE\" was not successful. Logs are located in install.log file"
  if [ $MODE == $BATCH_MODE ]; then
    exit 4
  fi
fi

# back to the starting folder location
cd "$START_FOLDER" || {
  echo "Failed to navigate back to the last working directory"
  exit 5
}
