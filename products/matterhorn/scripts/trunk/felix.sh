#!/bin/bash
# core
WORKING_DIR=/Users/mtrehan/Matterhorn
JIRA_TKT=MH-1432

FELIX_CONF=$WORKING_DIR/$JIRA_TKT/conf/config.properties

for fname in `grep "^ file:" $FELIX_CONF | awk '{split($0, a, "/"); print a[6]}'`
do
echo $fname
for jar in `find /Users/mtrehan/.m2/repository/ -name "$fname"`; do
  echo $jar
  cp $jar /Users/mtrehan/Matterhorn/$JIRA_TKT/lib/.
done;
done
