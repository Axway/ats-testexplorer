@echo off
@setlocal enabledelayedexpansion enableextensions

:: save the starting folder location
set START_FOLDER=%cd%

set NEW_DB_VERSION=4.0.4
set CURRENT_DB_VERSION=4.0.3

:: navigate to the upgrade file directory
cd  /d "%~dp0"

echo Upgrading TestExplorer Database from version %CURRENT_DB_VERSION% to %NEW_DB_VERSION%

echo Current upgrade could take more time especially on large databases. Please do not close console before success/fail message is displayed

echo It is recommended that you backup your database before continue!

rem get the command line parameter if there is such
set CMD_ARGUMENT=%~1

set HELP=false
IF [%CMD_ARGUMENT%]==[--help] set HELP=true
IF [%CMD_ARGUMENT%]==[/?] set HELP=true
IF "%HELP%" == "true" (
    echo Please specify the database name as a parameter for silent upgrade
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
IF %SILENT_MODE_USED% == true (
	set DB_NAME=%CMD_ARGUMENT%
) ELSE (
	set /p DB_NAME=Enter Database Name to upgrade:
)

REM check if there is database with this name and write the result to file
osql /E /d master -Q"SET NOCOUNT ON;SELECT name FROM master.dbo.sysdatabases where name='%DB_NAME%'" -h-1 /o db_list.txt

FindStr %DB_NAME% db_list.txt > NUL

IF %ERRORLEVEL% NEQ 0 (
	IF %SILENT_MODE_USED% == true (
		echo Such database does not exists. Upgrade abort
		del /f /q db_list.txt
		exit 1
	) ELSE (
		echo Such database does not exists. Please choose another name
		GOTO :set_dbname
	)
)

rem delete the db_list.txt file
del /f /q db_list.txt

:: ##################   CHECK DB VERSION    ##################################
type nul > tempCheckVersion.sql

echo DECLARE @newVersion varchar(20) = '%NEW_DB_VERSION%';                                              >> tempCheckVersion.sql
echo DECLARE @currentVersion varchar(20) = '%CURRENT_DB_VERSION%';                                      >> tempCheckVersion.sql
echo DECLARE @version varchar(20);                                                                      >> tempCheckVersion.sql
echo SELECT @version = value FROM tInternal WHERE [key]='version';                                                            >> tempCheckVersion.sql
echo PRINT 'Current DB version is ' + @version;                                                         >> tempCheckVersion.sql
echo IF @version = @newVersion                                                                          >> tempCheckVersion.sql
echo     RAISERROR (N'There is no need to upgrade. The current DB version is %%s.', 5, 1, @version);    >> tempCheckVersion.sql
echo ELSE IF @version ^<^> @currentVersion                                                              >> tempCheckVersion.sql
echo     RAISERROR (N'This script upgrades only databases with version %%s. If you have older version you should run all previous intermediate upgrade scripts.', 5, 1, @currentVersion);   >> tempCheckVersion.sql
echo ELSE                                                                                               >> tempCheckVersion.sql
echo     PRINT 'Upgrading to version: ' + @newVersion;                                                  >> tempCheckVersion.sql
echo GO                                                                                                 >> tempCheckVersion.sql

sqlcmd /E /b /V 5 /d "%DB_NAME%" /i tempCheckVersion.sql
IF %ERRORLEVEL% NEQ 0 goto stopUpgrade



:: ##################   UPGRADE SQL SCRIPT GENERATION   #####################
del /f /q tempCheckVersion.sql
type nul > tempUpgradeDBScript.sql

echo use [%DB_NAME%]                                                          >> tempUpgradeDBScript.sql
echo GO                                                                         >> tempUpgradeDBScript.sql
echo PRINT GETDATE()                                                            >> tempUpgradeDBScript.sql
echo GO                                                                         >> tempUpgradeDBScript.sql
echo ALTER DATABASE [%DB_NAME%] SET ANSI_NULLS ON                             >> tempUpgradeDBScript.sql
echo GO                                                                         >> tempUpgradeDBScript.sql
echo ALTER DATABASE [%DB_NAME%] SET ANSI_PADDING ON                           >> tempUpgradeDBScript.sql
echo GO                                                                         >> tempUpgradeDBScript.sql
echo ALTER DATABASE [%DB_NAME%] SET ANSI_WARNINGS ON                          >> tempUpgradeDBScript.sql
echo GO                                                                         >> tempUpgradeDBScript.sql
echo ALTER DATABASE [%DB_NAME%] SET ARITHABORT ON                             >> tempUpgradeDBScript.sql
echo GO                                                                         >> tempUpgradeDBScript.sql
echo ALTER DATABASE [%DB_NAME%] SET CONCAT_NULL_YIELDS_NULL ON                >> tempUpgradeDBScript.sql
echo GO                                                                         >> tempUpgradeDBScript.sql
echo ALTER DATABASE [%DB_NAME%] SET QUOTED_IDENTIFIER ON                      >> tempUpgradeDBScript.sql
echo GO                                                                         >> tempUpgradeDBScript.sql
echo UPDATE tInternal SET value = '%NEW_DB_VERSION%_draft' WHERE [key] = 'version' >> tempUpgradeDBScript.sql
echo GO                                                                            >> tempUpgradeDBScript.sql
type TestExplorerDB-Upgrade.sql                                                    >> tempUpgradeDBScript.sql
echo -- end of Upgrade script                                                      >> tempUpgradeDBScript.sql
echo GO                                                                            >> tempUpgradeDBScript.sql
echo UPDATE tInternal SET value = '%NEW_DB_VERSION%' WHERE [key] = 'version'       >> tempUpgradeDBScript.sql
echo GO                                                                            >> tempUpgradeDBScript.sql

osql /b /E /d master /i tempUpgradeDBScript.sql /o upgrade.log
IF %ERRORLEVEL% NEQ 0 goto upgradeFailed



:: ##################   UPGRADE SUCCESS     ################################
del /f /q tempUpgradeDBScript.sql
echo Upgrade Completed. Check the 'upgrade.log' for details.
GOTO :End


:: ##################   UPGRADE FAILED  ####################################
:upgradeFailed
rem del /f /q tempUpgradeDBScript.sql
echo ERROR - upgrade failed. Check the 'upgrade.log' file for the errors.
IF %SILENT_MODE_USED% == true (
	exit 2
) ELSE (
	GOTO :End
)

:: ##################   STOPING UPGRADE PROCEDURE   ########################
:stopUpgrade
del /f /q tempCheckVersion.sql
echo Upgrade aborted. No changes are made to the database.
IF %SILENT_MODE_USED% == true (
	exit 3
) ELSE (
	GOTO :End
)

:: ##################    THE END    ########################################
:End
echo Upgrade completed. Check upgrade.log file for potential errors.
IF %MANUAL_MODE_USED% == true (
	pause
	exit
) ELSE IF %CONSOLE_MODE_USED% == true (
	rem return to the start folder
	cd /d %START_FOLDER%
) ELSE (
	exit 0
)
