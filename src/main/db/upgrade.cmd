@echo off
set NEW_DB_VERSION=4.0.2
set CURRENT_DB_VERSION=4.0.0

echo Upgrading TestExplorer Database from version %CURRENT_DB_VERSION% to %NEW_DB_VERSION%

echo Current upgrade could take more time especially on large databases. Please do not close console before success/fail message is displayed

echo It is recommended that you backup your database before continue!
set /p ans=Are you sure you want to continue? [y/n]^>
if /i "%ans%" NEQ "y" if /i "%ans%" NEQ "yes" goto End

set /p logdbname=Enter Database Name to upgrade:



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

sqlcmd /E /b /V 5 /d "%logdbname%" /i tempCheckVersion.sql
IF %ERRORLEVEL% NEQ 0 goto stopUpgrade



:: ##################   UPGRADE SQL SCRIPT GENERATION   #####################
del /f /q tempCheckVersion.sql
type nul > tempUpgradeDBScript.sql

echo use [%logdbname%]                                                          >> tempUpgradeDBScript.sql
echo GO                                                                         >> tempUpgradeDBScript.sql
echo PRINT GETDATE()                                                            >> tempUpgradeDBScript.sql
echo GO                                                                         >> tempUpgradeDBScript.sql
echo ALTER DATABASE [%logdbname%] SET ANSI_NULLS ON                             >> tempUpgradeDBScript.sql
echo GO                                                                         >> tempUpgradeDBScript.sql
echo ALTER DATABASE [%logdbname%] SET ANSI_PADDING ON                           >> tempUpgradeDBScript.sql
echo GO                                                                         >> tempUpgradeDBScript.sql
echo ALTER DATABASE [%logdbname%] SET ANSI_WARNINGS ON                          >> tempUpgradeDBScript.sql
echo GO                                                                         >> tempUpgradeDBScript.sql
echo ALTER DATABASE [%logdbname%] SET ARITHABORT ON                             >> tempUpgradeDBScript.sql
echo GO                                                                         >> tempUpgradeDBScript.sql
echo ALTER DATABASE [%logdbname%] SET CONCAT_NULL_YIELDS_NULL ON                >> tempUpgradeDBScript.sql
echo GO                                                                         >> tempUpgradeDBScript.sql
echo ALTER DATABASE [%logdbname%] SET QUOTED_IDENTIFIER ON                      >> tempUpgradeDBScript.sql
echo GO                                                                         >> tempUpgradeDBScript.sql
echo UPDATE tInternal SET value = '%NEW_DB_VERSION%_draft' WHERE [key] = 'version' >> tempUpgradeDBScript.sql
echo GO                                                                            >> tempUpgradeDBScript.sql
type TestExplorerDB-Upgrade.sql                                                    >> tempUpgradeDBScript.sql
echo "  -- end of Upgrade script"                                                  >> tempUpgradeDBScript.sql
echo GO                                                                            >> tempUpgradeDBScript.sql
echo UPDATE tInternal SET value = '%NEW_DB_VERSION%' WHERE [key] = 'version'       >> tempUpgradeDBScript.sql
echo GO                                                                            >> tempUpgradeDBScript.sql

osql /b /E /d master /i tempUpgradeDBScript.sql /o upgrade.log
IF %ERRORLEVEL% NEQ 0 goto upgradeFailed



:: ##################   UPGRADE SUCCESS     ################################
del /f /q tempUpgradeDBScript.sql
echo Upgrade Completed. Check the 'upgrade.log' for details.
GOTO End


:: ##################   UPGRADE FAILED  ####################################
:upgradeFailed
rem del /f /q tempUpgradeDBScript.sql
echo ERROR - upgrade failed. Check the 'upgrade.log' file for the errors.
GOTO End

:: ##################   STOPING UPGRADE PROCEDURE   ########################
:stopUpgrade
del /f /q tempCheckVersion.sql
echo Upgrade aborted. No changes are made to the database.
GOTO End


:: ##################    THE END    ########################################
:End
pause

@echo on
exit
