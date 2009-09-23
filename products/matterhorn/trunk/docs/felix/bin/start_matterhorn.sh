##
# Configure these variables to match your environment
##

FELIX="/Applications/Matterhorn"
M2_REPO="/Users/johndoe/.m2/repository"
DEBUG_PORT="8000"
DEBUG_SUSPEND="n"

##
# Only change the line below if you want to customize the server
##

MAVEN_OPTS="-DM2_REPO=$M2_REPO"
FELIX_FILEINSTALL_OPTS="-Dfelix.fileinstall.dir=$FELIX/load"
PAX_CONFMAN_OPTS="-Dbundles.configuration.location=$FELIX/conf"
CXF_OPTS="-Djava.util.logging.config.file=$FELIX/conf/cxf.properties"

# Clear the felix cache directory
FELIX_CACHE="$FELIX/felix-cache"
rm -rf $FELIX_CACHE

# Create the debug config
DEBUG_OPTS="-Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,address=$DEBUG_PORT,server=y,suspend=$DEBUG_SUSPEND"

# For Java 6, you need some minor xml facility configuration
XML_OPTS="-Djavax.xml.stream.XMLInputFactory=com.ctc.wstx.stax.WstxInputFactory -Djavax.xml.stream.XMLOutputFactory=com.ctc.wstx.stax.WstxOutputFactory -Djavax.xml.stream.XMLEventFactory=com.ctc.wstx.stax.WstxEventFactory"

# Finally start felix
cd $FELIX
java $DEBUG_OPTS $XML_OPTS $MAVEN_OPTS $FELIX_FILEINSTALL_OPTS $PAX_CONFMAN_OPTS $CXF_OPTS -jar $FELIX/bin/felix.jar $FELIX_CACHE