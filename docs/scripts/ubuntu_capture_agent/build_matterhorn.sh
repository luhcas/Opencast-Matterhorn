#! /bin/bash
# Build Matterhorn.


FELIX_HOME=$1
export JAVA_HOME=$2

# get the necessary matterhorn source code
mkdir -p /home/$USERNAME/capture-agent
cd /home/$USERNAME/capture-agent
svn co http://opencast.jira.com/svn/MH/trunk

# setup felix configuration
cp -r /home/$USERNAME/capture-agent/docs/felix/bin/* ${FELIX_HOME}/bin
cp -r /home/$USERNAME/capture-agent/docs/felix/conf ${FELIX_HOME}

cd ..
mvn clean install -Pcapture -DdeployTo=${FELIX_HOME}/load
exit $?
