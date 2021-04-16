@echo off
@setlocal enabledelayedexpansion enableextensions

set BATCH_MODE=0
set INTERACTIVE_MODE=1
set MODE=%INTERACTIVE_MODE%
rem set host to connect to
IF [%MSSQL_HOST%]==[] (
    set MSSQL_HOST=localhost
) ELSE (
    echo MSSQL_HOST environment variable is defined with value: %MSSQL_HOST%
)

rem set port to connect to
IF [%MSSQL_PORT%]==[] (
    set MSSQL_PORT=1433
) ELSE (
    echo MSSQL_PORT environment variable is defined with value: %MSSQL_PORT%
)
rem set the name of the database to install
IF [%MSSQL_DATABASE%] NEQ [] (
    echo MSSQL_DATABASE environment variable is defined with value: %MSSQL_DATABASE%
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
    echo "The usage is ./install_postgresql.cmd [OPTION]...[VALUE]... "
    echo "The following script installs an ATS Logging DB to store test execution results. The current version is 4.0.8"
    echo "Available options"
    echo "  --help print this usage text"
    echo "  -H <target_SQL_server_host>, default is: localhost,Might be specified by env variable: MSSQL_HOST "
    echo "  -p <target_SQL_server_port>, default is: 1433, Might be specified by env variable: MSSQL_PORT "
    echo "  -d <target_SQL_database_name>, default: no;  Required for non-interactive batch mode. Might be specified by env variable: MSSQL_DBNAME "
    echo "  -u <target_SQL_user_name>, default is: AtsUser,Might be specified by env variable: MSSQL_USER_NAME "
    echo "  -s <target_SQL_user_password>, Might be specified by env variable: MSSQL_USER_PASSWORD "
    echo "  -U <target_SQL_admin_name>, default: no; Required for non-interactive batch mode. Might be specified by env variable: MSSQL_ADMIN_NAME "
    echo "  -S <target_SQL_admin_password>, default: no; Required for non-interactive batch mode. Might be specified by env variable: MSSQL_ADMIN_PASSWORD"
    GOTO :end
)

rem delete tempCreateDBScript.sql from previous installations
IF EXIST tempCreateDBScript.sql (
    del /f /q tempCreateDBScript.sql
)

rem fill in required parameters that has not been previously stated
IF %MODE%==%INTERACTIVE_MODE% (

    IF [%MSSQL_ADMIN_NAME%]==[] (
        SET /P MSSQL_ADMIN_NAME=Enter MSSQL server admin name:
    )

     IF [%MSSQL_ADMIN_PASSWORD%]==[] (
       SET /P MSSQL_ADMIN_PASSWORD=Enter MSSQL server admin password:
     )

     IF [%MSSQL_DATABASE%]==[] (
     :set_MSSQL_DATABASE
         SET /P MSSQL_DATABASE=Enter Test Explorer database name:
     )
)

REM check if there is already database with this name and write the result
sqlcmd -S tcp:%MSSQL_HOST%,%MSSQL_PORT% -U %MSSQL_ADMIN_NAME% -P %MSSQL_ADMIN_PASSWORD% /d master -Q"SET NOCOUNT ON;SELECT name FROM master.dbo.sysdatabases where name='%MSSQL_DATABASE%'" -h-1 | find /i "%MSSQL_DATABASE%"
if not errorlevel 1 (

     IF "%MODE%" == "%BATCH_MODE%" (
        echo Such database already exists. Now will exit
        exit 1
    ) ELSE (
        echo Such database already exists. Please choose another name
        GOTO :set_MSSQL_DATABASE
    )
)


:: ##################   INSTALL SQL SCRIPT #####################

echo USE [master] > tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo CREATE DATABASE [%MSSQL_DATABASE%]  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo IF @@ERROR ^^!= 0 >> tempCreateDBScript.sql
echo     BEGIN >> tempCreateDBScript.sql
echo       PRINT 'Error occurred during database creation' + @@ERROR >> tempCreateDBScript.sql
echo        set noexec on >> tempCreateDBScript.sql
echo     END >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo EXEC dbo.sp_dbcmptlevel @dbname=N'%MSSQL_DATABASE%', @new_cmptlevel=100 >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%MSSQL_DATABASE%] SET ANSI_NULL_DEFAULT OFF  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%MSSQL_DATABASE%] SET ANSI_NULLS ON  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%MSSQL_DATABASE%] SET ANSI_PADDING ON  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%MSSQL_DATABASE%] SET ANSI_WARNINGS ON  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%MSSQL_DATABASE%] SET ARITHABORT ON  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%MSSQL_DATABASE%] SET AUTO_CLOSE OFF  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%MSSQL_DATABASE%] SET AUTO_CREATE_STATISTICS ON  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%MSSQL_DATABASE%] SET AUTO_SHRINK OFF  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%MSSQL_DATABASE%] SET AUTO_UPDATE_STATISTICS ON  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%MSSQL_DATABASE%] SET CURSOR_CLOSE_ON_COMMIT OFF  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%MSSQL_DATABASE%] SET CURSOR_DEFAULT  GLOBAL  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%MSSQL_DATABASE%] SET CONCAT_NULL_YIELDS_NULL ON  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%MSSQL_DATABASE%] SET NUMERIC_ROUNDABORT OFF  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%MSSQL_DATABASE%] SET QUOTED_IDENTIFIER ON  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%MSSQL_DATABASE%] SET RECURSIVE_TRIGGERS OFF  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%MSSQL_DATABASE%] SET  READ_WRITE  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%MSSQL_DATABASE%] SET RECOVERY SIMPLE  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%MSSQL_DATABASE%] SET  MULTI_USER  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%MSSQL_DATABASE%] SET TORN_PAGE_DETECTION ON  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo USE [%MSSQL_DATABASE%] >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo IF NOT EXISTS ( SELECT name FROM master.sys.server_principals WHERE name = 'AtsUser' ) >> tempCreateDBScript.sql
echo    BEGIN >> tempCreateDBScript.sql
echo         EXEC master.dbo.sp_addlogin @loginame = N'AtsUser', @passwd = 'AtsPassword', @defdb = N'%MSSQL_DATABASE%', @deflanguage = N'us_english' >> tempCreateDBScript.sql
echo    END >> tempCreateDBScript.sql

echo         EXEC dbo.sp_grantdbaccess @loginame = N'AtsUser', @name_in_db = N'AtsUser' >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql

echo         EXEC dbo.sp_addrolemember @rolename = N'db_owner', @membername  = N'AtsUser' >> tempCreateDBScript.sql
echo         USE [%MSSQL_DATABASE%] >> tempCreateDBScript.sql

echo GO >> tempCreateDBScript.sql


type TestExplorerDB.sql>>tempCreateDBScript.sql

powershell -command "(get-content tempCreateDBScript.sql) -replace 'AtsUser', '%MSSQL_USER_NAME%'  | Set-Content tempCreateDBScript.sql"
powershell -command "(get-content tempCreateDBScript.sql) -replace 'AtsPassword', '%MSSQL_USER_PASSWORD%'  | Set-Content tempCreateDBScript.sql"

sqlcmd -S tcp:%MSSQL_HOST%,%MSSQL_PORT% -U %MSSQL_ADMIN_NAME% -P %MSSQL_ADMIN_PASSWORD% /d master /i tempCreateDBScript.sql /o install.log

set NUM_OF_ERRORS=0
for /f "tokens=*" %%a in ('findstr /R /C:"^Msg [0-9]*, Level [1-9]*, State" install.log') DO (
  set /a NUM_OF_ERRORS+= 1
)

sqlcmd -S tcp:%MSSQL_HOST%,%MSSQL_PORT% -U %MSSQL_USER_NAME% -P %MSSQL_USER_PASSWORD% -d %MSSQL_DATABASE% -Q "SELECT * FROM tInternal"

IF %NUM_OF_ERRORS% == 0  (
    echo "Installing of %MSSQL_DATABASE% completed successfully. Logs are located in install.log file"
    IF  "%MODE%" == "%BATCH_MODE%" (
        exit 0
    )
) ELSE (
    echo "Errors during install: %NUM_OF_ERRORS%"
    echo "Installing of %MSSQL_DATABASE% was not successful. Logs are located in install.log file"
    IF "%MODE%" == "%BATCH_MODE%" (
        exit 4
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
