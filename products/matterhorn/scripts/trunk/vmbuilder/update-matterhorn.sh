#!/bin/bash

#stop felix
/home/opencast/shutdown.sh
cd /home/opencast/opencast
# update from svn
svn update
# build matterhorn
mvn clean install -DdeployTo=/usr/local/felix-framework-2.0.1/load
# creating backup of configuration
tar -czf /home/opencast/felix-config-backup.tar.gz /usr/local/felix-framework-2.0.1/conf/ 
# update felix configuration
cp -rf docs/felix/config/ /usr/local/felix-framework-2.0.1/conf/
cd /home/opnecast
# restart felix
/home/opencast/startup.sh
