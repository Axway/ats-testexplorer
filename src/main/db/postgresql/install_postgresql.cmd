@echo off

:: save the starting folder location
set START_FOLDER=%cd%

:: check if the script is executed manually
set INTERACTIVE=0
echo %cmdcmdline% | find /i "%~0" >nul
if not errorlevel 1 set INTERACTIVE=1

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
psql -U postgres -l > db_list.txt
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
IF %ERRORLEVEL%==0 (
	IF %INTERACTIVE% == 0 (
		rem remove the command parameter values
		set FIRST_CMD_ARGUMENT=

		echo Such database already exists. Please choose another name
		GOTO :set_dbname
	) ELSE (
		IF [%FIRST_CMD_ARGUMENT%]==[] (
			echo Such database already exists. Please choose another name
			GOTO :set_dbname
		) ELSE (
			echo Such database already exists. Now will exit
			del /f /q db_list.txt
			exit 2
		)
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
IF NOT %INTERACTIVE% == 0 (
	IF [%FIRST_CMD_ARGUMENT%]==[] (
		pause
	) ELSE (
		exit 0
	)
)

rem return to the start folder
cd /d %START_FOLDER%

