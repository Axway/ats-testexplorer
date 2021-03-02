@echo off
@setlocal enabledelayedexpansion enableextensions

set BATCH_MODE=0
set INTERACTIVE_MODE=1
set MODE=%INTERACTIVE_MODE%
rem set host to connect to
IF [%MSSQLHOST%]==[] (
    set MSSQLHOST=localhost
) ELSE (
    echo MSSQLHOST environment variable is defined with value: %MSSQLHOST%
)

rem set port to connect to
IF [%MSSQLPORT%]==[] (
    set MSSQLPORT=5432
) ELSE (
    echo MSSQLPORT environment variable is defined with value: %MSSQLPORT%
)
rem set the name of the database to install
IF [%MSSQLDATABASE%] NEQ [] (
    echo MSSQLDATABASE environment variable is defined with value: %MSSQLDATABASE%
	set MODE=%BATCH_MODE%
)

rem set the name of the mssql user
IF [%MSSQL_ADMIN_NAME%] NEQ [] (
    echo MSSQL_ADMIN_NAME environment variable is defined with value: %MSSQL_ADMIN_NAME%
)

IF [%MSSQL_ADMIN_PASSWORD%] NEQ [] (
    echo MSSQL_ADMIN_PASSWORD environment variable is defined with value: %MSSQL_ADMIN_PASSWORD%
)
rem set the name of the mssql user to be created
IF [%MSSQL_USER_NAME%]==[] (
    set MSSQL_USER_NAME=AtsUser
) ELSE (
    echo MSSQL_USER_NAME environment variable is defined with value: %MSSQL_USER_NAME%
)

rem set port to connect to
IF [%MSSQL_USER_PASSWORD%]==[] (
    set MSSQL_USER_PASSWORD=AtsPassword
) ELSE (
    echo MSSQL_USER_PASSWORD environment variable is defined with value: %MSSQL_USER_PASSWORD%
)

set HELP=false
:GETOPTS
IF "%1" == "-H" ( set MSSQLHOST=%2& shift
)ELSE IF "%1" == "-p" ( set MSSQLPORT=%2& shift
)ELSE IF "%1" == "-d" ( set MSSQLDATABASE=%2& set MODE=%BATCH_MODE%& shift
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



IF %HELP% == "true" (
echo Please specify the database name as a parameter for silent install
)

:: save the starting folder location
set START_FOLDER=%cd%

:: navigate to the install file directory
cd  /d "%~dp0"

echo INSTALLING Test Explorer Database
set path=%path%;"C:\Program Files\Microsoft SQL Server\MSSQL\Binn"

rem delete tempCreateDBScript.sql from previous installations
IF EXIST tempCreateDBScript.sql (
	del /f /q tempCreateDBScript.sql
)


:set_MSSQLDATABASE
IF %MODE%==%INTERACTIVE_MODE% (

    SET /P MSSQLDATABASE=Enter Test Explorer database name:
)

REM check if there is already database with this name and write the result
sqlcmd -S tcp:%MSSQLHOST%,%MSSQLPORT% -U %MSSQL_ADMIN_NAME% -P %MSSQL_ADMIN_PASSWORD% /d master -Q"SET NOCOUNT ON;SELECT name FROM master.dbo.sysdatabases where name='%MSSQLDATABASE%'" -h-1 | find /i "%MSSQLDATABASE%"
if not errorlevel 1 (

	 IF "%MODE%" == "%BATCH_MODE%" (
		echo Such database already exists. Now will exit
		exit 1
	) ELSE (
		echo Such database already exists. Please choose another name
		GOTO :set_MSSQLDATABASE
	)
)


:: ##################   INSTALL SQL SCRIPT #####################

echo USE [master] > tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo CREATE DATABASE [%MSSQLDATABASE%]  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo EXEC dbo.sp_dbcmptlevel @dbname=N'%MSSQLDATABASE%', @new_cmptlevel=100 >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%MSSQLDATABASE%] SET ANSI_NULL_DEFAULT OFF  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%MSSQLDATABASE%] SET ANSI_NULLS ON  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%MSSQLDATABASE%] SET ANSI_PADDING ON  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%MSSQLDATABASE%] SET ANSI_WARNINGS ON  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%MSSQLDATABASE%] SET ARITHABORT ON  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%MSSQLDATABASE%] SET AUTO_CLOSE OFF  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%MSSQLDATABASE%] SET AUTO_CREATE_STATISTICS ON  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%MSSQLDATABASE%] SET AUTO_SHRINK OFF  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%MSSQLDATABASE%] SET AUTO_UPDATE_STATISTICS ON  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%MSSQLDATABASE%] SET CURSOR_CLOSE_ON_COMMIT OFF  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%MSSQLDATABASE%] SET CURSOR_DEFAULT  GLOBAL  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%MSSQLDATABASE%] SET CONCAT_NULL_YIELDS_NULL ON  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%MSSQLDATABASE%] SET NUMERIC_ROUNDABORT OFF  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%MSSQLDATABASE%] SET QUOTED_IDENTIFIER ON  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%MSSQLDATABASE%] SET RECURSIVE_TRIGGERS OFF  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%MSSQLDATABASE%] SET  READ_WRITE  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%MSSQLDATABASE%] SET RECOVERY SIMPLE  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%MSSQLDATABASE%] SET  MULTI_USER  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%MSSQLDATABASE%] SET TORN_PAGE_DETECTION ON  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo USE [%MSSQLDATABASE%] >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo EXEC master.dbo.sp_addlogin @loginame = N'AtsUser', @passwd = 'AtsPassword', @defdb = N'%MSSQLDATABASE%', @deflanguage = N'us_english' >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo EXEC dbo.sp_grantdbaccess @loginame = N'AtsUser', @name_in_db = N'AtsUser' >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo EXEC dbo.sp_addrolemember @rolename = N'db_owner', @membername  = N'AtsUser' >> tempCreateDBScript.sql
echo USE [%MSSQLDATABASE%] >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql


rem IF %ERRORLEVEL% NEQ 0 (
rem	echo Installation was not successful
rem	IF "%SILENT_MODE_USED%" == "true" (
rem		exit 2
rem	) ELSE (
rem		GOTO :end
rem	)
rem )

type TestExplorerDB.sql>>tempCreateDBScript.sql

sqlcmd -S tcp:%MSSQLHOST%,%MSSQLPORT% -U %MSSQL_ADMIN_NAME% -P %MSSQL_ADMIN_PASSWORD% /d master /i tempCreateDBScript.sql /o install.log

sqlcmd -S tcp:%MSSQLHOST%,%MSSQLPORT% -U %MSSQL_ADMIN_NAME% -P %MSSQL_ADMIN_PASSWORD% -d %MSSQLDATABASE% -Q "SELECT * FROM tInternal"

IF %ERRORLEVEL% NEQ 0 (
	del /q /f tempCreateDBScript.sql
	echo Installation was not successful. Check install.log file for errors.
	IF "%SILENT_MODE_USED%" == "true" (
		exit 3
	) ELSE (
		GOTO :end
	)
) ELSE (
	del /q /f tempCreateDBScript.sql
	echo Installation completed. Check install.log file for potential errors.
	IF "%SILENT_MODE_USED%" == "true" (
		exit 0
	) ELSE (
		GOTO :end
	)
)

rem return to the start folder
:end
IF "%CONSOLE_MODE_USED%" == "true" (
	cd /d %START_FOLDER%
) ELSE IF "%MANUAL_MODE_USED%" == "true" (
	pause
	exit
)
