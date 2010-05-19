#! /bin/bash

###################################
# Set matterhorn to start on boot #
###################################

# Checks this script is being run from install.sh
if [[ ! $INSTALL_RUN ]]; then
    echo "You shouldn't run this script directly. Please use the install.sh instead"
    exit 1
fi

echo "# Start the matterhorn capture agent" > $STARTUP_SCRIPT

# This section sets up some enviroment variables
echo "env FELIX_HOME=${FELIX_HOME}" >> $STARTUP_SCRIPT
echo "env M2_REPO=${M2_REPO}" >> $STARTUP_SCRIPT
echo "env JAVA_HOME=${JAVA_HOME}" >> $STARTUP_SCRIPT

# This specifies in which runlevel shall and shall not be run this script
echo "start on runlevel [2345]" >> $STARTUP_SCRIPT
echo "stop on runlevel [!2345]" >> $STARTUP_SCRIPT

# Here is a list of required applications that should be already loaded before this script runs
echo "expect fork" >> $STARTUP_SCRIPT

# This is the body of the script
################################
echo "script" >> $STARTUP_SCRIPT
# Reload the epiphan drivers
echo "make -C $CA_DIR/$VGA2USB_DIR reload" >> $STARTUP_SCRIPT
# Set up the video devices
echo "$CA_DIR/$CONFIG_SCRIPT" >> $STARTUP_SCRIPT
# Starts matterhorn
echo "su matterhorn -c \"exec /bin/bash ${FELIX_HOME}/bin/start_matterhorn.sh\" &" >> $STARTUP_SCRIPT

echo "end script" >> $STARTUP_SCRIPT


# Set the appropriate permissions
chown root:root $STARTUP_SCRIPT
