#! /bin/bash

#########################################################################
# Create / choose the user for whom the capture agent will be installed #
#########################################################################

# Checks this script is being run from install.sh
if [[ ! $INSTALL_RUN ]]; then
    echo "You shouldn't run this script directly. Please use the install.sh instead"
    exit 1
fi

# Prompt for user name
read -p "Input desired opencast username [$USERNAME]: " input
if [[ -n "$input" ]]; then
    USERNAME=$input
fi

# Add user and give sudo priveleges and permissions for accessing audio/video devices
useradd -m -s /bin/bash $USERNAME
var=$?
if [[ $var -eq 0 ]]; then
    # Ask for the user password
    for i in $(seq 1 $MAX_PASSWD_ATTEMPTS); do
	passwd $USERNAME
	if [[ $? -eq 0 ]]; then
	    echo "$USERNAME password updated succesfully"
	    break
	elif [[ $i -eq $MAX_PASSWD_ATTEMPTS ]]; then
	    echo "Error. Too many password attempts. Aborting."
	    return 1
	fi
   done
elif [[ $var -ne 9 ]]; then
    echo "Error when creating the $USERNAME user"
    exit 1
fi

# Setting up user's permissions by including it in the appropriate groups
usermod -aG admin,video,audio $USERNAME

# Exports the username, its home and the directory where the capture agent files will be stored
export USERNAME=$USERNAME
export HOME=$(grep "^${USERNAME}:" /etc/passwd | cut -d: -f 6)
if [[ -z "$HOME" ]]; then
    echo "Error: the specified user doesn't exist or doesn't have a HOME folder"
    exit 1
fi

# Export some other env. variables depending on the locations just created
export CA_DIR=$HOME/$CA_SUBDIR
export TRUNK=$CA_DIR/$TRUNK_SUBDIR
export FELIX_HOME=$CA_DIR/$FELIX_SUBDIR
export CAPTURE_PROPS=${FELIX_HOME}/${FELIX_PROPS_SUFFIX}
export GEN_PROPS=${FELIX_HOME}/${FELIX_GENCONF_SUFFIX}
export M2_REPO=$HOME/$M2_SUFFIX
