#!/bin/bash

if [ -n "${FELIX_HOME:-x}" ]; then
  FELIX=$FELIX_HOME
else
  FELIX="/opt/matterhorn/felix"
fi


if [ -n "${M2_REPO:-x}" ]; then
  M2_REPO=$M2_REPO
else
  M2_REPO="/home/opencast/.m2/repository"
fi


MATTERHORN_HOME=/opt/matterhorn
MATTERHORN_CONF=/opt/matterhorn/felix/conf
MATTERHORN_LIB=/opt/matterhorn/felix
MATTERHORN_LOG=/opt/matterhorn/log
FELIX_HOME=/opt/matterhorn/felix

#
# Define Constants
#
MAVEN_ARG="-DM2_REPO=$M2_REPO"
MATTERHORN_BUNDLE_DIR=$MATTERHORN_LIB/load
MATTERHORN_WORK_DIR=$MATTERHORN_LIB/work
MATTERHORN_STATIC_DIR=$MATTERHORN_WORK_DIR/opencast/static
MATTERHORN_CACHE_DIR=$MATTERHORN_LIB/felix-cache
MATTERHORN_LOG_FILE=/opt/matterhorn/log/felix.log

#
# Check if nightly is already running
#
FELIX_PID=`ps ux | awk '/felix.jar/ && !/awk/ {print $2}'`
if [ ! -z $FELIX_PID ]; then
  echo "Felix is already running with pid $FELIX_PID"
  exit 1
fi

#
# Clear the matterhorn work directory
#
rm -rf $MATTERHORN_WORK_DIR

#
# Create the directories under /var/lib/matterhorn
#

mkdir -p $MATTERHORN_WORK_DIR
mkdir -p $MATTERHORN_WORK_DIR/opencast
mkdir -p $MATTERHORN_STATIC_DIR
mkdir -p $MATTERHORN_BUNDLE_DIR

# Save current version
svnversion > $MATTERHORN_STATIC_DIR/version.txt

#
# Cd into the work dir, since some java code creates work files in the
# directory used when starting the vm.
#
cd $MATTERHORN_WORK_DIR

#
# Start felix
#
FELIX_FILEINSTALL_OPTS="-Dfelix.fileinstall.dir=$MATTERHORN_BUNDLE_DIR"
PAX_CONFMAN_OPTS="-Dbundles.configuration.location=$MATTERHORN_CONF"
CXF_OPTS="-Djava.util.logging.config.file=$MATTERHORN_CONF/cxf.properties"
JAVA_OPTS="-Xms256m -Xmx512m -XX:PermSize=64m -XX:MaxPermSize=128m -Djava.io.tmpdir=$MATTERHORN_WORK_DIR"
DEBUG_OPTS="-Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n"
PAX_LOGGING_OPTS="-Dorg.ops4j.pax.logging.DefaultServiceLog.level=WARN -Dopencast.logdir=$MATTERHORN_LOG"
UTIL_LOGGING_OPTS="-Djava.util.logging.config.file=$FELIX_HOME/conf/services/java.util.logging.properties"

# Start felix
nohup java $JAVA_OPTS $MAVEN_ARG $DEBUG_OPTS $FELIX_FILEINSTALL_OPTS $PAX_CONFMAN_OPTS $CXF_OPTS $PAX_LOGGING_OPTS $UTIL_LOGGING_OPTS -jar $FELIX_HOME/bin/felix.jar $MATTERHORN_CACHE_DIR >> $MATTERHORN_LOG_FILE &

