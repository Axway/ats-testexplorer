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
    set MSSQLPORT=1433
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
    echo MSSQL_ADMIN_PASSWORD environment variable is defined with environment variable
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
    echo MSSQL_USER_PASSWORD environment variable is defined with environment variable
)

:: save the starting folder location
set START_FOLDER=%cd%

:: navigate to the install file directory
cd  /d "%~dp0"

set path=%path%;"C:\Program Files\Microsoft SQL Server\MSSQL\Binn"
:: check if the script is executed manually
set CONSOLE_MODE_USED=true
echo %cmdcmdline% | find /i "%~0" >nul
if not errorlevel 1 set CONSOLE_MODE_USED=false

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


IF "%HELP%" == "true" (
    echo "The usage is ./install_postgresql.cmd [OPTION]...[VALUE]...
   The following script installs an ATS Logging DB to store test execution results. The current version is 4.0.8"
     echo "Available options
 -H <target_SQL_server_host>, default is: localhost,Might be specified by env variable: MSSQL_HOST
   -p <target_SQL_server_port>, default is: 1433, Might be specified by env variable: MSSQL_PORT
   -d <target_SQL_database_name>, default: no;  Required for non-interactive batch mode. Might be specified by env variable: MSSQL_DBNAME
   -u <target_SQL_user_name>, default is: AtsUser,Might be specified by env variable: MSSQL_USER_NAME
   -s <target_SQL_user_password>, Might be specified by env variable: MSSQL_USER_PASSWORD
   -U <target_SQL_admin_name>, default: no; Required for non-interactive batch mode. Might be specified by env variable: MSSQL_ADMIN_NAME
   -S <target_SQL_admin_password>, default: no; Required for non-interactive batch mode. Might be specified by env variable: MSSQL_ADMIN_PASSWORD"

)

rem delete tempCreateDBScript.sql from previous installations
IF EXIST tempCreateDBScript.sql (
	del /f /q tempCreateDBScript.sql
)

rem fill in required parameters that has not been previously stated
IF %MODE%==%INTERACTIVE_MODE% (

   IF [%MSSQL_ADMIN_NAME%]==[] (
     SET /P MSSQL_ADMIN_NAME=Enter MSSQL sever admin name:
     )

     IF [%MSSQL_ADMIN_PASSWORD%]==[] (
       SET /P MSSQL_ADMIN_PASSWORD=Enter MSSQL sever admin password:
     )

     IF [%MSSQLDATABASE%]==[] (
     :set_MSSQLDATABASE
       SET /P MSSQLDATABASE=Enter Test Explorer database name:
      )
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


type TestExplorerDB.sql>>tempCreateDBScript.sql

sqlcmd -S tcp:%MSSQLHOST%,%MSSQLPORT% -U %MSSQL_ADMIN_NAME% -P %MSSQL_ADMIN_PASSWORD% /d master /i tempCreateDBScript.sql /o install.log

sqlcmd -S tcp:%MSSQLHOST%,%MSSQLPORT% -U %MSSQL_ADMIN_NAME% -P %MSSQL_ADMIN_PASSWORD% -d %MSSQLDATABASE% -Q "SELECT * FROM tInternal"

IF %ERRORLEVEL% NEQ 0 (
	del /q /f tempCreateDBScript.sql
	echo Installation was not successful. Check install.log file for errors.
	IF "%MODE%" == "%BATCH_MODE%" (
		exit 3
	) ELSE (
		GOTO :end
	)
) ELSE (
	del /q /f tempCreateDBScript.sql
	echo Installation completed. Check install.log file for potential errors.
		IF "%MODE%" == "%BATCH_MODE%" (
		exit 0
	) ELSE (
		GOTO :end
	)
)

rem return to the start folder
:end
IF "%CONSOLE_MODE_USED%" == "true" (
	cd /d %START_FOLDER%
) ELSE IF "%MODE%" == "%INTERACTIVE_MODE%" (
	pause
	exit
)
