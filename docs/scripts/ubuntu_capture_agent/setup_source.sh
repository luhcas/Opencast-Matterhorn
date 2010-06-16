#! /bin/bash

###############################
# Setup Matterhorn from trunk #
###############################

# Checks this script is being run from install.sh
if [[ ! $INSTALL_RUN ]]; then
    echo "You shouldn't run this script directly. Please use the install.sh instead"
    exit 1
fi

# Detect if the matterhorn source has been already checked out
url=$(svn info $SOURCE 2> /dev/null | grep URL: | cut -d ' ' -f 2) 
if [[ -n "$url" ]]; then
    echo
    read -p "The source $url has been already checked out. Do you wish to keep it [Y/n]? " keep
    while [[ -z "$(echo "${keep:-Y}" | grep -i '^[yn]')" ]]; do
	read -p "Please answer [Y]es or [n]o: " keep
    done  
else
    keep=no
fi

if [[ -n "$(echo "${keep:-Y}" | grep -i '^n')" ]]; then
    # Get the necessary matterhorn source code (the whole trunk, as specified in MH-3211)
    while [[ true ]]; do
	echo
	read -p "Enter the branch or tag you would like to download [${SRC_DEFAULT##*/}]: " response
	: ${response:=${SRC_DEFAULT##*/}}

	if [[ "$response" == "${TRUNK_URL##*/}" ]]; then
	    address=$TRUNK_URL
	else
	    # Check the branches first
	    address=$BRANCHES_URL/$response
	    svn info $address &> /dev/null
	    # If $address does not exist, try the tags
	    [[ $? -ne 0 ]] && address=$TAGS_URL/$response
	fi

	rm -rf $SOURCE
	echo -n "Attempting to download matterhorn source from $address... "
	svn co --force $address $SOURCE &> /dev/null	

	if [[ $? -eq 0 ]]; then
	    #### Exit the loop ####
	    break
	fi

	## Error. The loop repeats
	echo "Error!"
	echo "Couldn't check out the matterhorn code. Is the URL correct?"
    done
    echo "Done"
fi

# Setup felix configuration
echo -n "Applying matterhorn configuration files to felix... "
cp -rf $SOURCE/docs/felix/bin ${FELIX_HOME}
cp -rf $SOURCE/docs/felix/conf ${FELIX_HOME}
echo "Done"
