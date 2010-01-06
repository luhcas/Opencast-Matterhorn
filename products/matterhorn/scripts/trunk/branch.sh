#!/bin/bash

#
# Branch a Module
#
JIRA_TKT=MH-1432
WORK_DIR=/Users/mtrehan/Matterhorn/svn/$JIRA_TKT

cd $WORK_DIR

for i in \
opencast-admin-ui \
opencast-analysis-service-api \
opencast-authentication-api \
opencast-build-tools \
opencast-captions-service-impl \
opencast-capture-admin-service-api \
opencast-capture-admin-service-impl \
opencast-capture-service-api \
opencast-capture-service-impl \
opencast-composer-service-api \
opencast-composer-service-impl \
opencast-conductor \
opencast-conductor-api \
opencast-conductor-service-api \
opencast-db \
opencast-demo \
opencast-distribution-service-api \
opencast-distribution-service-local-impl \
opencast-engage-player \
opencast-engage-repository-service-api \
opencast-engage-service-api \
opencast-engage-service-impl \
opencast-http \
opencast-ingest-service-api \
opencast-ingest-service-impl \
opencast-ingestui-service-api \
opencast-ingestui-service-impl \
opencast-inspection-service-api \
opencast-inspection-service-impl \
opencast-maven-plugin \
opencast-media \
opencast-notification-service-api \
opencast-runtime-info-ui \
opencast-runtime-tools \
opencast-scheduler-api \
opencast-scheduler-impl \
opencast-search-service-api \
opencast-search-service-impl \
opencast-stream \
opencast-test-harness \
opencast-util \
opencast-workflow-service-api \
opencast-workflow-service-impl \
opencast-workflow-ui \
opencast-working-file-repository-service-api \
opencast-working-file-repository-service-impl \
opencast-workspace-api \
opencast-workspace-impl
do
    echo " Module: $i"
    svn copy https://source.opencastproject.org/svn/modules/$i/trunk https://source.opencastproject.org/svn/modules/$i/branches/0.4-RC -m "MH-1432 Recreating 0.4 Release Candidate"

#  if [ -f $WORK_DIR/$i/pom.xml ]; then
#    echo " 0.4-RC: $i"
#    sed '1,$s/\>0.1-SNAPSHOT\</\>0.4-RC\</' $WORK_DIR/$i/pom.xml >/tmp/manjit-pom.xml
#    cp /tmp/manjit-pom.xml $WORK_DIR/$i/pom.xml
#    sleep 1
#  fi

done
