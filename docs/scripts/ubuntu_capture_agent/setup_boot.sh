#! /bin/bash

###################################
# Set matterhorn to start on boot #
###################################

# Checks this script is being run from install.sh
if [[ ! $INSTALL_RUN ]]; then
    echo "You shouldn't run this script directly. Please use the install.sh instead"
    exit 1
fi

ACTUAL_SCRIPT=$FELIX_HOME/bin/matterhorn_init_d.sh

ln -s $ACTUAL_SCRIPT $STARTUP_SCRIPT

sed -i "s#\$MATTERHORN_HOME#$OC_DIR#g" $ACTUAL_SCRIPT
sed -i "s#\$FELIX_HOME#$FELIX_HOME#g" $ACTUAL_SCRIPT
sed -i "s#\$USERNAME#$USERNAME#g" $ACTUAL_SCRIPT
sed -i "s#\$CA_DIR#$CA_DIR#g" $ACTUAL_SCRIPT
sed -i "s#\$M2_REPO#$M2_REPO#g" $ACTUAL_SCRIPT
sed -i "s/IS_CA=false/IS_CA=true/g" $ACTUAL_SCRIPT

# Set the appropriate permissions
chown root:root $STARTUP_SCRIPT
chmod 755 $STARTUP_SCRIPT
update-rc.d "${STARTUP_SCRIPT##*/}" defaults 99 01
