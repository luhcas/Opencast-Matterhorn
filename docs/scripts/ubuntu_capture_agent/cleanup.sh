#! /bin/bash

###########################################
# Cleanup all traces of the capture agent #
###########################################

USER=                                                    # Matterhorn's user name
SRC_LIST=                                                # Location for sources.list
SRC_LIST_BKP=                                            # Location for the backup file
OC_DIR=                                                  # Location of matterhorn files
CA_DIR=                                                  # The directory where the capture agent files live
STARTUP_SCRIPT=                                          # Path to the matterhorn startup script
RULES_FILE=                                              # Path to the file specifying rules for the installed devices
PKG_LIST=                                                # List of packages to be uninstalled

# Checks if this script is being run with root privileges, exiting if it doesn't
if [[ `id -u` -ne 0 ]]; then
    echo "This script requires root privileges. Please run it with the sudo command or log in to the root user and try again"
    exit 1
fi

# Double check to make sure they aren't losing any vital information
read -p "Are you sure you want to remove the Matterhorn Capture Agent and user $USER from your system? (no) " response
while [[ -z $(echo "$response" | grep -i 'yes') && -z $(echo "${response:-no}" | grep -i 'no') ]]; do
    read -p "Please write yes or no: (no) " response
done

if [[ $(echo "${response:-no}" | grep -i '^[nN]') ]]; then
  exit 0
fi

# Remove vga2usb driver
rmmod vga2usb 2> /dev/null

# Remove dependencies installed by the scripts
apt-get purge $PKG_LIST &> /dev/null
apt-get autoremove &> /dev/null

# Restore appropriate sources.list
mv $SRC_LIST.$SRC_LIST_BKP $SRC_LIST &> /dev/null
apt-get update

# Remove the configuration that starts matterhorn on boot
rm -f $STARTUP_SCRIPT

# Remove the udev rules that manage the devices
rm -f $RULES_FILE

# Remove the capture storage directory
rm -rf $OC_DIR

# Remove the jv4linfo library
rm -f /usr/lib/libjv4linfo.so

# Remove the CA_DIR directory
rm -rf $CA_DIR

# Remove the user and their home directory
read -p "Do you want to remove the matterhorn user (y/N)? " response
until [[ $(echo ${response:-no} | grep -i '^[yn]') ]]; do
    read -p "Please answer (y)es or (N)o: " response
done

if [[ $(echo ${response:-no} | grep -i '^y') ]]; then
    echo -n "Deleting user $USER... "
    userdel -r -f $USER &> /dev/null
    echo "Done"
fi

# Kills felix
kill -9 $(ps U matterhorn 2> /dev/null | grep java | cut -d ' ' -f 2) 2> /dev/null

echo -e "\n\nDone uninstalling Matterhorn Capture Agent.\n\n" 

# Prompts the user to reboot or not
read -p "Some matterhorn settings won't be completely removed until the system reboots. Do you wish to do it now (Y/n)? " response

while [[ -z "$(echo ${response:-Y} | grep -i '^[yn]')" ]]; do
    read -p "Please enter (Y)es or (n)o: " response
done

if [[ -n "$(echo ${response:-Y} | grep -i '^y')" ]]; then
    echo -e "\n\nRebooting... "
    reboot > /dev/null
else
    echo
    read -n 1 -s -p "Hit any key to exit..."
    clear
fi
