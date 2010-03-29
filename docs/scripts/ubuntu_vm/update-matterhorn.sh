#!/bin/bash

cd /opt/matterhorn/matterhorn_trunk

# update from svn
if [ -n $1 ]
then
  svn update -r $1
else
  svn update
fi

#stop felix
sudo /home/opencast/shutdown.sh

# Clean old jars
rm -rf /opt/matterhorn/felix/load

# build matterhorn
mvn clean install -DskipTests -DdeployTo=/opt/matterhorn/felix/load

# creating backup of configuration
tar -czf /home/opencast/felix-config-backup.tar.gz /opt/matterhorn/felix/conf/ 

# update felix configuration
cp -rf docs/felix/conf/* /opt/matterhorn/felix/conf/
cd /home/opencast

# update felix config (url)
MY_IP=`ifconfig | grep "inet addr:" | grep -v 127.0.0.1 | awk '{print $2}' | cut -d':' -f2`
sed -i "s/http:\/\/localhost:8080/http:\/\/$MY_IP:8080/" /opt/matterhorn/felix/conf/config.properties
# update capture properties
sed -i "s/http:\/\/localhost:8080/http:\/\/$MY_IP:8080/" /opencast/config/capture.properties

# restart felix
/home/opencast/startup.sh
