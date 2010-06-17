#!/bin/bash

######################################
# Configure Matterhorn Capture Agent #
######################################

# Variables section ####################################################################################################################

# General variables

# Maximum number of attempts to stablish the matterhorn user password
MAX_PASSWD_ATTEMPTS=3

# Default name for the matterhorn user 
export USERNAME=matterhorn
# Storage directory for the matterhorn-related files
export OC_DIR=/opt/matterhorn
# Name for the directory where the matterhorn-related files will be stored
export CA_DIR=$OC_DIR/capture-agent
# Directory where the source code will be downloaded to
export SOURCE=$CA_DIR/matterhorn-source

# Path from where this script is run initially
export START_PATH=$PWD
# Directory where this script will be run
export WORKING_DIR=/tmp/cainstallscript

# Root for the source code repository
export SVN_URL=http://opencast.jira.com/svn/MH
# Extension for the SVN_URL to reach the trunk
export TRUNK_URL=$SVN_URL/trunk
# Extension for the SVN_URL to reach the branches
export BRANCHES_URL=$SVN_URL/branches
# Extension for the SVN_URL to reach the tags
export TAGS_URL=$SVN_URL/tags

# Default URL from where scripts and java source will be dowloaded
export SRC_DEFAULT=$TRUNK_URL

# File containing the rules to be applied by udev to the configured devices -- not a pun!
export DEV_RULES=/etc/udev/rules.d/matterhorn.rules
# File name for the bash script under HOME containing the device configuration routine
export CONFIG_SCRIPT=device_config.sh
# Default value for the core url
export DEFAULT_INGEST_URL=http://localhost:8080
# Subdirectory under HOME where the epiphan driver will be downloaded to
export VGA2USB_DIR=epiphan_driver
# Location of the file 'sources.list'
export SRC_LIST=/etc/apt/sources.list
# Suffix to be appended to the backup file for sources.list
export BKP_SUFFIX=backup
# Path of the script which is set up to configure and run felix upon startup
export STARTUP_SCRIPT=/etc/init/matterhorn.conf
# URL of the default Ubuntu mirror where the packages will be downloaded from
export DEFAULT_MIRROR=http://archive.ubuntu.com/ubuntu
# URL of the default Ubuntu 'security' mirror
export DEFAULT_SECURITY=http://security.ubuntu.com/ubuntu
# URL of the default Ubuntu 'partner' mirror
export DEFAULT_PARTNER=http://archive.canonical.com/ubuntu

# Logging file                                                                                                                                               
export LOG_FILE=$START_PATH/install_info.txt

# The subsidiary scripts will check for this variable to check they are being run from here
export INSTALL_RUN=true


# Third-party dependencies variables
# Packages that are installed by default (one per line --please note the quotation mark at the end!!!)
export PKG_LIST="alsa-utils
v4l-conf
ivtv-utils
curl
maven2
sun-java6-jdk
subversion
wget
openssh-server
gcc gstreamer0.10-alsa
gstreamer0.10-plugins-base
gstreamer0.10-plugins-good
gstreamer0.10-plugins-ugly
gstreamer0.10-plugins-ugly-multiverse
gstreamer0.10-ffmpeg
ntp
acpid"

# Packages that require the user approval to be installed (one per line --please note the quotation mark at the end!!!)
export BAD_PKG_LIST="gstreamer0.10-plugins-bad gstreamer0.10-plugins-bad-multiverse"
# Reasons why each of the "bad" packages should be installed (one per line, in the same order as the bad packages)
export BAD_PKG_REASON="These packages provide support for h264 and mpeg2"

# 0-based index default option for the device flavor
export DEFAULT_FLAVOR=0
# Lists of flavors the user can choose from to assign to a certain device
export FLAVORS="presenter/source presentation/source audience/source indefinite/source"

# URL to download the epiphan driver
export EPIPHAN_URL=http://www.epiphan.com/downloads/linux

# Name of the file containing the felix files
export FELIX_FILENAME=org.apache.felix.main.distribution-2.0.4.tar.gz
# URL where the previous file can be fetched
export FELIX_URL=http://archive.apache.org/dist/felix/$FELIX_FILENAME
# Subdir under the user home where FELIX_HOME is
export FELIX_HOME=$OC_DIR/felix
# Path under FELIX_HOME where the general matterhorn configuration
export GEN_PROPS=$FELIX_HOME/conf/config.properties
# Path under FELIX_HOME where the capture agent properties are
export CAPTURE_PROPS=$FELIX_HOME/conf/services/org.opencastproject.capture.impl.ConfigurationManager.properties
# Directory UNDER FELIX HOME where the felix filex will be deployed
export DEPLOY_DIR=matterhorn

# Path to where the installed jvm's are
export JAVA_PREFIX=/usr/lib/jvm
# A regexp to filter the right jvm directory from among all the installed ones
# The chosen JAVA_HOME will be $JAVA_PREFIX/`ls $JAVA_PREFIX | grep $JAVA_PATTERN`
export JAVA_PATTERN=java-6-sun                                           
                                                                         
# Path to the maven2 repository, under the user home
export M2_SUFFIX=.m2/repository

# Default ntp server
export DEFAULT_NTP_SERVER=ntp.ubuntu.com
# Location for the ntp configuration files
export NTP_CONF=/etc/ntp.conf

# Location of the jv4linfo jar
export JV4LINFO_URL=http://luniks.net/luniksnet/download/java/jv4linfo
# Name of the jv4linfo file
export JV4LINFO_JAR=jv4linfo-0.2.1-src.jar
# Shared object required by the jv4linfo jar to function
export JV4LINFO_LIB=libjv4linfo.so
# Directory where the shared object will be copied so that jvm can find it. In other words, it must be in the java.library.path
export JV4LINFO_PATH=/usr/lib
# Directory where the jv4linfo-related files are stored
export JV4LINFO_DIR=$CA_DIR/jv4linfo
                                                                         

# Required scripts for installation
SETUP_USER=./setup_user.sh
INSTALL_VGA2USB=./install_vga2usb_drivers.sh
SETUP_DEVICES=./setup_devices.sh
INSTALL_DEPENDENCIES=./install_dependencies.sh
SETUP_SOURCE=./setup_source.sh
SETUP_ENVIROMENT=./setup_enviroment.sh
SETUP_BOOT=./setup_boot.sh
export CLEANUP=./cleanup.sh                # This variable is exported because the script is modified by another

SCRIPTS=( "$SETUP_USER" "$INSTALL_VGA2USB" "$SETUP_DEVICES" "$INSTALL_DEPENDENCIES" "$SETUP_ENVIROMENT" "$SETUP_SOURCE" "$SETUP_BOOT" "$CLEANUP" )
SCRIPTS_EXT=docs/scripts/ubuntu_capture_agent

# End of variables section########################################################################################



# Checks if this script is being run with root privileges, exiting if it doesn't
if [[ `id -u` -ne 0 ]]; then
    echo "This script requires root privileges. Please run it with the sudo command or log in to the root user and try again"
    exit 1
fi

# Change the working directory to a temp directory under /tmp
# Deletes it first, in case it existed previously (MH-3797)
rm -rf $WORKING_DIR
mkdir -p $WORKING_DIR
cd $WORKING_DIR

# Log the technical outputs                                                                                                                                  
echo "# Output of uname -a" > $LOG_FILE
uname -a >> $LOG_FILE
echo >> $LOG_FILE
echo "# Total memory" >> $LOG_FILE
echo $(cat /proc/meminfo | grep -m 1 . | cut -d ':' -f 2) >> $LOG_FILE
echo >> $LOG_FILE
echo "# Processor(s) model name(s)"
IFS='                                                                                                                                                        
'
models=$(cat /proc/cpuinfo | sed -e '/model name/!d' -e 's/^.*: *//g')
unset IFS
for name in $models; do
    echo $name >> $LOG_FILE
done

# If wget isn't installed, get it from the ubuntu software repo
wget foo &> /dev/null
if [ $? -eq 127 ]; then
    apt-get -y --force-yes install wget &>/dev/null
    if [ $? -ne 0 ]; then
	echo "Couldn't install the necessary command 'wget'. Please try to install it manually and re-run this script"
	exit 1
    fi
fi

# Check for the necessary scripts and download them from the svn location
# Using C-like syntax in case file names have whitespaces
for (( i = 0; i < ${#SCRIPTS[@]}; i++ )); do
    f=${SCRIPTS[$i]}
	# Check if the script is in the directory where the install.sh script was launched
	if [[ -e $START_PATH/$f ]]; then
	    # ... and copies it to the working directory
	    cp $START_PATH/$f $WORKING_DIR
	else
	    # The script is not in the initial directory, so try to download it from the opencast source page
	    wget $SRC_DEFAULT/$SCRIPTS_EXT/$f &> /dev/null	    
	    # Check the file is downloaded
	    if [[ $? -ne 0 ]]; then
		echo "Couldn't retrieve the script $f from the repository. Try to download it manually and re-run this script."
		exit 2
	    fi
	fi  
    chmod +x $f
done

# Choose/create the matterhorn user (WARNING: The initial perdiod (.) MUST be there so that the script can export several variables)
. ${SETUP_USER}

# Create the directory where all the capture-agent-related files will be stored
mkdir -p $CA_DIR

# Install the 3rd party dependencies (WARNING: The initial perdiod (.) MUST be there so that the script can export several variables)
. ${INSTALL_DEPENDENCIES}
if [[ "$?" -ne 0 ]]; then
    echo "Error installing the 3rd party dependencies."
    exit 1
fi

# Install the vga2usb driver
${INSTALL_VGA2USB}
if [[ "$?" -ne 0 ]]; then
    echo "Error installing the vga2usb driver."
    exit 1
fi

# Set up the matterhorn code --doesn't build yet!
${SETUP_SOURCE}
if [[ "$?" -ne 0 ]]; then
    echo "Error setting up the matterhorn code. Contact matterhorn@opencastproject.org for assistance."
    exit 1
fi

# Setup properties of the devices
${SETUP_DEVICES}
if [[ "$?" -ne 0 ]]; then
    echo "Error setting up the capture devices. Contact matterhorn@opencastproject.org for assistance."
    exit 1
fi

# Set up user enviroment
${SETUP_ENVIROMENT}
if [[ "$?" -ne 0 ]]; then
    echo "Error setting up the enviroment for $USERNAME. Contact matterhorn@opencastproject.org for assistance."
    exit 1
fi

# Build matterhorn
echo -e "\n\nProceeding to build the capture agent source. This may take a long time. Press any key to continue...\n\n"
read -n 1 -s

cd $SOURCE
su matterhorn -c "mvn clean install -Pcapture -DdeployTo=\${FELIX_HOME}/${DEPLOY_DIR}"
if [[ "$?" -ne 0 ]]; then
    echo -e "\nError building the matterhorn code. Contact matterhorn@opencastproject.org for assistance."
    exit 1
fi
cd $WORKING_DIR

# Set up the file to run matterhorn automatically on startup
${SETUP_BOOT}

# Log the contents of /etc/issue
echo >> $LOG_FILE
echo "# Contents in /etc/issue" >> $LOG_FILE
cat /etc/issue >> $LOG_FILE

echo -e "\n\n\nCapture Agent succesfully installed\n\n\n"

read -p "It is recommended to reboot the system after installation. Do you wish to do it now [Y/n]? " response

while [[ -z "$(echo ${response:-Y} | grep -i '^[yn]')" ]]; do
    read -p "Please enter [Y]es or [n]o: " response
done

if [[ -n "$(echo ${response:-Y} | grep -i '^y')" ]]; then
    echo "Rebooting... "
    reboot > /dev/null
else
    echo -e "\n\nThe capture agent will start automatically after rebooting the system."
    echo "However, you can start it manually by running ${FELIX_HOME}/bin/start_matterhorn.sh"
    echo "Please direct your questions / suggestions / etc. to the list: matterhorn@opencastproject.org"
    echo
    read -n 1 -s -p "Hit any key to exit..."
    clear

fi
