@echo off

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

rem get the command line arguments, if there is such
set FIRST_CMD_ARGUMENT=%~1
set SECOND_CMD_ARGUMENT=%~2

set HELP=false
IF [%FIRST_CMD_ARGUMENT%]==[--help] set HELP=true
IF [%FIRST_CMD_ARGUMENT%]==[/?] set HELP=true
IF "%HELP%" == "true" (
    echo Please specify the database name as first parameter and database password as second parameter for silent install
    GOTO :end
)

:: check if the script is executed manually
set INTERACTIVE=0
echo %cmdcmdline% | find /i "%~0" >nul
if not errorlevel 1 set INTERACTIVE=1

IF %INTERACTIVE% == 0 (
	SET CONSOLE_MODE_USED=true
) ELSE (
	IF [%FIRST_CMD_ARGUMENT%]==[] (
		SET MANUAL_MODE_USED=true
	) ELSE (
		SET SILENT_MODE_USED=true
	)
)

:set_dbname
IF "%SILENT_MODE_USED%" == "true" (
	set DB_NAME=%FIRST_CMD_ARGUMENT%
) ELSE (
	set /p DB_NAME=Enter Database name:
)

rem set password
IF NOT [%SECOND_CMD_ARGUMENT%]==[] (
    set PGPASSWORD=%SECOND_CMD_ARGUMENT%
)

:: see if database exists
psql -U postgres -l > db_list.txt
IF %ERRORLEVEL% NEQ 0 (
	echo There was problem checking for database existence
	echo Check if the provided password is correct
	IF "%SILENT_MODE_USED%" == "true" (
		del /f /q db_list.txt
		exit 1
	) ELSE (
		GOTO :end
	)
)

findstr /m %DB_NAME% db_list.txt
IF %ERRORLEVEL%==0 (
	IF "%SILENT_MODE_USED%" == "true" (
		echo Such database already exists. Rerun the script with different name or drop the database. Installation aborted.
		del /f /q db_list.txt
		exit 2
	) ELSE (
		echo Database with the same name already exists. Please choose another name or drop the database.
		GOTO :set_dbname
	)
) else (
	echo Installing "%DB_NAME% ..."
	echo CREATE DATABASE "%DB_NAME%"; >> tmpInstallDbScript.sql
	echo. >> tmpInstallDbScript.sql
	echo \connect %DB_NAME% >> tmpInstallDbScript.sql
	type TestExplorerDB_PostgreSQL.sql >> tmpInstallDbScript.sql
	psql.exe -U postgres -a -f tmpInstallDbScript.sql | FINDSTR 'ERROR:' > install.log
	echo Installing of "%DB_NAME%" completed. See install.log file for errors
)
del /f /q db_list.txt

echo Installation completed. Check install.log file for potential errors.
:end
IF "%CONSOLE_MODE_USED%" == "true" (
	rem return to the start folder
	cd /d %START_FOLDER%
) ELSE IF "%MANUAL_MODE_USED%" == "true" (
	pause
	exit
) ELSE (
	exit 0
)
