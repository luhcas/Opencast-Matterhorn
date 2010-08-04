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
MATTERHORN=$MATTERHORN_HOME
#eg:  /opt/matterhorn/felix, or $MATTERHORN/felix
FELIX=$FELIX_HOME
#eg:  Commonly opencast or matterhorn.  Can also be your normal user if you are testing.
MATTERHORN_USER=$USERNAME
#Enable this if this machine is a CA.  This will enable capture device autoconfiguration.
IS_CA=false

##
# To enable the debugger on the vm, enable all of the following options
##

DEBUG_PORT="8000"
DEBUG_SUSPEND="n"
#DEBUG_OPTS="-Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,address=$DEBUG_PORT,server=y,suspend=$DEBUG_SUSPEND"

##
# Only change the line below if you want to customize the server
##

LOGDIR=$MATTERHORN/logs
MAVEN_ARG="-DM2_REPO=$M2_REPO"
FELIX_FILEINSTALL_OPTS="-Dfelix.fileinstall.dir=$FELIX/load"
PAX_CONFMAN_OPTS="-Dbundles.configuration.location=$FELIX/conf"
PAX_LOGGING_OPTS="-Dorg.ops4j.pax.logging.DefaultServiceLog.level=WARN -Dopencast.logdir=$LOGDIR"
UTIL_LOGGING_OPTS="-Djava.util.logging.config.file=$FELIX/conf/services/java.util.logging.properties"
GRAPHICS_OPTS="-Djava.awt.headless=true -Dawt.toolkit=sun.awt.HeadlessToolkit"

FELIX_CACHE="$FELIX/felix-cache"

DAEMON="/usr/bin/java"
OPTS="$DEBUG_OPTS $GRAPHICS_OPTS $MAVEN_ARG $FELIX_FILEINSTALL_OPTS $PAX_CONFMAN_OPTS $PAX_LOGGING_OPTS $UTIL_LOGGING_OPTS $CXF_OPTS -jar $FELIX/bin/felix.jar $FELIX_CACHE"
NAME=matterhorn
PATH=/sbin:/bin:/usr/sbin:/usr/bin:$FELIX
LOGFILE=/var/log/matterhorn.log
CHROOT=/var/run/matterhorn/empty
CHDIR=$FELIX

test -x $DAEMON || exit 0
. /lib/lsb/init-functions

if [ ! -e "$LOGFILE" ]
then
  touch $LOGFILE
  chmod 640 $LOGFILE
  chown root:adm $LOGFILE
fi

case "$1" in
  start)
    log_begin_msg "Starting OpenCast Matterhorn: $NAME"
    if $IS_CA ; then
        $MATTERHORN/capture-agent/device_config.sh
        if [ -d $MATTERHORN/capture-agent/epiphan_driver ]; then
                make -C $MATTERHORN/capture-agent/epiphan_driver load
        fi
    fi

    [ -d ${CHROOT} ] || mkdir -p ${CHROOT}
    [ -d ${CHDIR} ] || mkdir -p ${CHDIR}
    start-stop-daemon --start --background -m --oknodo --chuid $MATTERHORN_USER --chdir $CHDIR --pidfile /var/run/matterhorn/matterhorn.pid --exec $DAEMON -- $OPTS && log_end_msg 0 || log_end_msg 1
    ;;
  stop)
    log_begin_msg "Stopping OpenCast Matterhorn: $NAME"
    if $IS_CA ; then
        if [ -d $MATTERHORN/capture-agent/epiphan_driver ]; then
                make -C $MATTERHORN/capture-agent/epiphan_driver unload
        fi
    fi

    start-stop-daemon --stop --pidfile /var/run/matterhorn/matterhorn.pid --oknodo --exec $DAEMON && log_end_msg 0 || log_end_msg 1
    rm -f /var/run/matterhorn/matterhorn.pid
    ;;
  restart)
    $0 stop
    $0 start
    ;;
  reload|force-reload)
    log_begin_msg "Reloading $NAME configuration files"
    start-stop-daemon --stop --pidfile /var/run/matterhorn/matterhorn.pid --signal 1 --exec $DAEMON && log_end_msg 0 || log_end_msg 1
    ;;
  status)
    pid=`cat /var/run/matterhorn/matterhorn.pid 2>/dev/null` || true
    if test ! -f /var/run/matterhorn/matterhorn.pid -o -z "$pid"; then
      echo "OpenCast Matterhorn is not running"
      exit 3
    fi
    if ps "$pid" >/dev/null 2>&1; then
      echo "OpenCast Matterhorn is running"
      exit 0
    else
      echo "OpenCast Matterhorn is not running"
      exit 1
    fi
    ;;
  *)
    log_success_msg "Usage: /etc/init.d/$NAME {start|stop|restart|reload|status}"
    exit 1
    ;;
esac

exit 0
