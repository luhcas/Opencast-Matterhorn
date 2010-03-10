#! /bin/bash

# Must be ran as "matterhorn" user

export M2_REPO=/home/$USER/.m2/repository
export FELIX_HOME=/home/$USER/felix-framework-2.0.4
export JAVA_HOME=/usr/lib/jvm/java-6-sun-1.6.0.15

su $USER -c "date >> /home/$USER/nightly.log"
su $USER -c "svn cat http://opencast.jira.com/svn/MH/trunk/pom.xml > pom.xml"
su $USER -c "svn up /home/$USER/capture-agent/modules/*"
su $USER -c "svn up /home/$USER/capture-agent/docs"

cd /home/$USER/capture-agent
su $USER -c "mvn clean install -Pcapture -DdeployTo=${FELIX_HOME}/load"

/sbin/reboot now

