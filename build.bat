@ECHO OFF
ECHO Make sure you have added in PATH location to bin directories of JDK, Maven and Git client
REM Build and install artifacts locally
mvn -V -s settings.xml clean install %*
REM For other goals you may comment above line and uncomment next one
REM mvn -V -s settings.xml %*