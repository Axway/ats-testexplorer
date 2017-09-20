@echo off

:: Note the trailing space at the beginning
set OLD_DB_VERSION=4.0.2
set NEW_DB_VERSION=4.0.3

set /p DB_NAME=Enter Database name:

:: delete previous tmpUpgradeDbScript.sql if one exists
del /f /q tmpUpgradeDbScript.sql
type nul > tmpUpgradeDbScript.sql
:: delete previous upgrade.log if one exists
del /f /q upgrade.log
type nul > upgrade.log

:: see if database exists
psql.exe -U postgres -l > db_list.txt 2>&1
findstr /m %DB_NAME% db_list.txt

if %errorlevel%==0 (
	echo Database "%DB_NAME%" does exist.
) else (
	echo Database "%DB_NAME%" does not exist.
	exit
)

psql.exe -U postgres -d %DB_NAME% -t -c "SELECT \"value\" FROM \"tInternal\" WHERE \"key\" = 'version'" >> db_version.txt
set /p DB_VERSION=<db_version.txt
del /f /q db_version.txt
if %DB_VERSION%==%OLD_DB_VERSION% (
	echo "Upgrading %DB_NAME% from %DB_VERSION% to %NEW_DB_VERSION%"
	echo "UPGRADING \"$DB_NAME\" from version \"$DB_VERSION\" to \"$NEW_DB_VERSION\""
	echo \connect %DB_NAME% >> tmpUpgradeDbScript.sql
	type nul  >> tmpUpgradeDbScript.sql
	type TestExplorerDb_PostgreSQL_Upgrade.sql >> tmpUpgradeDbScript.sql
	psql.exe -U postgres -a -f tmpUpgradeDbScript.sql 2>&1 | findstr WARNING > upgrade.log
	echo "Upgrading of \"$DB_NAME\" completed. See upgrade.log file for errors"
) else (
	echo "Could not upgrade %DB_NAME% from %DB_VERSION% to %NEW_DB_VERSION%"
)

del /f /q db_list.txt

pause
@echo on
exit