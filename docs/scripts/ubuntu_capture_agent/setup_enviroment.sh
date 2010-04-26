#! /bin/bash

#########################################
# Setup environment for matterhorn user #
#########################################

# Checks this script is being run from install.sh
if [[ ! $INSTALL_RUN ]]; then
    echo "You shouldn't run this script directly. Please use the install.sh instead"
    exit 1
fi

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
echo "Creating the cleanup script... "
sed -i "s/^USER=/USER=${USERNAME//\//\\/}/" "$CLEANUP"
sed -i "s/^SRC_LIST=/SRC_LIST=${SRC_LIST//\//\\/}/" "$CLEANUP"
sed -i "s/^SRC_LIST_BKP=/SRC_LIST_BKP=${SRC_LIST_BKP//\//\\/}/" "$CLEANUP"
sed -i "s/^OC_DIR=/OC_DIR=${OC_DIR//\//\\/}/" "$CLEANUP"
sed -i "s/^CA_DIR=/CA_DIR=${CA_DIR//\//\\/}/" "$CLEANUP"
sed -i "s/^ON_STARTUP_FILE=/ON_STARTUP_FILE=${ON_STARTUP_FILE//\//\\/}/" "$CLEANUP"

cp $CLEANUP $CA_DIR