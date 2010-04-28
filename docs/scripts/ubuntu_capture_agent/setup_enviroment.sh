#! /bin/bash

#########################################
# Setup environment for matterhorn user #
#########################################

# Checks this script is being run from install.sh
if [[ ! $INSTALL_RUN ]]; then
    echo "You shouldn't run this script directly. Please use the install.sh instead"
    exit 1
fi

# Setup opencast directories
read -p "Where would you like the opencast directories to be stored ($OC_DIR)? " directory

if [[ "$directory" != "" ]]; then
    OC_DIR="$directory"
fi
echo

mkdir -p $OC_DIR/cache
mkdir -p $OC_DIR/config
mkdir -p $OC_DIR/volatile
mkdir -p $OC_DIR/cache/captures

chown -R $USERNAME:$USERNAME $OC_DIR
chmod -R 700 $OC_DIR

# Define capture agent name by using the hostname
unset agentName
hostname=`hostname`
read -p "Please enter the agent name: ($hostname) " name
while [[ -z $(echo "${agentName:-$hostname}" | grep '^[a-zA-Z0-9_\-][a-zA-Z0-9_\-]*$') ]]; do
    read - p "Please use only alphanumeric characters, hyphen(-) and underscore(_): ($hostname) " agentName
done 
sed -i "s/capture\.agent\.name.*/capture\.agent\.name=${agentName:-$hostname}/g" $CAPTURE_PROPS
echo

# Prompt for core hostname. Default to localhost:8080
read -p "Please enter Matterhorn Core hostname ($DEFAULT_CORE_URL): " core
core=$(echo ${core:-$DEFAULT_CORE_URL} | sed 's/\([\/\.]\)/\\\1/g')
sed -i "s/\(org\.opencastproject\.server\.url=\).*$/\1$core/" $GEN_PROPS

# Set up maven and felix enviroment variables in the user session
echo -n "Setting up maven and felix enviroment for $USERNAME... "
EXPORT_M2_REPO="export M2_REPO=${M2_REPO}"
EXPORT_FELIX_HOME="export FELIX_HOME=${FELIX_HOME}"
EXPORT_JAVA_HOME="export JAVA_HOME=${JAVA_HOME}"

grep -e "${EXPORT_M2_REPO}" $HOME/.bashrc &> /dev/null
if [ "$?" -ne 0 ]; then
    echo "${EXPORT_M2_REPO}" >> $HOME/.bashrc
fi
grep -e "${EXPORT_FELIX_HOME}" $HOME/.bashrc &> /dev/null
if [ "$?" -ne 0 ]; then
    echo "${EXPORT_FELIX_HOME}" >> $HOME/.bashrc
fi
grep -e "${EXPORT_JAVA_HOME}" $HOME/.bashrc &> /dev/null
if [ "$?" -ne 0 ]; then
    echo "${EXPORT_JAVA_HOME}" >> $HOME/.bashrc
fi

chown $USERNAME:$USERNAME $HOME/.bashrc

# Change permissions and owner of the $CA_DIR folder
chmod -R 700 $CA_DIR
chown -R $USERNAME:$USERNAME $CA_DIR

echo "Done"

# Set up the deinstallation script
# The syntax ${varname//\//\\/} escapes the possible /'s that might be in the variable's value
echo -n "Creating the cleanup script... "
SRC_LIST_BKP=$SRC_LIST.$BKP_SUFFIX
sed -i "s/^USER=[^\ ]*\?\(.*\)$/USER=${USERNAME//\//\\/}\1/" "$CLEANUP"
sed -i "s/^SRC_LIST=[^\ ]*\?\(.*\)$/SRC_LIST=${SRC_LIST//\//\\/}\1/" "$CLEANUP"
sed -i "s/^SRC_LIST_BKP=[^\ ]*\?\(.*\)$/SRC_LIST_BKP=${SRC_LIST_BKP//\//\\/}\1/" "$CLEANUP"
sed -i "s/^OC_DIR=[^\ ]*\?\(.*\)$/OC_DIR=${OC_DIR//\//\\/}\1/" "$CLEANUP"
sed -i "s/^CA_DIR=[^\ ]*\?\(.*\)$/CA_DIR=${CA_DIR//\//\\/}\1/" "$CLEANUP"
sed -i "s/^RULES_FILE=[^\ ]*\?\(.*\)$/RULES_FILE=${DEV_RULES//\//\\/}\1/" "$CLEANUP"
sed -i "s/^CA_DIR=[^\ ]*\?\(.*\)$/CA_DIR=${CA_DIR//\//\\/}\1/" "$CLEANUP"
sed -i "s/^STARTUP_SCRIPT=[^\ ]*\?\(.*\)$/STARTUP_SCRIPT=${STARTUP_SCRIPT//\//\\/}\1/" "$CLEANUP"

# Write the uninstalled package list to the cleanup.sh template
if [[ ${#PKG_LIST[@]} -gt 0 ]]; then
    sed -i "s/^PKG_LIST=.*$/PKG_LIST=( ${PKG_LIST[@]} )/" $CLEANUP
else
    sed -i "s/^PKG_LIST=.*$/PKG_LIST=/" $CLEANUP
fi

echo "Done"

# Prompt for the location of the cleanup script
unset location

while [[ true  ]]; do
    read -p "Please enter the location to store the cleanup script ($START_PATH): " location
    if [[ -d "${location:=$START_PATH}" ]]; then
	if [[ -e $location ]]; then
	    read -p "File $location/$CLEANUP already exists. Do you wish to overwrite it (y/N)? " response
	    if [[ -n "$(echo ${response:-N} | grep -i '^y')" ]]; then
		break;
	    fi
	fi
    else
	echo "Invalid location. $location is not a directory."
    fi
done

cp $CLEANUP ${location:=$START_PATH}
chown --reference=$location $location/$CLEANUP