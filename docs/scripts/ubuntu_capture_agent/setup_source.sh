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
	read -p "Where would you like to download the source from [$SRC_DEFAULT]? " response

	if [[ "${response:$SRC_DEFAULT}" == "trunk" ]]; then
	    address=$TRUNK_URL
	else
	    address=$BRANCHES_URL/$response
	fi

	echo -n "Downloading matterhorn source from $address... "
	rm -rf $SOURCE
	svn co --force $address $SOURCE > /dev/null

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
