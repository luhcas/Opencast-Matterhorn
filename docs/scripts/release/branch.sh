#!/bin/bash

#
# Branch a Module
#
BRANCH_OLD=0.7-SNAPSHOT
BRANCH_VER=0.7
JIRA_TKT=MH-3218

WORK_DIR=/Users/mtrehan/Matterhorn/svn

SVN_DIR=$WORK_DIR/$JIRA_TKT
SVN_URL=https://opencast.jira.com/svn/MH
TRUNK_URL=$SVN_URL/trunk
BRANCH_URL=$SVN_URL/branches/$BRANCH_VER

svn copy $TRUNK_URL $BRANCH_URL -m "$JIRA_TKT Creating $BRANCH_VER Branch"

cd $WORK_DIR

svn co $BRANCH_URL $JIRA_TKT

cd $SVN_DIR

echo "Main:"

sed "1,\$s/\>$BRANCH_OLD\</\>$BRANCH_VER\</" $SVN_DIR/pom.xml >/tmp/mh-branch-pom.xml
cp /tmp/mh-branch-pom.xml $SVN_DIR/pom.xml

for i in modules/matterhorn-*
do
    echo " Module: $i"

    if [ -f $SVN_DIR/$i/pom.xml ]; then
        echo " $BRANCH_VER: $i"
        sed "1,\$s/\>$BRANCH_OLD\</\>$BRANCH_VER\</" $SVN_DIR/$i/pom.xml >/tmp/mh-branch-pom.xml
        cp /tmp/mh-branch-pom.xml $SVN_DIR/$i/pom.xml
        sleep 1
    fi
done

svn commit -m "$JIRA_TKT Updated pom.xml files to reflect correct version"
