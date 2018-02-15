@echo off
@setlocal enabledelayedexpansion enableextensions

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

rem get the command line parameter if there is such
set CMD_ARGUMENT=%~1

set HELP=false
IF [%CMD_ARGUMENT%]==[--help] set HELP=true
IF [%CMD_ARGUMENT%]==[/?] set HELP=true
IF "%HELP%" == "true" (
    echo Add the database name as a parameter for silent install
)

:set_dbname 
IF [%CMD_ARGUMENT%]==[] (
    SET /P dbname=Enter Test Explorer database name:
) ELSE (
    set dbname=%CMD_ARGUMENT%
)

REM check if there is already database with this name and write the result to file
osql /E /d master -Q"SET NOCOUNT ON;SELECT name FROM master.dbo.sysdatabases where name='%dbname%'" -h-1 /o check_dbname.txt

REM get only the first line from the file
FOR /F "delims= " %%i IN ( check_dbname.txt ) DO (
    SET file_cont=%%i
    GOTO :endfor
)
:endfor
del /f /q check_dbname.txt

REM check if there is already database with the same name, if so back to set a new name
IF [%file_cont%] EQU [%dbname%] ( 
	IF [%CMD_ARGUMENT%]==[] (
	echo Such database already exists. Please choose another name 
		GOTO :set_dbname
	) ELSE (
	echo Such database already exists. Now will exit
		exit 1
	)
)


:: ##################   INSTALL SQL SCRIPT #####################

echo USE [master] > tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo CREATE DATABASE [%dbname%]  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo EXEC dbo.sp_dbcmptlevel @dbname=N'%dbname%', @new_cmptlevel=80 >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%dbname%] SET ANSI_NULL_DEFAULT OFF  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%dbname%] SET ANSI_NULLS ON  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%dbname%] SET ANSI_PADDING ON  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%dbname%] SET ANSI_WARNINGS ON  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%dbname%] SET ARITHABORT ON  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%dbname%] SET AUTO_CLOSE OFF  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%dbname%] SET AUTO_CREATE_STATISTICS ON  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%dbname%] SET AUTO_SHRINK OFF  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%dbname%] SET AUTO_UPDATE_STATISTICS ON  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%dbname%] SET CURSOR_CLOSE_ON_COMMIT OFF  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%dbname%] SET CURSOR_DEFAULT  GLOBAL  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%dbname%] SET CONCAT_NULL_YIELDS_NULL ON  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%dbname%] SET NUMERIC_ROUNDABORT OFF  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%dbname%] SET QUOTED_IDENTIFIER ON  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%dbname%] SET RECURSIVE_TRIGGERS OFF  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%dbname%] SET  READ_WRITE  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%dbname%] SET RECOVERY SIMPLE  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%dbname%] SET  MULTI_USER  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo ALTER DATABASE [%dbname%] SET TORN_PAGE_DETECTION ON  >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo USE [%dbname%] >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo EXEC master.dbo.sp_addlogin @loginame = N'AtsUser', @passwd = 'AtsPassword', @defdb = N'%dbname%', @deflanguage = N'us_english' >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo EXEC dbo.sp_grantdbaccess @loginame = N'AtsUser', @name_in_db = N'AtsUser' >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql
echo EXEC dbo.sp_addrolemember @rolename = N'db_owner', @membername  = N'AtsUser' >> tempCreateDBScript.sql
echo USE [%dbname%] >> tempCreateDBScript.sql
echo GO >> tempCreateDBScript.sql

IF %ERRORLEVEL% NEQ 0 (
	echo Check install.log file for errors.
	IF [%CMD_ARGUMENT%]==[] (
		pause
		) ELSE (
		exit 2
	)
)

type TestExplorerDB.sql>>tempCreateDBScript.sql
osql /E /d master /i tempCreateDBScript.sql /o install.log
IF %ERRORLEVEL% NEQ 0 (
	del /q /f tempCreateDBScript.sql
	echo Installation was not successful. Check install.log file for errors.
	IF [%CMD_ARGUMENT%]==[] (
		pause
	) ELSE (
		exit 3
	)
) ELSE (
	del /q /f tempCreateDBScript.sql
	echo Installation completed. Check install.log file for potential errors.
	IF [%CMD_ARGUMENT%]==[] (
		pause
	) ELSE (
		exit 0
	)
)

rem return to the start folder
cd /d %START_FOLDER%
