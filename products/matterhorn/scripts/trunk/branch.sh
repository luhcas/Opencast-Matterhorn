#/bin/bash

#
# Branch a Module
#
JIRA_TKT=MH-1991
WORK_DIR=/Users/mtrehan/Matterhorn/svn/$JIRA_TKT

cd $WORK_DIR

for i in opencast-*
do
    echo " Module: $i"
    svn copy https://source.opencastproject.org/svn/modules/$i/trunk https://source.opencastproject.org/svn/modules/$i/branches/0.4-RC -m "MH-1432 Recreating 0.4 Release Candidate"

#  if [ -f $WORK_DIR/$i/pom.xml ]; then
#    echo " 0.5-RC: $i"
#    sed '1,$s/\>0.1-SNAPSHOT\</\>0.5-RC\</' $WORK_DIR/$i/pom.xml >/tmp/branch-pom.xml
#    cp /tmp/branch-pom.xml $WORK_DIR/$i/pom.xml
#    sleep 1
#  fi

done
