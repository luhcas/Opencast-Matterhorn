#! /bin/sh

###################################
# Set matterhorn to start on boot #
###################################

# Checks this script is being run from install.sh
if [[ ! $INSTALL_RUN ]]; then
    echo "You shouldn't run this script directly. Please use the install.sh instead"
    exit 1
fi

echo """
#! /bin/sh
#This file controls the matterhorn felix daemon
. /lib/lsb/init-functions

case \"\$1\" in
  start)
        export FELIX_HOME=$FELIX_HOME
        export M2_REPO=$M2_REPO
        export JAVA_HOME=$JAVA_HOME
        make -C $CA_DIR/$VGA2USB_DIR reload > /dev/null
        $CA_DIR/device_config.sh > /dev/null
        log_daemon_msg \"Starting Matterhorn Felix instance\" \"felix\"
        if start-stop-daemon -b -c $USERNAME:$USERNAME --start --quiet --oknodo --pidfile \$FELIX_HOME/matterhorn.pid -a ${FELIX_HOME}/bin/start_matterhorn.sh ; then
                log_end_msg 0
        else
                log_end_msg 1
        fi
	;;
  stop)
        log_daemon_msg \"Stopping Matterhorn Felix instance\" \"felix\"
        start-stop-daemon --stop --oknodo --quiet --user $USERNAME --pidfile \$FELIX_HOME/matterhorn.pid --retry=TERM/30/KILL/5
        local status=\$?
        rm -f \$FELIX_HOME/matterhorn.pid
        log_end_msg \$status
        ;;
esac
""" > $STARTUP_SCRIPT

# Set the appropriate permissions
chown root:root $STARTUP_SCRIPT
chmod 755 $STARTUP_SCRIPT
update-rc.d "${STARTUP_SCRIPT##*/}" defaults
