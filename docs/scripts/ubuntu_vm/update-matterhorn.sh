#!/bin/bash

#stop felix
sudo /home/opencast/shutdown.sh
cd /opt/matterhorn/matterhorn_trunk
# update from svn
svn update
# build matterhorn
mvn clean install -DdeployTo=/opt/matterhorn/felix/load
# creating backup of configuration
tar -czf /home/opencast/felix-config-backup.tar.gz /opt/matterhorn/felix/conf/ 
# update felix configuration
sudo p -rf docs/felix/config/ /opt/matterhorn/felix/conf/
cd /home/opencast
# restart felix
/home/opencast/startup.sh
