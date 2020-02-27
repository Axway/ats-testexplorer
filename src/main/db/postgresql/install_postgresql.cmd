@ECHO OFF
SETLOCAL

SET SCRIPT_NAME=%0
SET DATABASE_NAME=""
SET PGPASSWORD=""
SET SILENT_INSTALL=0
SET SCRIPT_LOCATION_DIRECTORY=%~dp0
SET TEMP_INSTALL_SCRIPTS_FILE=%SCRIPT_LOCATION_DIRECTORY%\tmpInstallDbScript.sql
SET INSTALL_LOG_FILE=%SCRIPT_LOCATION_DIRECTORY%\install.log

:args
	SET PARAM=%~1
	SET ARG=%~2
	IF "%PARAM%" EQU "" (
		GOTO :check_install_mode
	)
	IF "%PARAM%" EQU "-n" (
		SET DATABASE_NAME=%ARG%
		SHIFT
		SHIFT
	) ELSE IF "%PARAM%" EQU "-p" (
		SET PGPASSWORD=%ARG%
		SHIFT
		SHIFT
	) ELSE IF "%PARAM%" EQU "--help" (
		GOTO :print_help
	) ELSE (
		echo Wrong parameter %PARAM%
		GOTO :print_help
	)
GOTO :args


:check_install_mode
IF %DATABASE_NAME% NEQ "" (
    SET SILENT_INSTALL=1
)
IF %SILENT_INSTALL% EQU 1 (
	GOTO :check_password
) ELSE (
	SET /p DATABASE_NAME=Enter Database name:
	SET /p PGPASSWORD=Enter postgres password:
	GOTO :check_password
)

:check_password
	IF EXIST query.txt DEL /F query.txt
	IF EXIST query.txt (
		echo Could not create file query.txt. Installation aborted
		GOTO :EOF
	)
	psql -U postgres -h localhost -w -c "SELECT 1" > query.txt
	findstr /m "1" query.txt > nul
	IF %ERRORLEVEL% NEQ 0 (
		echo Error occurred while checking postgres login
		IF EXIST query.txt DEL /F query.txt
		GOTO :EOF
	) ELSE (
		GOTO :check_db_exists
	)
GOTO :EOF

:check_db_exists
	IF EXIST query.txt DEL /F query.txt
	IF EXIST query.txt (
		echo Could not create file query.txt. Installation aborted
		GOTO :EOF
	)
	psql -U postgres -h localhost -d %DATABASE_NAME% -w -c "SELECT !@##$%^&" 2> query.txt
	findstr /m "!@##$%^&" query.txt > nul
	IF %ERRORLEVEL% EQU 0 (
		echo Database with name "%DATABASE_NAME%%" already exists
		IF EXIST query.txt DEL /F query.txt
		GOTO :EOF
	) ELSE (
		IF EXIST query.txt DEL /F query.txt
		GOTO :install
	)
GOTO :EOF

:install
	echo Begin installation of PostgreSQL ATS Log database %DATABASE_NAME% ...
	IF EXIST %TEMP_INSTALL_SCRIPTS_FILE% DEL /F %TEMP_INSTALL_SCRIPTS_FILE%
	TYPE nul >%TEMP_INSTALL_SCRIPTS_FILE%
	IF NOT EXIST %TEMP_INSTALL_SCRIPTS_FILE% (
		echo Could not create file \"%TEMP_INSTALL_SCRIPTS_FILE%\" in directory [%SCRIPT_LOCATION_DIRECTORY%]. Error code is: [%ERRORLEVEL%]. Aborting install
		EXIT 1
	)
	IF EXIST %INSTALL_LOG_FILE% DEL /F %INSTALL_LOG_FILE%
	TYPE nul >%INSTALL_LOG_FILE%
	IF NOT EXIST %INSTALL_LOG_FILE% (
		echo Could not create file \"%INSTALL_LOG_FILE%\" in directory [%SCRIPT_LOCATION_DIRECTORY%]. Error code is: [%ERRORLEVEL%]. Aborting install
		EXIT 2
	)
	echo CREATE DATABASE "%DATABASE_NAME%"; >> %TEMP_INSTALL_SCRIPTS_FILE%
	echo. >> %TEMP_INSTALL_SCRIPTS_FILE%
	echo \connect %DATABASE_NAME% >> %TEMP_INSTALL_SCRIPTS_FILE%
	IF NOT EXIST %SCRIPT_LOCATION_DIRECTORY%\\TestExplorerDb_PostgreSQL.sql (
		echo File \"TestExplorerDb_PostgreSQL.sql\" does not exist in directory [%SCRIPT_LOCATION_DIRECTORY%]. Aborting install
		EXIT 3
	)
	TYPE TestExplorerDB_PostgreSQL.sql >> %TEMP_INSTALL_SCRIPTS_FILE%
	IF %ERRORLEVEL% NEQ 0 (
		echo "Could not write install content to \"%$TEMP_INSTALL_SCRIPTS_FILE%\". Aborting install"
		EXIT 4
	)
	psql -U postgres -h localhost -w -a -f %TEMP_INSTALL_SCRIPTS_FILE% > nul 2> %INSTALL_LOG_FILE%
	findstr /m "ERROR:" %INSTALL_LOG_FILE% > nul
	IF %ERRORLEVEL% EQU 0 (
		echo Install unsuccessfull! There were errors while installing the database. See [%INSTALL_LOG_FILE%] for more information.
		EXIT 5
	)
	findstr /m "FATAL:" %INSTALL_LOG_FILE% > nul
	IF %ERRORLEVEL% EQU 0 (
		echo Install unsuccessfull! There were errors while installing the database. See [%INSTALL_LOG_FILE%] for more information.
		EXIT 6
	)
	echo Installation completed successfully.
GOTO :EOF

:print_help
	echo usage: %SCRIPT_NAME% -n DATABASE_NAME (the database's name) -p PGPASSWORD (the postgres user's password) --help
GOTO:EOF
ENDLOCAL
