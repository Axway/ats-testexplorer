echo "Make sure you have added in PATH location to bin directories of JDK, Maven and Git client"
# Build and install artifacts locally
mvn -V -s settings.xml clean install $*
# For other goals you may comment above line and uncomment next one
#mvn -V -s settings.xml $*