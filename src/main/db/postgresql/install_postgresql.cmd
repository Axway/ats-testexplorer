@echo off
rem Environment variables to use if non-default DB environment is used. Below are listed default values
rem set PGHOST=localhost
rem set PGPORT=5432
rem set PGUSER=postgres - 'postgres' admin DB user is used to create the new DB and create regular DB user

rem set host to connect to
set BATCH_MODE=0
set INTERACTIVE_MODE=1
set MODE=%INTERACTIVE_MODE%

IF [%PGHOST%]==[] (
    set PGHOST=localhost
) ELSE (
    echo PGHOST environment variable is defined with value: %PGHOST%
)

rem set port to connect to
IF [%PGPORT%]==[] (
    set PGPORT=5433
) ELSE (
    echo PGPORT environment variable is defined with value: %PGPORT%
)

IF [%PGDATABASE%] NEQ [] (
   echo PGDATABASE environment variable is defined with value: %PGDATABASE%
   	set MODE=%BATCH_MODE%
)

rem set port to connect to
IF [%PGUSER%]  NEQ [] (
  echo PGUSER environment variable is defined with value: %PGUSER%
)

IF [%PGPASSWORD%] NEQ [] (
    echo PGPASSWORD environment variable is defined with environment variable
)

rem set port to connect to
IF [%PSQL_USER_NAME%]==[] (
    set PSQL_USER_NAME=AtsUser
) ELSE (
    echo PSQL_USER_NAME environment variable is defined with value: %PSQL_USER_NAME%
)

IF [%PSQL_USER_PASSWORD%]==[] (
    set PSQL_USER_PASSWORD=AtsPassword
) ELSE (
    echo PSQL_USER_PASSWORD environment variable is defined with environment variable
)


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
    echo "The usage is ./install_postgresql.cmd [OPTION]...[VALUE]...
   The following script installs a ATS Logging DB to store test execution results. The version is 4.0.7"
     echo "Available options
   -H <target_SQL_server_host>, default is: localhost,Might be specified by env variable: PGHOST
   -p <target_SQL_server_port>, default is: 5433, Might be specified by env variable: PGPORT
   -d <target_SQL_database_name>, Might be specified by env variable: PGDATABASE
   -u <target_SQL_user_name>, default is: AtsUser,Might be specified by env variable: PSQL_USER_NAME
   -s <target_SQL_user_password>, default is: AtsPassword,Might be specified by env variable: PSQL_USER_PASSWORD
   -U <target_SQL_admin_name>,use current OS account Might be specified by env variable: PGUSER
   -S <target_SQL_admin_password>, use current OS account  Might be specified by env variable: PGPASSWORD"

)

:: save the starting folder location
set START_FOLDER=%cd%

:: navigate to the install file directory
cd  /d "%~dp0"

:: delete previous tmpInstallDbScript.sql if one exists
del /f /q tmpInstallDbScript.sql
type nul >tmpInstallDbScript.sql

:: delete previous install.log if one exists
del /f /q install.log
type nul >install.log

:: check if the script is executed manually
echo %cmdcmdline% | find /i "%~0" >nul
rem IF NOT ERRORLEVEL 1 set MODE=%INTERACTIVE_MODE%

echo %PGDATABASE%
:set_dbname
IF  %MODE% == %INTERACTIVE_MODE% (
	set /p PGDATABASE=Enter Database name:
)

:: see if database exists
psql -U %PGUSER% -h %PGHOST% -p %PGPORT% -l > db_list.txt
IF %ERRORLEVEL% NEQ 0 (
	echo There was problem checking for database existence
	echo Check if the provided postgres password, host and port are correct. In non-local mode pg_hba.conf should allow connect from current host.
	IF %MODE% == %BATCH_MODE% (
		del /f /q db_list.txt
	    exit 1
	) ELSE (
		GOTO :end
	)
)

findstr /m %PGDATABASE% db_list.txt
IF %ERRORLEVEL% == 0 (
	IF %MODE% == %BATCH_MODE% (
		echo Such database already exists. Rerun the script with different name or drop the database. Installation aborted.
		del /f /q db_list.txt
		 exit 2
	) ELSE (
		echo Database with the same name already exists. Please choose another name or drop the database.
		GOTO :set_dbname
	)
) else (
	echo Installing "%PGDATABASE% ..."
	echo CREATE DATABASE "%PGDATABASE%"; >> tmpInstallDbScript.sql
	echo. >> tmpInstallDbScript.sql
	echo \connect %PGDATABASE% >> tmpInstallDbScript.sql
	type TestExplorerDB_PostgreSQL.sql >> tmpInstallDbScript.sql
	set DBNAME=%PGDATABASE%
    set PGDATABASE=

	psql -h %PGHOST% -p %PGPORT% -U %PGUSER% -a -f tmpInstallDbScript.sql | FINDSTR 'ERROR:' > install.log

	echo Installing of "%PGDATABASE%" completed. See install.log file for errors
)
del /f /q db_list.txt
echo Installation completed. Check install.log file for potential errors.
set PGDATABASE=%DBNAME%
set PGPASSWORD=%PSQL_USER_PASSWORD%
psql -U %PSQL_USER_NAME% -h %PGHOST% -p %PGPORT% -d %PGDATABASE% -c "SELECT * FROM \"tInternal\";"


IF %ERRORLEVEL% NEQ 0 (
	echo Installation was not successful
	IF "%MODE%" == "%BATCH_MODE%"  (
	  exit 3
	) ELSE (
		GOTO :end
	)
)

:end
rem IF "%CONSOLE_MODE_USED%" == "true" (
rem	rem return to the start folder
rem	cd /d %START_FOLDER%
rem ) ELSE IF "%MANUAL_MODE_USED%" == "true" (
rem	pause
rem	exit
rem ) ELSE (
rem	exit 0
rem )
