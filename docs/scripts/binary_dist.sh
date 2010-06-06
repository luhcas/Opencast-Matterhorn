#!/bin/bash

# SETTINGS
# The source directory
SRCDIR=/Users/josh/dev/src/mh_jira
VERSION=r7341-bin-dist
LOCAL_MH_BUNDLES=/Applications/Matterhorn_all/matterhorn
TEMPDIR=felix-framework-2.0.5

# Get an exploded, vanilla felix, downloading if necessary
[ -f ./felix_temp.zip ] || curl -o felix_temp.zip http://repo2.maven.org/maven2/org/apache/felix/org.apache.felix.main.distribution/2.0.5/org.apache.felix.main.distribution-2.0.5.zip
unzip -q felix_temp.zip
#rm felix_temp.zip

# Copy Matterhorn files to felix
cp -R $SRCDIR/docs/felix/bin $TEMPDIR
cp -R $SRCDIR/docs/felix/conf $TEMPDIR
cp -R $SRCDIR/docs/felix/load $TEMPDIR
cp -R $SRCDIR/docs/felix/inbox $TEMPDIR
cp -R $SRCDIR/docs/scripts/3rd_party_tools $TEMPDIR

# Clean up unnneeded felix files
rm $TEMPDIR/pom.xml
rm $TEMPDIR/assembly.xml
rm $TEMPDIR/LICENSE
rm -rf $TEMPDIR/doc

# Copy the 3rd party osgi bundles to the lib directory
mkdir $TEMPDIR/lib
for i in `sed '/file:.*\//!d' $SRCDIR/docs/felix/conf/config.properties | sed "s#file:.*}#$HOME/.m2/repository#" | sed s/.$//` ; do cp $i $TEMPDIR/lib; done

# Fix the path to the 3rd party osgi bundles in config.properties
sed -i '' 's# file:.*/# file:lib/#g' $TEMPDIR/conf/config.properties

# Copy matterhorn bundles
mkdir $TEMPDIR/matterhorn
cp $LOCAL_MH_BUNDLES/*.jar $TEMPDIR/matterhorn

# Copy matterhorn license
cp $SRCDIR/docs/licenses.txt $TEMPDIR

# Rename and zip felix
mv $TEMPDIR matterhorn-$VERSION
zip -r -q matterhorn-$VERSION.zip matterhorn-$VERSION

# Clean up
#rm -rf $TEMPDIR
