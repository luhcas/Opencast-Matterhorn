REM ##
REM # Configure these variables to match your environment
REM ##

SET FELIX=E:/Libraries/felix-1.8.0
SET M2_REPO=C:/Users/cab938/.m2/repository

REM ##
REM # Deploy without tests since test fail on windows
REM ##

cd ..
mvn clean install -DdeployTo=%FELIX%/load -DskipTests
