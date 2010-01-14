#! /bin/sh
. /lib/lsb/init-functions
. /etc/default/rcS

PATH="/sbin:/bin:/usr/bin"

log_begin_msg 'Running opencast scripts...'
if [ ! -e /home/opencast/firstboot.done ]; then
  cd /home/opencast
  /usr/bin/sudo touch /home/opencast/firstboot.done
  /usr/bin/sudo /home/opencast/external_tools.sh
fi

# add whatever commands to run a boot time here
#
#

log_end_msg 0

