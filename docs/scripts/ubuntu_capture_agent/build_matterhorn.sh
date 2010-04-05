#! /bin/bash
# Build Matterhorn.


FELIX_HOME=$1
export JAVA_HOME=$2

# get the necessary matterhorn source code
mkdir -p /home/$USERNAME/capture-agent
cd /home/$USERNAME/capture-agent
svn cat http://opencast.jira.com/svn/MH/trunk/pom.xml > pom.xml
svn co http://opencast.jira.com/svn/MH/trunk/docs docs
mkdir -p modules
cd modules
svn co http://opencast.jira.com/svn/MH/trunk/modules/matterhorn-runtime-tools matterhorn-runtime-tools
svn co http://opencast.jira.com/svn/MH/trunk/modules/matterhorn-build-tools matterhorn-build-tools
svn co http://opencast.jira.com/svn/MH/trunk/modules/matterhorn-util matterhorn-util
svn co http://opencast.jira.com/svn/MH/trunk/modules/matterhorn-media matterhorn-media
svn co http://opencast.jira.com/svn/MH/trunk/modules/matterhorn-dublincore matterhorn-dublincore
svn co http://opencast.jira.com/svn/MH/trunk/modules/matterhorn-metadata-api matterhorn-metadata-api
svn co http://opencast.jira.com/svn/MH/trunk/modules/matterhorn-capture-admin-service-api matterhorn-capture-admin-service-api
svn co http://opencast.jira.com/svn/MH/trunk/modules/matterhorn-capture-agent-api matterhorn-capture-agent-api
svn co http://opencast.jira.com/svn/MH/trunk/modules/matterhorn-capture-agent-impl matterhorn-capture-agent-impl
svn co http://opencast.jira.com/svn/MH/trunk/modules/matterhorn-http matterhorn-http

# setup felix configuration
cp -r /home/$USERNAME/capture-agent/docs/felix/bin/* ${FELIX_HOME}/bin
cp -r /home/$USERNAME/capture-agent/docs/felix/conf ${FELIX_HOME}

cd ..
mvn clean install -Pcapture -DdeployTo=${FELIX_HOME}/load
exit $?
