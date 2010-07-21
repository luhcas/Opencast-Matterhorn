#! /bin/bash

###################################
# Set matterhorn to start on boot #
###################################

# Checks this script is being run from install.sh
if [[ ! $INSTALL_RUN ]]; then
    echo "You shouldn't run this script directly. Please use the install.sh instead"
    exit 1
fi

echo """
#This file controls the matterhorn felix daemon
. /lib/lsb/init-functions

case \"\$1\" in
  start)
        export FELIX_HOME=$FELIX_HOME
        export M2_REPO=$M2_REPO
        export JAVA_HOME=$JAVA_HOME
        make -C $CA_DIR/$VGA2USB_DIR reload
        $CA_DIR/device_config.sh
        log_daemon_msg \"Starting Matterhorn Felix instance\" \"felix\"
        if start-stop-daemon -b -c $USERNAME:$USERNAME --start --quiet --oknodo -m --pidfile /var/run/matterhorn.pid --exec ${FELIX_HOME}/bin/start_matterhorn.sh ; then
                log_end_msg 0
        else
                log_end_msg 1
        fi
	;;
  stop)
	log_daemon_msg \"Shutting down Matterhorn Felix instance\" \"felix\"
	${FELIX_HOME}/bin/shutdown_matterhorn.sh
	log_end_msg 0
        ;;
esac
""" > $STARTUP_SCRIPT

# Set the appropriate permissions
chown root:root $STARTUP_SCRIPT
chmod 755 $STARTUP_SCRIPT
update-rc.d `basename $STARTUP_SCRIPT` defaults
