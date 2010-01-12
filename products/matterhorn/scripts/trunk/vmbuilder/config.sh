#!/bin/sh

#This script should be run as the opencast user, not sudo

#location of the opencast source files
export OC=/home/opencast/opencast
#location of felix
export FELIX_HOME=/home/opencast/felix-framework-2.0.1
#location of the local maven repo
export M2_REPO=/home/opencast/.m2/repository
#url to the version of opencast you want to build
export OC_URL=http://source.opencastproject.org/svn/products/matterhorn/trunk/
#url to the felix framework, use version 2.0 or beyond
export FELIX_URL=http://apache.mirror.iweb.ca/felix/felix-framework-2.0.1.tar.gz
#where java installs into
export JAVA_HOME=/usr/lib/jvm/java-6-sun
#maven needs a bit more memory
export MAVEN_OPTS="-Xms256m -Xmx512m -XX:PermSize=64m -XX:MaxPermSize=128m"

#write environment variables to login file
echo "export OC=$OC" >> .bashrc
echo "export FELIX_HOME=$FELIX_HOME" >> .bashrc
echo "export M2_REPO=$M2_REPO" >> .bashrc
echo "export OC_URL=$OC_URL" >> .bashrc
echo "export FELIX_URL=$FELIX_URL" >> .bashrc
echo "export JAVA_HOME=$JAVA_HOME" >> .bashrc
echo "export MAVEN_OPTS=\"$MAVEN_OPTS\"" >> .bashrc

#add a couple of helpful aliases to the bashrc
#todo: we shouldn't have to skip tests, but they are buggy
echo "alias buildmh='mvn install -DdeployTo=$FELIX_HOME/load -DskipTests'" >> .bashrc
echo "alias buildcleanmh='mvn clean install -DdeployTo=$FELIX_HOME/load -DskipTests'" >> .bashrc
echo "alias startmh='$OC/docs/felix/bin/start_matterhorn.sh'" >> .bashrc

#put aliases in current environment
alias buildmh='mvn install -DdeployTo=$FELIX_HOME/load -DskipTests'
alias buildcleanmh='mvn clean install -DdeployTo=$FELIX_HOME/load -DskipTests'
alias startmh='$OC/docs/felix/bin/start_matterhorn.sh'

#check out the matterhorn build
#todo: lock this to a particular revision number
#mkdir $OC
#svn co $OC_URL $OC

#check out and install felix
curl $FELIX_URL | tar xz
mkdir ${FELIX_HOME}/load

#build matterhorn
cd $OC
buildcleanmh

#install felix config files
cp -r $OC/docs/felix/conf/* $FELIX_HOME/conf/

#start matterhorn
startmh

#todo: write an updated message of the day file
#todo: set clock to sync with time server?  important for capture agents?

