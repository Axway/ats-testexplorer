@echo off

:: save the starting folder location
set START_FOLDER=%cd%

:: navigate to the upgrade file directory
cd  /d "%~dp0"

:: check if the script is executed manually
set INTERACTIVE=0
echo %cmdcmdline% | find /i "%~0" >nul
if not errorlevel 1 set INTERACTIVE=1

echo %INTERACTIVE%

:: Note the trailing space at the beginning
set OLD_DB_VERSION=4.0.3
set NEW_DB_VERSION=4.1.0

:: delete previous tmpUpgradeDbScript.sql if one exists
del /f /q tmpUpgradeDbScript.sql
type nul > tmpUpgradeDbScript.sql
:: delete previous upgrade.log if one exists
del /f /q upgrade.log
type nul > upgrade.log

set FIRST_CMD_ARGUMENT=%~1
set SECOND_CMD_ARGUMENT=%~2

set HELP=false
IF [%FIRST_CMD_ARGUMENT%]==[--help] set HELP=true
IF [%FIRST_CMD_ARGUMENT%]==[/?] set HELP=true
IF "%HELP%" == "true" (
    echo Add the database name as first parameter and database password as second parameter for silent upgrade
    pause
	exit
)

:set_dbname
IF [%FIRST_CMD_ARGUMENT%]==[] (
	set /p DB_NAME=Enter Database name:
) ELSE (
    set DB_NAME=%FIRST_CMD_ARGUMENT%
)

rem set password
IF NOT [%SECOND_CMD_ARGUMENT%]==[] (
    set PGPASSWORD=%SECOND_CMD_ARGUMENT%
)

:: see if database exists
psql.exe -U postgres -l > db_list.txt
IF %ERRORLEVEL% NEQ 0 (
	echo There was problem getting checking for database existence
	echo Check if the provided password is correct
	IF %INTERACTIVE% == 0 (
		GOTO :end
	) ELSE (
		IF [%FIRST_CMD_ARGUMENT%]==[] (
			pause 
			exit
		) ELSE (
			del /f /q db_list.txt
			exit 1
		)
	)
)

findstr /m %DB_NAME% db_list.txt
IF %ERRORLEVEL% NEQ 0 (
	IF %INTERACTIVE% == 0 (
		rem remove the command parameter values
		set FIRST_CMD_ARGUMENT=

		echo "Database %DB_NAME% does not exist. Please choose another name"
		GOTO :set_dbname
	) ELSE (
		IF [%FIRST_CMD_ARGUMENT%]==[] (
			echo "Database %DB_NAME% does not exist. Please choose another name"
			GOTO :set_dbname
		) ELSE (
			del /f /q db_list.txt
			echo "Database %DB_NAME% does not exist. Now will exit"
			exit 1
		)
	)
)
del /f /q db_list.txt

psql.exe -U postgres -d %DB_NAME% -t -c "SELECT \"value\" FROM \"tInternal\" WHERE \"key\" = 'version'" >> db_version.txt
set /p DB_VERSION=<db_version.txt
del /f /q db_version.txt
IF %DB_VERSION%==%OLD_DB_VERSION% (
	echo "Upgrading %DB_NAME% from %DB_VERSION% to %NEW_DB_VERSION%"
	echo \connect %DB_NAME% >> tmpUpgradeDbScript.sql
	type nul  >> tmpUpgradeDbScript.sql
	type nul  >> tmpUpgradeDbScript.sql
	echo UPDATE "tInternal" SET value = '%NEW_DB_VERSION%_draft' WHERE key = 'version'; >> tmpUpgradeDbScript.sql
	type nul  >> tmpUpgradeDbScript.sql
	type nul  >> tmpUpgradeDbScript.sql
	type TestExplorerDb_PostgreSQL_Upgrade.sql >> tmpUpgradeDbScript.sql
	psql.exe -U postgres -a -f tmpUpgradeDbScript.sql | findstr "ERROR: WARNING:" > upgrade.log
) ELSE (
	echo "Could not upgrade %DB_NAME% from %DB_VERSION% to %NEW_DB_VERSION%"
	IF %INTERACTIVE% == 1 (
		IF [%FIRST_CMD_ARGUMENT%]==[] (
			pause
			exit
		) ELSE (
			exit 2
		)
	) ELSE (
		GOTO :end
	)
)

(type upgrade.log | find /c "ERROR:")>>num_of_errors.txt
set /p NUM_OF_ERRORS=<num_of_errors.txt
del num_of_errors.txt
IF %NUM_OF_ERRORS%==0 (
	psql.exe -U postgres -d %DB_NAME% -t -c "UPDATE \"tInternal\" SET value = '%NEW_DB_VERSION%' WHERE key = 'version'"
) ELSE (
	echo "Errors during upgrade: %NUM_OF_ERRORS%. See upgrade.log file for errors"
	IF %INTERACTIVE% == 1 (
		IF [%FIRST_CMD_ARGUMENT%]==[] (
			pause
		) ELSE (
			exit 
		)
	)
)

echo "Upgrading of %DB_NAME% completed. See upgrade.log file for potential errors"
IF %INTERACTIVE% == 1 (
	echo value %FIRST_CMD_ARGUMENT%
	IF [%FIRST_CMD_ARGUMENT%]==[] (
		pause
	) ELSE (
		exit 0
	)
)

rem return to the start folder
:end
cd /d %START_FOLDER%

