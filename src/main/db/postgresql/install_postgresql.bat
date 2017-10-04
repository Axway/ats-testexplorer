@echo off

set /p DB_NAME=Enter Database name:

:: delete previous tmpInstallDbScript.sql if one exists
del /f /q tmpInstallDbScript.sql
type nul >tmpInstallDbScript.sql

:: delete previous install.log if one exists
del /f /q install.log
type nul >install.log

:: see if database exists
psql -U postgres -l > db_list.txt 2>&1

findstr /m %DB_NAME% db_list.txt
if %errorlevel%==0 (
	echo Database "%DB_NAME%" already exists.
) else (
	echo Installing "%DB_NAME% ..."
	echo CREATE DATABASE "%DB_NAME%"; >> tmpInstallDbScript.sql
	echo. >> tmpInstallDbScript.sql
	echo \connect %DB_NAME% >> tmpInstallDbScript.sql
	type TestExplorerDB_PostgreSQL.sql >> tmpInstallDbScript.sql
	psql.exe -U postgres -a -f tmpInstallDbScript.sql 2>&1 | FINDSTR 'ERROR:' > install.log
	echo Installing of "%DB_NAME%" completed. See install.log file for errors
)
del /f /q db_list.txt

pause
@echo on
exit
