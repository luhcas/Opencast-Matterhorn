REM ##
REM # Configure these variables to match your environment
REM ##

SET FELIX=E:/Libraries/felix-1.8.0
SET M2_REPO=C:/Users/cab938/.m2/repository
SET DEBUG_PORT=8000
SET DEBUG_SUSPEND=n

REM ##
REM # Only change the lines below if you know what you are doing
REM ##

SET MAVEN_OPTS=-DM2_REPO=%M2_REPO%
SET FELIX_FILEINSTALL_OPTS=-Dfelix.fileinstall.dir=%FELIX%/load
SET PAX_CONFMAN_OPTS=-Dbundles.configuration.location=%FELIX%/conf
SET CXF_OPTS=-Djava.util.logging.config.file=%FELIX%/conf/cxf.properties

REM # Clear the felix cache directory
SET FELIX_CACHE=%FELIX%/felix-cache
rm -rf %FELIX_CACHE%

REM # Create the debug config
SET DEBUG_OPTS=-Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,address=%DEBUG_PORT%,server=y,suspend=%DEBUG_SUSPEND%

REM # For Java 6, you need some minor xml facility configuration
REM No longer needed for CXF post 01 Oct
REM SET XML_OPTS=-Djavax.xml.stream.XMLInputFactory=com.ctc.wstx.stax.WstxInputFactory -Djavax.xml.stream.XMLOutputFactory=com.ctc.wstx.stax.WstxOutputFactory -Djavax.xml.stream.XMLEventFactory=com.ctc.wstx.stax.WstxEventFactory

REM # Finally start felix
rm -rf %FELIX%/felix-cache/*
cd %FELIX%
java %DEBUG_OPTS% %MAVEN_OPTS% %FELIX_FILEINSTALL_OPTS% %PAX_CONFMAN_OPTS% %CXF_OPTS% -jar %FELIX%/bin/felix.jar %FELIX_CACHE%