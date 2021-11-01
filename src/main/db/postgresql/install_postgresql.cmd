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
    set PGPORT=5432
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
    echo PGPASSWORD environment variable is defined and will be used
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
    echo PSQL_USER_PASSWORD environment variable is defined and will be used
)

:: save the starting folder location
set START_FOLDER=%cd%

:: navigate to the install file directory
cd  /d "%~dp0"

:: check if the script is executed manually
set CONSOLE_MODE_USED=true
echo %cmdcmdline% | find /i "%~0" >nul
IF NOT ERRORLEVEL 1 set CONSOLE_MODE_USED=false

set HELP=false
:GETOPTS
IF "%1" == "-H" ( set PGHOST=%2& shift
)ELSE IF "%1" == "-p" ( set PGPORT=%2& shift
)ELSE IF "%1" == "-d" ( set PGDATABASE=%2& set MODE=%BATCH_MODE%& shift
)ELSE IF "%1" == "-U" ( set PGUSER=%2& shift
)ELSE IF "%1" == "--help" ( set HELP=true
)ELSE IF "%1" == "-S" ( set PGPASSWORD=%2& shift
)ELSE IF "%1" == "-u" ( set PSQL_USER_NAME=%2& shift
)ELSE IF "%1" == "-s" ( set PSQL_USER_PASSWORD=%2& shift
)ELSE ( set HELP=true & if "%2%" ==! "" & shift )
shift
IF NOT "%1" == "" (
goto GETOPTS
)

IF "%HELP%" == "true" (
   echo "The usage is ./install_postgresql.cmd [OPTION]...[VALUE]..."
   echo "The following script installs an ATS Logging DB to store test execution results. The current version is 4.0.9"
   echo "Available options:"
   echo "  -H <target_SQL_server_host>, default is: localhost. Might be specified by env variable: PGHOST"
   echo "  -p <target_SQL_server_port>, default is: 5432. Might be specified by env variable: PGPORT"
   echo "  -d <target_SQL_database_name>, default: no. Required for non-interactive - batch mode. Might be specified by env variable: PGDATABASE"
   echo "  -u <target_SQL_user_name>, default is: AtsUser. Might be specified by env variable: PSQL_USER_NAME"
   echo "  -s <target_SQL_user_password>. Might be specified by env variable: PSQL_USER_PASSWORD"
   echo "  -U <target_SQL_admin_name>,default: no. Required for non-interactive - batch mode. Might be specified by env variable: PGUSER"
   echo "  -S <target_SQL_admin_password>, default: no. Required for non-interactive - batch mode. Might be specified by env variable: PGPASSWORD"
   GOTO :end
)

:: delete previous tmpInstallDbScript.sql if one exists
IF EXIST tmpInstallDbScript.sql (
del /f /q tmpInstallDbScript.sql
)
type nul >tmpInstallDbScript.sql

:: delete previous install.log if one exists
IF EXIST install.log (
    del /f /q install.log
)
type nul >install.log

rem fill in required parameters that has not been previously stated
IF  %MODE% == %INTERACTIVE_MODE% (

IF [%PGUSER%]==[] (
  SET /P PGUSER=Enter PostgreSQL server admin name:
  )

  IF [%PGPASSWORD%]==[] (
    SET /P PGPASSWORD=Enter PostgreSQL server admin password:
  )

  IF [%PGDATABASE%]==[] (
  :set_dbname
   set /p PGDATABASE=Enter Database name:
   )

)

:: check if database exists
psql -U %PGUSER% -h %PGHOST% -p %PGPORT% -l > db_list.txt
IF %ERRORLEVEL% NEQ 0 (
	echo There was problem checking for database existence
	echo Check if the provided postgres password, host and port are correct. In non-local mode pg_hba.conf should allow connect from current host.
	IF "%MODE%" == "%BATCH_MODE%" (
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

powershell -command "(get-content tmpInstallDbScript.sql) -replace 'AtsUser', '%PSQL_USER_NAME%'  | Set-Content tmpInstallDbScript.sql"
powershell -command "(get-content tmpInstallDbScript.sql) -replace 'AtsPassword', '%PSQL_USER_PASSWORD%'  | Set-Content tmpInstallDbScript.sql"

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
IF "%CONSOLE_MODE_USED%" == "true" (
rem return to the start folder
cd /d %START_FOLDER%
) ELSE IF "%MODE%" == "%INTERACTIVE_MODE%"  (
	pause
	exit
 ) ELSE (
	exit 0
)
