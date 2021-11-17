#!/usr/bin/env bash

OLD_DB_VERSION=4.0.9
NEW_DB_VERSION=4.0.10
NEEDS_UPGRADE=false

INTERACTIVE_MODE=0
BATCH_MODE=1
MODE=$INTERACTIVE_MODE

function print_help() {
  echo "The usage is ${0} [OPTION] [VALUE] ...
The following script upgrades an ATS Logging DB from version $OLD_DB_VERSION to current version $NEW_DB_VERSION"
  echo "Available options
  -H <target_SQL_server_host>, default is: localhost; Might be specified by env variable: PGHOST
  -p <target_SQL_server_port>, default is: 5432; Might be specified by env variable: PGPORT
  -d <target_SQL_database_name>; default: no. Required for non-interactive (batch mode). Might be specified by env variable: PGDATABASE
  -U <target_SQL_admin_name>, default: use current OS account;  Might be specified by env variable: PGUSER
  -S <target_SQL_admin_password>, default: no; Might be specified by env variable: PGPASSWORD
  -u <target_SQL_user_name>, default is: AtsUser; Might be specified by env variable: PSQL_USER_NAME
  -s <target_SQL_user_password>; Might be specified by env variable: PSQL_USER_PASSWORD"
}

# save the starting folder location
START_FOLDER="$PWD"

# navigate to the install file directory
cd $(dirname $0)

# delete previous tmpUpgradeDbScript.sql if one exists
rm -rf tmpUpgradeDbScript.sql
touch tmpUpgradeDbScript.sql

# delete previous upgrade.log if one exists
rm -rf upgrade.log
touch upgrade.log

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
  echo "PGPASSWORD environment variable is defined and will be with be used"
fi

export PGPASSWORD=$PGPASSWORD

if [ -z "$PSQL_USER_NAME" ]; then
  PSQL_USER_NAME="AtsUser"
else
  echo "PSQL_USER_NAME environment variable is defined with the value: $PSQL_USER_NAME"
fi

if [ -z "$PSQL_USER_PASSWORD" ]; then
  PSQL_USER_PASSWORD="AtsPassword"
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
  echo "Admin user credentials need to be provided in order to create new database"
  exit 2
fi

# password could have been provided externally from env
# if interactive mode
#if [ -z "$PGPASSWORD" ];
#then
#  # reads silently the value without echo to the terminal
#  read -sp 'Enter admin DB (postgres) password and press enter (input is hidden): ' PGPASSWORD
#  export PGPASSWORD
#  # new line
#  echo ' '
#fi

if [ -z "$PGPASSWORD" ];
then
    echo "Neither PGPASSWORD env variable nor -S option is set. Aborting upgrade"
    # TODO: optionally check for ~/.pgpass but complex parsing is needed to check if there is line for desired host:user
    exit 3
fi    


function check_db_existence() {
  # return the number of DBs with provided name.
  PGDATABASE="$1"
  # see if database exists.
  # Make sure PGPASSWORD is already set
  DBS_OUTPUT=$(psql -h $PGHOST -p $PGPORT -U $PGUSER -l)
  if [ $? != 0 ]; then
      echo "List of installed databases could not be retrieved. Possible cause is wrong host, port parameter, DB admin user or password"
      echo "Use option \"-h\" for help"
      exit 6
  fi
  # 1st column of result is the table. Check with spaces around to prevent possible substring match
  DATABASE_EXISTS=$(echo "$DBS_OUTPUT" | grep -c --regexp="^ $PGDATABASE ")

#  if [ "$DATABASE_EXISTS" == 0 ]; then
#    if [ $MODE == $BATCH_MODE ]; then
#      echo "No database exists with the given name. Upgrade not possible. "
#      exit 3
#    else
#      echo "No database exists with the given name. Please select another name"
#    fi
#  fi
  return "$DATABASE_EXISTS"
}

DATABASE_EXISTS=0
until [ "$DATABASE_EXISTS" == 1 ]; do

  if [ "$MODE" == "$INTERACTIVE_MODE" ]; then
    read -r -p 'Enter Database name: ' PGDATABASE
  fi
  # see if database exists
  check_db_existence "$PGDATABASE"
  DATABASE_EXISTS=$?
  if [ "$DATABASE_EXISTS" == "0" ]; then
    if [ "$MODE" == "$BATCH_MODE" ]; then
      echo "Database named $PGDATABASE does not exist so it could not be upgraded. Installation will abort."
      exit 3
    else
      echo "Database named $PGDATABASE does not exist. Please choose existing DB or Ctrl+C to abort script execution."
    fi
  fi
done

# get database version and change NEEDS_UPGRADE flag if needed
DB_VERSION=$(psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" -t -c "SELECT \"value\" FROM \"tInternal\" WHERE \"key\" = 'version'" | xargs) # | xargs is used to trim the db version string

if [ "$DB_VERSION" = "$OLD_DB_VERSION" ]; then
  NEEDS_UPGRADE=true
else
  NEEDS_UPGRADE=false
fi

if [ "$NEEDS_UPGRADE" = true ]; then
  echo "UPGRADING \"$PGDATABASE\" from version \"$DB_VERSION\" to \"$NEW_DB_VERSION\""
  echo "\connect $PGDATABASE" >>tmpUpgradeDbScript.sql
  echo " " >>tmpUpgradeDbScript.sql
  echo "UPDATE \"tInternal\" SET value = '${NEW_DB_VERSION}_draft' WHERE key = 'version';" >>tmpUpgradeDbScript.sql
  echo " " >>tmpUpgradeDbScript.sql
  cat TestExplorerDb_PostgreSQL_Upgrade.sql >>tmpUpgradeDbScript.sql
  psql -U "$PGUSER" -h "$PGHOST" -p "$PGPORT" -a -f tmpUpgradeDbScript.sql >upgrade.log 2>&1

# grep 'ERROR:\|WARNING:'
  NUM_OF_ERRORS=$(cat upgrade.log | grep -ci --regex='ERROR:\|FATAL:')
  if [[ "$NUM_OF_ERRORS" == 0 ]]; then
    # check internal versions table
    psql -U "$PGUSER" -h "$PGHOST" -p $PGPORT -d "$PGDATABASE" -t -c "UPDATE \"tInternal\" SET value = '$NEW_DB_VERSION' WHERE key = 'version'"
  else
    echo "Errors found during install: $NUM_OF_ERRORS. Check upgrade.log file for details"
    if [ $MODE == $BATCH_MODE ]; then
      exit 4
    fi
  fi

  echo "Installation of database \"$PGDATABASE\" completed."
  if [ $MODE == $BATCH_MODE ]; then
    exit 0
  fi
  # back to the starting folder location
  cd "$START_FOLDER" || {
    echo "Failed to navigate back to last working directory"
    exit 5
  }

else
  echo "Could not upgrade \"$PGDATABASE\" from \"$DB_VERSION\" to \"$NEW_DB_VERSION\""
  if [ $MODE == $BATCH_MODE ]; then
    exit 6
  fi
fi
