@echo off
@setlocal enabledelayedexpansion enableextensions

:: save the starting folder location

set START_FOLDER=%cd%

set NEW_DB_VERSION=4.0.10
set CURRENT_DB_VERSION=4.0.9

:: navigate to the upgrade file directory
cd  /d "%~dp0"

echo Upgrading TestExplorer Database from version %CURRENT_DB_VERSION% to %NEW_DB_VERSION%

echo Current upgrade could take more time especially on large databases. Please do not close console before success/fail message is displayed

echo It is recommended that you backup your database before continue!

set BATCH_MODE=0
set INTERACTIVE_MODE=1
set MODE=%INTERACTIVE_MODE%

rem set host to connect to
IF [%MSSQL_HOST%]==[] (
    set MSSQL_HOST=localhost
) ELSE (
    echo "MSSQL_HOST environment variable is defined with value: %MSSQL_HOST%"
)

rem set port to connect to
IF [%MSSQL_PORT%]==[] (
    set MSSQL_PORT=1433
) ELSE (
    echo "MSSQL_PORT environment variable is defined with value: %MSSQL_PORT%"
)
rem set the name of the database to upgrade
IF [%MSSQL_DATABASE%] NEQ [] (
    echo "MSSQL_DATABASE environment variable is defined with value: %MSSQL_DATABASE%"
	set MODE=%BATCH_MODE%
)

rem set the name of the mssql user
IF [%MSSQL_ADMIN_NAME%] NEQ [] (
    echo "MSSQL_ADMIN_NAME environment variable is defined with value: %MSSQL_ADMIN_NAME%"
)

IF [%MSSQL_ADMIN_PASSWORD%] NEQ [] (
    echo "MSSQL_ADMIN_PASSWORD environment variable is defined and will be used"
)
rem set the name of the mssql user to be created
IF [%MSSQL_USER_NAME%]==[] (
    set MSSQL_USER_NAME=AtsUser
) ELSE (
    echo "MSSQL_USER_NAME environment variable is defined with value: %MSSQL_USER_NAME%"
)

rem set port to connect to
IF [%MSSQL_USER_PASSWORD%]==[] (
    set MSSQL_USER_PASSWORD=AtsPassword
) ELSE (
    echo "MSSQL_USER_PASSWORD environment variable is defined and will be used"
)

set path=%path%;"C:\Program Files\Microsoft SQL Server\MSSQL\Binn"
:: check if the script is executed manually
set CONSOLE_MODE_USED=true
echo %cmdcmdline% | find /i "%~0" >nul
if not errorlevel 1 set CONSOLE_MODE_USED=false

set HELP=false
:GETOPTS
IF "%1" == "-H" ( set MSSQL_HOST=%2& shift
)ELSE IF "%1" == "-p" ( set MSSQL_PORT=%2& shift
)ELSE IF "%1" == "-d" ( set MSSQL_DATABASE=%2& set MODE=%BATCH_MODE%& shift
)ELSE IF "%1" == "-U" ( set MSSQL_ADMIN_NAME=%2& shift
)ELSE IF "%1" == "--help" ( set HELP="true"
)ELSE IF "%1" == "-S" ( set MSSQL_ADMIN_PASSWORD=%2& shift
)ELSE IF "%1" == "-u" ( set MSSQL_USER_NAME=%2& shift
)ELSE IF "%1" == "-s" ( set MSSQL_USER_PASSWORD=%2& shift
)ELSE ( set HELP="true" & if "%2%" ==! "" & shift )
shift
IF NOT "%1" == "" (
goto GETOPTS
)


IF "%HELP%" == "true" (
    echo "The usage is ./upgrade.cmd [OPTION]...[VALUE]...
   The following script upgrades an ATS Logging DB from version %OLD_DB_VERSION% to current version %NEW_DB_VERSION%"
     echo "Available options
   -H <target_SQL_server_host>, default is: localhost,Might be specified by env variable: MSSQL_HOST
   -p <target_SQL_server_port>, default is: 1433, Might be specified by env variable: MSSQL_PORT
   -d <target_SQL_database_name>, default: no;  Required for non-interactive batch mode. Might be specified by env variable: MSSQL_DBNAME
   -u <target_SQL_user_name>, default is: AtsUser,Might be specified by env variable: MSSQL_USER_NAME
   -s <target_SQL_user_password>, Might be specified by env variable: MSSQL_USER_PASSWORD
   -U <target_SQL_admin_name>, default: no; Required for non-interactive batch mode. Might be specified by env variable: MSSQL_ADMIN_NAME
   -S <target_SQL_admin_password>, default: no; Required for non-interactive batch mode. Might be specified by env variable: MSSQL_ADMIN_PASSWORD"
)

rem fill in required parameters that has not been previously stated
IF "%MODE%"=="%INTERACTIVE_MODE%" (

    IF [%MSSQL_ADMIN_NAME%]==[] (
         SET /P MSSQL_ADMIN_NAME=Enter MSSQL sever admin name:
         )

         IF [%MSSQL_ADMIN_PASSWORD%]==[] (
           SET /P MSSQL_ADMIN_PASSWORD=Enter MSSQL sever admin password:
         )

         IF [%MSSQL_DATABASE%]==[] (
         :set_MSSQL_DATABASE
           SET /P MSSQL_DATABASE=Enter Test Explorer database name:
          )
       )
)

REM check if there is already database with this name and write the result
sqlcmd -S tcp:%MSSQL_HOST%,%MSSQL_PORT% -U %MSSQL_ADMIN_NAME% -P %MSSQL_ADMIN_PASSWORD% /d master -Q"SET NOCOUNT ON;SELECT name FROM master.dbo.sysdatabases where name='%MSSQL_DATABASE%'" -h-1 | find /i "%MSSQL_DATABASE%"
if errorlevel 1 (

	 IF "%MODE%" == "%BATCH_MODE%" (
		echo "A database with the specified name: %MSSQL_DATABASE% does not exist. Now will exit"
		exit 1
	) ELSE (
		echo  "A database with the specified name: %MSSQL_DATABASE% does not exist."
		GOTO :set_MSSQL_DATABASE
	)
)


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

sqlcmd -S tcp:%MSSQL_HOST%,%MSSQL_PORT% -U %MSSQL_ADMIN_NAME% -P %MSSQL_ADMIN_PASSWORD% /b /V 5 /d "%MSSQL_DATABASE%" /i tempCheckVersion.sql
IF %ERRORLEVEL% NEQ 0 goto stopUpgrade



:: ##################   UPGRADE SQL SCRIPT GENERATION   #####################
del /f /q tempCheckVersion.sql
type nul > tempUpgradeDBScript.sql

echo use [%MSSQL_DATABASE%]                                                          >> tempUpgradeDBScript.sql
echo GO                                                                         >> tempUpgradeDBScript.sql
echo PRINT GETDATE()                                                            >> tempUpgradeDBScript.sql
echo GO                                                                         >> tempUpgradeDBScript.sql
echo ALTER DATABASE [%MSSQL_DATABASE%] SET ANSI_NULLS ON                             >> tempUpgradeDBScript.sql
echo GO                                                                         >> tempUpgradeDBScript.sql
echo ALTER DATABASE [%MSSQL_DATABASE%] SET ANSI_PADDING ON                           >> tempUpgradeDBScript.sql
echo GO                                                                         >> tempUpgradeDBScript.sql
echo ALTER DATABASE [%MSSQL_DATABASE%] SET ANSI_WARNINGS ON                          >> tempUpgradeDBScript.sql
echo GO                                                                         >> tempUpgradeDBScript.sql
echo ALTER DATABASE [%MSSQL_DATABASE%] SET ARITHABORT ON                             >> tempUpgradeDBScript.sql
echo GO                                                                         >> tempUpgradeDBScript.sql
echo ALTER DATABASE [%MSSQL_DATABASE%] SET CONCAT_NULL_YIELDS_NULL ON                >> tempUpgradeDBScript.sql
echo GO                                                                         >> tempUpgradeDBScript.sql
echo ALTER DATABASE [%MSSQL_DATABASE%] SET QUOTED_IDENTIFIER ON                      >> tempUpgradeDBScript.sql
echo GO                                                                         >> tempUpgradeDBScript.sql
echo UPDATE tInternal SET value = '%NEW_DB_VERSION%_draft' WHERE [key] = 'version' >> tempUpgradeDBScript.sql
echo GO                                                                            >> tempUpgradeDBScript.sql
type TestExplorerDB-Upgrade.sql                                                    >> tempUpgradeDBScript.sql
echo -- end of Upgrade script                                                      >> tempUpgradeDBScript.sql
echo GO                                                                            >> tempUpgradeDBScript.sql
echo UPDATE tInternal SET value = '%NEW_DB_VERSION%' WHERE [key] = 'version'       >> tempUpgradeDBScript.sql
echo GO                                                                            >> tempUpgradeDBScript.sql

sqlcmd -S tcp:%MSSQL_HOST%,%MSSQL_PORT% -U %MSSQL_ADMIN_NAME% -P %MSSQL_ADMIN_PASSWORD% /b /d master /i tempUpgradeDBScript.sql /o upgrade.log
IF %ERRORLEVEL% NEQ 0 goto upgradeFailed



:: ##################   UPGRADE SUCCESS     ################################
del /f /q tempUpgradeDBScript.sql
echo Upgrade Completed. Check the 'upgrade.log' for details.
GOTO :End


:: ##################   UPGRADE FAILED  ####################################
:upgradeFailed
rem del /f /q tempUpgradeDBScript.sql
echo ERROR - upgrade failed. Check the 'upgrade.log' file for the errors.
IF "%MODE%" == "%BATCH_MODE%" (
	exit 2
) ELSE (
	GOTO :End
)

:: ##################   STOPING UPGRADE PROCEDURE   ########################
:stopUpgrade
del /f /q tempCheckVersion.sql
echo Upgrade aborted. No changes are made to the database.
IF "%MODE%" == "%BATCH_MODE%" (
	exit 3
) ELSE (
	GOTO :End
)

:: ##################    THE END    ########################################
:End
echo Upgrade completed. Check upgrade.log file for potential errors.
IF "%CONSOLE_MODE_USED%" == "true" (
	rem return to the start folder
	cd /d %START_FOLDER%
) ELSE IF "%MODE%" == "%INTERACTIVE_MODE%" (
    	pause
    	exit
    ) ELSE (
  	exit 0
)
