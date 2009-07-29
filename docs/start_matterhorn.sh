##
# Configure these variables to match your environment
##
FELIX='/Users/josh/dev/felix-1.8.0'
M2_REPO='/Users/josh/.m2/repository'

##
# Only change the line below if you want to customize the server
##
rm -rf $FELIX/felix-cache/*
cd $FELIX
java -Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,address=9191,server=y,suspend=n -DM2_REPO=$M2_REPO -Dfelix.fileinstall.dir=$FELIX/load -jar $FELIX/bin/felix.jar $FELIX/felix-cache

