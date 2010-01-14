#!/bin/bash

#
# Define some constants
#
MH_VERSION=0.4
JIRA_TKT=MH-1432
FELIX_VER=felix-framework-2.0.1

WORKING_DIR=/Users/mtrehan/Matterhorn

TEMPLATE_DIR=$WORKING_DIR/template

MATTERHORN_DIR=$WORKING_DIR/$JIRA_TKT

MATTERHORN_BIN_DIR=$MATTERHORN_DIR/bin
MATTERHORN_LIB_DIR=$MATTERHORN_DIR/lib
MATTERHORN_BUNDLE_DIR=$MATTERHORN_DIR/bundles
MATTERHORN_CONF_DIR=$MATTERHORN_DIR/conf
MATTERHORN_LICENSE_DIR=$MATTERHORN_DIR/licenses
MATTERHORN_INSTALL_DIR=$MATTERHORN_DIR/install
MATTERHORN_FELIX_DIR=$MATTERHORN_DIR/$FELIX_VER

FELIX_CONF=$MATTERHORN_DIR/conf/config.properties

#SVN_URL=http://source.opencastproject.org/svn/products/matterhorn/branches/$JIRA_TKT
SVN_URL=http://source.opencastproject.org/svn/products/matterhorn/trunk

EXPORT_DIR=$WORKING_DIR/svn
EXPORT_NAME=$JIRA_TKT

SVN_DIR=$EXPORT_DIR/$EXPORT_NAME

#
# Check for maven repository configuration
#
if [ -z "$M2_REPO" ]; then
  echo "Maven repository is undefined"
  exit 1;
fi

#
# Check for java configuration
#
if [ -z "$JAVA_HOME" ]; then
  echo "Java home is undefined"
  exit 1;
fi

#
# Make sure the deployment environment is set up
#
if [ ! -d "$WORKING_DIR" ]; then
  echo "Working directory does not exist.  Please check and re-run this script."
  exit 1;
fi

if [ ! -d "$TEMPLATE_DIR" ]; then
  echo "Template directory does not exist.  Please check and re-run this script."
  exit 1;
fi

if [ -d "$MATTERHORN_DIR" ]; then
  cd $WORKING_DIR
  rm -rf $JIRA_TKT
fi

cd $WORKING_DIR
mkdir $JIRA_TKT $MATTERHORN_LIB_DIR $MATTERHORN_BUNDLE_DIR $MATTERHORN_CONF_DIR

cp -R $TEMPLATE_DIR/bin $MATTERHORN_BIN_DIR
cp -R $TEMPLATE_DIR/licenses $MATTERHORN_LICENSE_DIR
cp -R $TEMPLATE_DIR/install $MATTERHORN_INSTALL_DIR
cp -R $TEMPLATE_DIR/$FELIX_VER $MATTERHORN_FELIX_DIR

cp $TEMPLATE_DIR/README $MATTERHORN_DIR/.
cp $TEMPLATE_DIR/LICENSE $MATTERHORN_DIR/.
cp $TEMPLATE_DIR/CHANGES $MATTERHORN_DIR/.
cp $TEMPLATE_DIR/install.sh $MATTERHORN_DIR/.

#
# Refresh the source code
#
if [ ! -d "$EXPORT_DIR" ]; then
  mkdir $EXPORT_DIR
fi
cd $EXPORT_DIR
#rm -rf $EXPORT_NAME
#svn export $SVN_URL $EXPORT_NAME

#
# Change file ownership/group to tomcat
#
chown -R mtrehan $EXPORT_DIR/$EXPORT_NAME
chown -R mtrehan $MATTERHORN_DIR
chgrp -R staff $EXPORT_DIR/$EXPORT_NAME
chgrp -R staff $MATTERHORN_DIR

# Update the version number

cd $SVN_DIR

sed '1,$s/\>0.1-SNAPSHOT\</\>0.5-RC\</' $SVN_DIR/pom.xml >/tmp/branch-pom.xml
cp /tmp/branch-pom.xml $SVN_DIR/pom.xml

for i in opencast-*
do
  echo " Module: $i"

  if [ -f $SVN_DIR/$i/pom.xml ]; then
    echo " 0.5-RC: $i"
    sed '1,$s/\>0.1-SNAPSHOT\</\>0.5-RC\</' $SVN_DIR/$i/pom.xml >/tmp/branch-pom.xml
    cp /tmp/branch-pom.xml $SVN_DIR/$i/pom.xml
    sleep 1
  fi

done

#
# Build
#
cd $EXPORT_DIR/$EXPORT_NAME
export MAVEN_OPTS="$MAVEN_OPTS"
mvn install -DskipTests -DdeployTo=$MATTERHORN_BUNDLE_DIR

#
# Remove bundles and files that are no longer used (older than an hour)
#
find $MATTERHORN_BUNDLE_DIR -mmin +60 -exec rm -f {} \;

#
# Adjust and deploy the configuration. Modify the config file included in the
# source so it runs on port 8081
#
rsync -av --delete $SVN_DIR/docs/felix/conf/ $MATTERHORN_CONF_DIR

#
# Update Felix config and build lib
#
sed '1,$s/file:\${M2_REPO}.*\//file:\/usr\/local\/felix\/lib\//' $SVN_DIR/docs/felix/conf/config.properties > $MATTERHORN_CONF_DIR/config.properties

for fname in `grep "^ file:" $FELIX_CONF | awk '{split($0, a, "/"); print a[6]}'`
do
  echo $fname
  for jar in `find $M2_REPO/repository/ -name "$fname"`; do
    echo $jar
    cp $jar $MATTERHORN_DIR/lib/.
  done;
done

MH_DIR=matterhorn-$MH_VERSION
MH_TAR=$MH_DIR.tar
MH_TARGZ=$MH_TAR.gz

echo "Archiving..."
cd /Users/mtrehan/Matterhorn
mv $JIRA_TKT $MH_DIR

tar -cvf $MH_TAR ./$MH_DIR
gzip $MH_TAR

echo "Signing..."
gpg --print-md MD5 $MH_TARGZ > $MH_TARGZ.md5
gpg --print-md SHA1 $MH_TARGZ > $MH_TARGZ.sha1
gpg --armor --output $MH_TARGZ.asc --detach-sig $MH_TARGZ

mv $MH_DIR $JIRA_TKT

echo "Done."
