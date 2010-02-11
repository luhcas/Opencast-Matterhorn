#! /bin/bash
# Build Matterhorn.


FELIX_HOME=$1
export JAVA_HOME=/usr/lib/jvm/java-6-sun-1.6.0.15

# get the necessary matterhorn source code
svn co http://source.opencastproject.org/svn/products/matterhorn/trunk/ /home/$USERNAME/capture-agent --depth empty
cd /home/$USERNAME/capture-agent
svn up pom.xml
svn co http://source.opencastproject.org/svn/products/matterhorn/trunk/docs/ docs
svn co http://source.opencastproject.org/svn/modules/opencast-runtime-tools/trunk opencast-runtime-tools
svn co http://source.opencastproject.org/svn/modules/opencast-build-tools/trunk/ opencast-build-tools
svn co http://source.opencastproject.org/svn/modules/opencast-util/trunk/ opencast-util
svn co http://source.opencastproject.org/svn/modules/opencast-media/trunk/ opencast-media
svn co http://source.opencastproject.org/svn/modules/opencast-dublincore/trunk opencast-dublincore
svn co http://source.opencastproject.org/svn/modules/opencast-metadata-api/trunk opencast-metadata-api
svn co http://source.opencastproject.org/svn/modules/opencast-capture-admin-service-api/trunk/ opencast-capture-admin-service-api
svn co http://source.opencastproject.org/svn/modules/opencast-capture-service-api/trunk/ opencast-capture-service-api
svn co http://source.opencastproject.org/svn/modules/opencast-capture-service-impl/trunk/ opencast-capture-service-impl

# setup felix configuration
cp -r /home/$USERNAME/capture-agent/docs/felix/bin/* ${FELIX_HOME}/bin
cp -r /home/$USERNAME/capture-agent/docs/felix/conf/* ${FELIX_HOME}/conf

mvn clean install -Pcapture -DdeployTo=${FELIX_HOME}/load
