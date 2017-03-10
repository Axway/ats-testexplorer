@echo off
echo INSTALLING Test Explorer Database
set path=%path%;"C:\Program Files\Microsoft SQL Server\MSSQL\Binn"
start /wait generateCreateDBScript.cmd
type TestExplorerDB.sql>>tempCreateDBScript.sql
osql /E /d master /i tempCreateDBScript.sql /o install.log
del /q /f tempCreateDBScript.sql
echo Installation completed. Check install.log file for errors.
pause