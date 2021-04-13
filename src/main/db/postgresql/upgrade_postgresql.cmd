@echo off
:: save the starting folder location
set START_FOLDER=%cd%

:: navigate to the upgrade file directory
cd  /d "%~dp0"

:: Note the trailing space at the beginning
set OLD_DB_VERSION=4.0.7
set NEW_DB_VERSION=4.0.8

:: delete previous tmpUpgradeDbScript.sql if one exists
del /f /q tmpUpgradeDbScript.sql
type nul > tmpUpgradeDbScript.sql
:: delete previous upgrade.log if one exists
del /f /q upgrade.log
type nul > upgrade.log

set BATCH_MODE=0
set INTERACTIVE_MODE=1
set MODE=%INTERACTIVE_MODE%
rem set host to connect to
IF [%PGHOST%]==[] (
    set PGHOST=localhost
) ELSE (
    echo "PGHOST environment variable is defined with value: %PGHOST%"
)

rem set port to connect to
IF [%PGPORT%]==[] (
    set PGPORT=5432
) ELSE (
    echo "PGPORT environment variable is defined with value: %PGPORT%"
)
rem set the name of the database to upgrade
IF [%PGDATABASE%] NEQ [] (
    echo "PGDATABASE environment variable is defined with value: %PGDATABASE%"
	set MODE=%BATCH_MODE%
)

rem set the name of the mssql user
IF [%PGUSER%] NEQ [] (
    echo "PGUSER environment variable is defined with value: %PGUSER%"
)

IF [%PGPASSWORD%] NEQ [] (
    echo "PGPASSWORD environment variable is defined and will be used"
)
rem set the name of the mssql user to be created
IF [%PSQL_USER_NAME%]==[] (
    set PSQL_USER_NAME="AtsUser"
) ELSE (
    echo "PSQL_USER_NAME environment variable is defined with value: %PSQL_USER_NAME%"
)

rem set port to connect to
IF [%PSQL_USER_PASSWORD%]==[] (
    set PSQL_USER_PASSWORD="AtsPassword"
) ELSE (
    echo "PSQL_USER_PASSWORD environment variable is defined and will be used"
)


:: check if the script is executed manually
set CONSOLE_MODE_USED=true
echo %cmdcmdline% | find /i "%~0" >nul
if not errorlevel 1 set CONSOLE_MODE_USED=false


set HELP=false
:GETOPTS
IF "%1" == "-H" ( set PGHOST=%2& shift
)ELSE IF "%1" == "-p" ( set PGPORT=%2& shift
)ELSE IF "%1" == "-d" ( set PGDATABASE=%2& set MODE=%BATCH_MODE%& shift
)ELSE IF "%1" == "-U" ( set PGUSER=%2& shift
)ELSE IF "%1" == "--help" ( set HELP="true"
)ELSE IF "%1" == "-S" ( set PGPASSWORD=%2& shift
)ELSE IF "%1" == "-u" ( set PSQL_USER_NAME=%2& shift
)ELSE IF "%1" == "-s" ( set PSQL_USER_PASSWORD=%2& shift
)ELSE ( set HELP="true" & if "%2%" ==! "" & shift )
shift
IF NOT "%1" == "" (
goto GETOPTS
)

IF "%HELP%" == "true" (
    echo "The usage is ./upgrade_postgresql.cmd [OPTION]...[VALUE]...
   The following script upgrades an ATS Logging DB from version %OLD_DB_VERSION% to current version %NEW_DB_VERSION%"
     echo "Available options
   -H <target_SQL_server_host>, default is: localhost,Might be specified by env variable: PGHOST
   -p <target_SQL_server_port>, default is: 5432, Might be specified by env variable: PGPORT
   -d <target_SQL_database_name>, default: no. Required for non-interactive - batch mode. Might be specified by env variable: PGDATABASE
   -u <target_SQL_user_name>, default is: AtsUser,Might be specified by env variable: PSQL_USER_NAME
   -s <target_SQL_user_password>, Might be specified by env variable: PSQL_USER_PASSWORD
   -U <target_SQL_admin_name>,default: no; Required for non-interactive - batch mode. Might be specified by env variable: PGUSER
   -S <target_SQL_admin_password>, default: no; Required for non-interactive - batch mode. Might be specified by env variable: PGPASSWORD"

)


rem fill in required parameters that has not been previously stated
IF  "%MODE%" == "%INTERACTIVE_MODE%" (

 IF [%PGUSER%]==[] (
     SET /P PGUSER=Enter POSTGRE sever admin name:
     )

     IF [%PGPASSWORD%]==[] (
       SET /P PGPASSWORD=Enter POSTGRE sever admin password:
     )

     IF [%PGDATABASE%]==[] (
     :set_dbname
       SET /P PGDATABASE=Enter Test Explorer database name:
      )
   )
)

:: see if database exists
psql.exe -U %PGUSER% -h %PGHOST% -p %PGPORT% -l > db_list.txt
IF %ERRORLEVEL% NEQ 0 (
	echo There was problem getting checking for database existence
	echo Check if the provided password is correct
	IF "%MODE%" == "%BATCH_MODE%" (
		del /f /q db_list.txt
		exit 1
	) ELSE (
		GOTO :end
	)
)

findstr /m %PGDATABASE% db_list.txt
IF %ERRORLEVEL% NEQ 0 (
	IF "%MODE%" == "%BATCH_MODE%" (
		del /f /q db_list.txt
		echo "Database %PGDATABASE% does not exist. Now will exit"
		exit 2
	) ELSE (
		echo "Database %PGDATABASE% does not exist. Please choose another name"
		GOTO :set_dbname
	)
)
del /f /q db_list.txt

psql.exe -h %PGHOST% -p %PGPORT% -U %PGUSER% -d %PGDATABASE% -t -c "SELECT \"value\" FROM \"tInternal\" WHERE \"key\" = 'version'" >> db_version.txt
set /p DB_VERSION=<db_version.txt
del /f /q db_version.txt
IF %DB_VERSION%==%OLD_DB_VERSION% (
	echo "Upgrading %PGDATABASE% from %DB_VERSION% to %NEW_DB_VERSION%"
	echo \connect %PGDATABASE% >> tmpUpgradeDbScript.sql
	type nul  >> tmpUpgradeDbScript.sql
	type nul  >> tmpUpgradeDbScript.sql
	echo UPDATE "tInternal" SET value = '%NEW_DB_VERSION%_draft' WHERE key = 'version'; >> tmpUpgradeDbScript.sql
	type nul  >> tmpUpgradeDbScript.sql
	type nul  >> tmpUpgradeDbScript.sql
	type TestExplorerDb_PostgreSQL_Upgrade.sql >> tmpUpgradeDbScript.sql
	psql.exe -U postgres -a -f tmpUpgradeDbScript.sql | findstr "ERROR: WARNING:" > upgrade.log
) ELSE (
	echo "Could not upgrade %PGDATABASE% from %DB_VERSION% to %NEW_DB_VERSION%"
	IF "%MODE%" == "%BATCH_MODE%"  (
		 exit 3
	) ELSE (
		GOTO :end
	)
)

(type upgrade.log | find /c "ERROR:")>>num_of_errors.txt
set /p NUM_OF_ERRORS=<num_of_errors.txt
del num_of_errors.txt
IF %NUM_OF_ERRORS%==0 (
	psql.exe -h %PGHOST% -p %PGPORT% -U %PGUSER% -d %PGDATABASE% -t -c "UPDATE \"tInternal\" SET value = '%NEW_DB_VERSION%' WHERE key = 'version'"
) ELSE (
	echo "Errors during upgrade: %NUM_OF_ERRORS%. See upgrade.log file for errors"
	IF "%MODE%" == "%BATCH_MODE%" (
		 exit 4
	) ELSE (
		GOTO :end
	)
)

:end
IF "%CONSOLE_MODE_USED%" == "true" (
rem return to the start folder
cd /d %START_FOLDER%
) ELSE IF "%MODE%" == "%INTERACTIVE_MODE%"  (
	pause
	exit
 ) ELSE (
	exit 0
)

