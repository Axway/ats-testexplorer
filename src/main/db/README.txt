Instruction on how to get Test Explorer up and running are provided at:

https://axway.github.io/ats-testexplorer/Test-Explorer---Install-guide.html


For ATS Developers only:
	
	Notes for changing upgrade scripts:
	
		Before adding changes to the upgrade scripts for both MSSQL and PostgreSQL, check the provided template_upgrade_script.sql
		Note that <sql_action> is 'ALTER' or 'CREATE' for MSSQL and 'CREATE OR REPLACE' for PostgreSQL.
		After adding those changes, update the initial version, which is the last internal version from the upgrade scripts in both MSSQL and PostgreSQL install scripts.
		
	Both files (install and upgrade) for MSSQL must contain two empty rows at the end.
