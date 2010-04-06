#! /bin/bash

USERNAME=$1
export M2_REPO=/home/$USERNAME/.m2/repository
export FELIX_HOME=/home/$USERNAME/felix-framework-2.0.4
export JAVA_HOME=/usr/lib/jvm/java-6-sun-1.6.0.15

su $USERNAME -c "svn cat http://opencast.jira.com/svn/MH/trunk/pom.xml > /home/$USERNAME/capture-agent/pom.xml"
su $USERNAME -c "svn up /home/$USERNAME/capture-agent/modules/*"
su $USERNAME -c "svn up /home/$USERNAME/capture-agent/docs"

REV=`su $USERNAME -c "svn info /home/$USERNAME/capture-agent/modules/matterhorn-capture-agent-impl | grep Revision"`

cd /home/$USERNAME/capture-agent
su $USERNAME -c "mvn clean install -Pcapture -DdeployTo=${FELIX_HOME}/load | mail -s \"testopencast $REV `date`\" example@example.com"

/sbin/reboot now
