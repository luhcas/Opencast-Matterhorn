#!/bin/bash

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
  M2_REPO="/Users/johndoe"
fi

if [ ! -z $OPENCAST_LOGDIR ]; then
  LOGDIR=$OPENCAST_LOGDIR
else
  LOGDIR=$FELIX/logs
fi

##
# To enable the debugger on the vm, enable all of the following options
##

DEBUG_PORT="8000"
DEBUG_SUSPEND="n"
DEBUG_OPTS="-Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,address=$DEBUG_PORT,server=y,suspend=$DEBUG_SUSPEND"

##
# Only change the line below if you want to customize the server
##

MAVEN_ARG="-DM2_REPO=$M2_REPO"
FELIX_FILEINSTALL_OPTS="-Dfelix.fileinstall.dir=$FELIX/load"
PAX_CONFMAN_OPTS="-Dbundles.configuration.location=$FELIX/conf"
PAX_LOGGING_OPTS="-Dorg.ops4j.pax.logging.DefaultServiceLog.level=WARN -Dopencast.logdir=$LOGDIR"
UTIL_LOGGING_OPTS="-Djava.util.logging.config.file=$FELIX/conf/services/java.util.logging.properties"
GRAPHICS_OPTS="-Djava.awt.headless=true -Dawt.toolkit=sun.awt.HeadlessToolkit"
JAVA_OPTS="-Xms256m -Xmx256m -XX:MaxPermSize=128m"

FELIX_CACHE="$FELIX/felix-cache"

# Make sure matterhorn bundles are reloaded
if [ -d "$FELIX_CACHE" ]; then
  echo "Removing cached matterhorn bundles from $FELIX_CACHE"
  for bundle in `find "$FELIX_CACHE" -type f -name bundle.location | xargs grep --files-with-match -e "file:" | sed -e s/bundle.location// `; do
    rm -r $bundle
  done
fi

# Finally start felix
cd $FELIX
java $DEBUG_OPTS $GRAPHICS_OPTS $MAVEN_ARG $FELIX_FILEINSTALL_OPTS $PAX_CONFMAN_OPTS $PAX_LOGGING_OPTS $UTIL_LOGGING_OPTS $CXF_OPTS -jar $FELIX/bin/felix.jar $FELIX_CACHE
