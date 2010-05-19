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
url=$(svn info $TRUNK 2> /dev/null | grep URL: | cut -d ' ' -f 2) 
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
	read -p "Do you wish to download the source code from the official trunk? [Y/n]: " answer
	while [[ -z "$(echo "${answer:-Y}" | grep -i '^[yn]')" ]]; do
	    read -p "Please answer [Y]es or [n]o: " answer
	done
	
	if [[ $(echo "${answer:-Y}" | grep -i '^[n]') ]]; then
	    read -p "Enter the complete download address [$TRUNK_URL]: " address
	fi
	
	echo -n "Downloading matterhorn source from repository... "
	rm -rf $TRUNK
	svn co --force ${address:-$TRUNK_URL} $TRUNK > /dev/null

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
cp -rf $TRUNK/docs/felix/bin ${FELIX_HOME}
cp -rf $TRUNK/docs/felix/conf ${FELIX_HOME}
echo "Done"
