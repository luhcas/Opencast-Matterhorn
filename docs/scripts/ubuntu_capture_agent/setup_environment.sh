#! /bin/bash

#########################################
# Setup environment for matterhorn user #
#########################################

# Checks this script is being run from install.sh
if [[ ! $INSTALL_RUN ]]; then
    echo "You shouldn't run this script directly. Please use the install.sh instead"
    exit 1
fi

. "${FUNCTIONS}"

# Setup opencast storage directories
# TODO: Uncomment the following lines -and remove the next two- once the correct defaults for the directories are set in the config files in svn
## Read default from the config file
#default_dir=$(grep "^org\.opencastproject\.storage\.dir=.*$" $GEN_PROPS | cut -d '=' -f 2)
#ask -d "$default_dir" "Where would you like the matterhorn directories to be stored?" oc_dir
#: ${oc_dir:=$OC_DIR}
ask -d "$OC_DIR" "Where would you like the matterhorn directories to be stored?" oc_dir
echo

# Create the directories
mkdir -p "$oc_dir"/cache
mkdir -p "$oc_dir"/config
mkdir -p "$oc_dir"/volatile
mkdir -p "$oc_dir"/cache/captures

# Establish their permissions
chown -R $USERNAME:$USERNAME "$oc_dir"
chmod -R 770 "$oc_dir"

# Write the directory name to the agent's config file
#                 this->_______<- escapes the dots in the property key, so that sed doesn't not interpret them as wildcards
sed -i "s#^${STORAGE_KEY//./\\.}=.*\$#${STORAGE_KEY}=$oc_dir#" "$GEN_PROPS"

# Define capture agent name by using the hostname
unset agentName
ask -d "$(hostname)" -f '^[a-zA-Z0-9_\-]*$' -e "Please use only alphanumeric characters, hyphen(-) and underscore(_)"\
    "Please enter the agent name" agentName

sed -i "s/^${AGENT_NAME_KEY//./\\.}=.*$/${AGENT_NAME_KEY}=$agentName/" "$CAPTURE_PROPS"
echo

# Prompt for the URL where the core lives.
# TODO: (or maybe not) Support a distributed core would mean to set different URLs separately, rather than this centralized one
## Read default from the config file
#DEFAULT_CORE_URL=$(grep "^${CORE_URL_KEY//./\\.}=.*$" $CAPTURE_PROPS | cut -d '=' -f 2)
ask -d "$DEFAULT_CORE_URL" "Please enter the URL to the root of the machine hosting the ingestion service" core
sed -i "s#^${CORE_URL_KEY//./\\.}=.*\$#${CORE_URL_KEY}=$core#" "$CAPTURE_PROPS"

# Prompt for the time between two updates of the recording schedule
default_poll=$(grep "${SCHEDULE_POLL_KEY}" "$CAPTURE_PROPS" | cut -d '=' -f 2) #<-- This reads the default value from the config file
# The value in the file is in seconds, but the user is asked for a value in minutes --that's why the value after -d is adjusted
ask -d "$((default_poll/60))" -f '^0*[1-9][0-9]*$' -h '? - more info' -e "Invalid value"\
    "Please enter the time (in minutes) between two updates of the agent's recording schedule" poll
# Write the value to the file, adjusting the value from minutes to seconds
sed -i "s/${SCHEDULE_POLL_KEY//./\\.}=.*$/${SCHEDULE_POLL_KEY}=$((poll*60))/" "$CAPTURE_PROPS"

# Set up maven and felix enviroment variables in the user session
echo -n "Setting up maven and felix enviroment for $USERNAME... "
EXPORT_M2_REPO="export M2_REPO=${M2_REPO}"
EXPORT_FELIX_HOME="export FELIX_HOME=${FELIX_HOME}"
EXPORT_JAVA_HOME="export JAVA_HOME=${JAVA_HOME}"
EXPORT_SOURCE_HOME="export MATTERHORN_SOURCE=${SOURCE}"

grep -e "${EXPORT_M2_REPO}" "$HOME"/.bashrc &> /dev/null
if [ "$?" -ne 0 ]; then
    echo "${EXPORT_M2_REPO}" >> "$HOME"/.bashrc
fi
grep -e "${EXPORT_FELIX_HOME}" "$HOME"/.bashrc &> /dev/null
if [ "$?" -ne 0 ]; then
    echo "${EXPORT_FELIX_HOME}" >> "$HOME"/.bashrc
fi
grep -e "${EXPORT_JAVA_HOME}" "$HOME"/.bashrc &> /dev/null
if [ "$?" -ne 0 ]; then
    echo "${EXPORT_JAVA_HOME}" >> "$HOME"/.bashrc
fi
grep -e "${EXPORT_SOURCE_HOME}" "$HOME"/.bashrc &> /dev/null
if [ "$?" -ne 0 ]; then
    echo "${EXPORT_SOURCE_HOME}" >> "$HOME"/.bashrc
fi
echo "alias deploy=\"mvn install -DdeployTo=$FELIX_HOME/load\"" >> "$HOME"/.bashrc
echo "alias redeploy=\"mvn clean && deploy" >> "$HOME"/.bashrc

#chown $USERNAME:$USERNAME $HOME/.bashrc

# Change permissions and owner of the $CA_DIR folder
chmod -R 770 "$CA_DIR"
chown -R $USERNAME:$USERNAME "$CA_DIR"

# Add a link to the capture agent folder in the user home folder
ln -s $CA_DIR "$HOME"/"${CA_DIR##*/}"
chown $USERNAME:$USERNAME "$HOME"/"${CA_DIR##*/}"

echo "Done"

# Set up the deinstallation script
echo -n "Creating the cleanup script... "
SRC_LIST_BKP="$SRC_LIST"."$BKP_SUFFIX"
sed -i "s#^USER=.*\$#USER=$USERNAME#" "$CLEANUP"
sed -i "s#^HOME=.*\$#HOME=$HOME#" "$CLEANUP"
sed -i "s#^SRC_LIST=.*\$#SRC_LIST=$SRC_LIST#" "$CLEANUP"
sed -i "s#^SRC_LIST_BKP=.*\$#SRC_LIST_BKP=$SRC_LIST_BKP#" "$CLEANUP"
sed -i "s#^OC_DIR=.*\$#OC_DIR=$oc_dir#" "$CLEANUP"
sed -i "s#^CA_DIR=.*\$#CA_DIR=$CA_DIR#" "$CLEANUP"
sed -i "s#^RULES_FILE=.*\$#RULES_FILE=$DEV_RULES#" "$CLEANUP"
sed -i "s#^CA_DIR=.*\$#CA_DIR=$CA_DIR#" "$CLEANUP"
sed -i "s#^STARTUP_SCRIPT=.*\$#STARTUP_SCRIPT=$STARTUP_SCRIPT#" "$CLEANUP"

# Write the uninstalled package list to the cleanup.sh template
sed -i "s#^PKG_LIST=.*\$#PKG_LIST=\"$(echo $(cat $PKG_BACKUP))\"#" "$CLEANUP"

echo "Done"

# Prompt for the location of the cleanup script
echo
while [[ true ]]; do
    ask -d "$START_PATH" "Please enter the location to store the cleanup script" location
    if [[ -d "$location" ]]; then
        if [[ ! -e "$location/$CLEANUP" ]]; then
            break;
        fi
        yesno -d no "File $location/$CLEANUP already exists. Do you wish to overwrite it?" response
        if [[ "$response" ]]; then
            break;
        fi
    else
        echo -n "Invalid location. $location "
	if [[ -e "$location" ]]; then
	    echo "is not a directory"
	else 
	    echo "does not exist"
	fi
    fi
done


cp "$CLEANUP" "${location:=$START_PATH}"
chown --reference="$location" "$location"/"$CLEANUP"
