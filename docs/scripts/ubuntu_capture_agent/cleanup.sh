#! /bin/bash
#
# Cleanup all traces of the capture agent

# get the user associated with the capture agent build
echo -n "Which user runs the capture agent? (WARNING: This user will be removed) "
read USER

id $USER &> /dev/null
if [ "$?" -ne 0 ]; then
  echo "User $USER does not exist."
  exit
fi

# double check to make sure they aren't losing any vital information
echo -n "Are you sure you want to remove the Matterhorn Capture Agent and user $USER from your system? (y/N) "
read RESPONSE

if [ "$RESPONSE" != "y" ]; then
  exit
fi

# remove vga2usb driver
lsmod | grep "^vga2usb" &> /dev/null
if [ "$?" -ne 0 ]; then
  sudo rmmod vga2usb
fi

# restore appropriate sources.list
sudo mv /etc/apt/sources.list.backup /etc/apt/sources.list &> /dev/null

# remove the configuration that starts matterhorn on boot
sudo rm -f /etc/init/matterhorn.conf

# remove the udev rules that manage the devices
sudo rm -f /etc/udev/rules.d/95-perso.rules

# remove the capture storage directory
sudo rm -rf /opencast

# remove the jv4linfo library
sudo rm -f /usr/lib/libjv4linfo.so

# remove the user, all with all the
sudo userdel -r -f $USER

echo "Done uninstalling Matterhorn Capture Agent." 
