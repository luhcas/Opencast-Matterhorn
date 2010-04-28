##
# Configure these variables to match your environment
##

if [ ! -z $FELIX_HOME ]; then
  FELIX=$FELIX_HOME
else
  FELIX="/Applications/Matterhorn"
fi

if [ ! -z $M2_REPO ]; then
  M2_REPO=$M2_REPO
else
  M2_REPO="/Users/johndoe/.m2/repository"
fi

if [ ! -z $OPENCAST_LOGDIR ]; then
  LOGDIR=$OPENCAST_LOGDIR
else
  LOGDIR=$FELIX/logs
fi

DEBUG_PORT="8000"
DEBUG_SUSPEND="n"

##
# Only change the line below if you want to customize the server
##

MAVEN_ARG="-DM2_REPO=$M2_REPO"
FELIX_FILEINSTALL_OPTS="-Dfelix.fileinstall.dir=$FELIX/load"
PAX_CONFMAN_OPTS="-Dbundles.configuration.location=$FELIX/conf"
PAX_LOGGING_OPTS="-Dorg.ops4j.pax.logging.DefaultServiceLog.level=WARN -Dopencast.logdir=$LOGDIR"
UTIL_LOGGING_OPTS="-Djava.util.logging.config.file=$FELIX/conf/services/java.util.logging.properties"

# Clear the felix cache directory
FELIX_CACHE="$FELIX/felix-cache"
rm -rf $FELIX_CACHE

# Create the debug config
#DEBUG_OPTS="-Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,address=$DEBUG_PORT,server=y,suspend=$DEBUG_SUSPEND"

# For Java 6, you need some minor xml facility configuration
# No longer needed for CXF 2.3.0 after 01 Oct
#XML_OPTS="-Djavax.xml.stream.XMLInputFactory=com.ctc.wstx.stax.WstxInputFactory -Djavax.xml.stream.XMLOutputFactory=com.ctc.wstx.stax.WstxOutputFactory -Djavax.xml.stream.XMLEventFactory=com.ctc.wstx.stax.WstxEventFactory"
XML_OPTS=""

#Graphics options
GRAPHIX_OPTS="-Djava.awt.headless=true"


# Finally start felix
cd $FELIX
java $DEBUG_OPTS $XML_OPTS $GRAPHIX_OPTS $MAVEN_ARG $FELIX_FILEINSTALL_OPTS $PAX_CONFMAN_OPTS $PAX_LOGGING_OPTS $UTIL_LOGGING_OPTS $CXF_OPTS -jar $FELIX/bin/felix.jar $FELIX_CACHE
