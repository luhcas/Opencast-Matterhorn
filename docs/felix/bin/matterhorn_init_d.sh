#!/bin/sh
### BEGIN INIT INFO
# Provides:          opencast matterhorn
# Required-Start:    $remote_fs $syslog
# Required-Stop:     $remote_fs $syslog
# Default-Start:     2 3 4 5
# Default-Stop:      1
# Short-Description: lecture recording and management system
### END INIT INFO
# /etc/init.d/matterhorn
#

set -e

##
# These variables are set in the configuration scripts.
##
#eg:  /opt/matterhorn
MATTERHORN=/opt/matterhorn
#eg:  /opt/matterhorn/felix, or $MATTERHORN/felix
export FELIX_HOME=$MATTERHORN/felix
#eg:  /opt/matterhorn/capture-agent, or $MATTERHORN/capture-agent
CA=$CA_DIR
#eg:  Commonly opencast or matterhorn.  Can also be your normal user if you are testing.
MATTERHORN_USER=$USERNAME
export M2_REPO=/home/$USERNAME
#Enable this if this machine is a CA.  This will enable capture device autoconfiguration.
IS_CA=false

NAME=matterhorn
PATH=/sbin:/bin:/usr/sbin:/usr/bin:$FELIX_HOME

case "$1" in
  start)
    echo -n "Starting Matterhorn: " 
    if $IS_CA ; then
        $CA/device_config.sh
        if [ -d $CA/epiphan_driver -a -z "$(lsmod | grep vga2usb)" ]; then
                make -C $CA/epiphan_driver load
        fi
    fi

# copy away the gogo shell files otherwise matterhorn can not run in background
    if [ ! -d $FELIX/backup ]; then 
      mkdir $FELIX/backup
    fi
    if [ -f $FELIX/bundle/org.apache.felix.gogo.* ]; then
      mv -f $FELIX/bundle/org.apache.felix.gogo.* $FELIX/backup/
    fi

# Make sure matterhorn bundles are reloaded
    if [ -d "$FELIX_CACHE" ]; then
      echo "Removing cached matterhorn bundles from $FELIX_CACHE"
      for bundle in `find "$FELIX_CACHE" -type f -name bundle.location | xargs grep --files-with-match -e "file:" | sed -e s/bundle.location// `; do
        rm -r $bundle
      done
    fi
    
    cd $FELIX
    su -c  "nohup $FELIX_HOME/bin/start_matterhorn.sh & > /dev/null 2>&1" $MATTERHORN_USER
    echo "done." 
    ;;
  stop)
    echo -n "Stopping Matterhorn: " 
    MATTERHORN_PID=`ps aux | awk '/felix.jar/ && !/awk/ {print $2}'`
    if [ -z $MATTERHORN_PID ]; then
      echo "Matterhorn already stopped"
      exit 1
    fi

    kill $MATTERHORN_PID

    sleep 10

    MATTERHORN_PID=`ps aux | awk '/felix.jar/ && !/awk/ {print $2}'`
    if [ ! -z $MATTERHORN_PID ]; then
      echo "Hard killing since felix ($MATTERHORN_PID) seems unresponsive to regular kill"
    
      kill -9 $MATTERHORN_PID
    fi


    if $IS_CA ; then
        if [ -d $CA/epiphan_driver -a -z "$(lsmod | grep vga2usb)" ]; then
                make -C $CA/epiphan_driver unload
        fi
    fi

    echo "done."
    ;;
  restart)
    $0 stop
    $0 start
    ;;
  status)
    MATTERHORN_PID=`ps aux | awk '/felix.jar/ && !/awk/ {print $2}'`
    if [ -z $MATTERHORN_PID ]; then
      echo "Matterhorn is not running"
      exit 0
    else
      echo "OpenCast Matterhorn is running"
      exit 0
    fi   
    ;;
  *)
    echo "Usage: /etc/init.d/$NAME {start|stop|restart|status}"
    exit 1
    ;;
esac

exit 0
