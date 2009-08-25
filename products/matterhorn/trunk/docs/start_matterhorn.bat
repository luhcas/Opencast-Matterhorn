REM ##
REM # Configure these variables to match your environment
REM ##

SET FELIX=E:/Libraries/felix-1.8.0
SET M2_REPO=C:/Users/cab938/.m2/repository

REM ##
REM # Only change the line below if you want to customize the server
REM ##

rm -rf %FELIX%/felix-cache/*
cd %FELIX%
java -Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,address=9191,server=y,suspend=n -DM2_REPO=%M2_REPO% -Dfelix.fileinstall.dir=%FELIX%/load -jar %FELIX%/bin/felix.jar %FELIX%/felix-cache

