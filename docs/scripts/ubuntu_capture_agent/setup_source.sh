#! /bin/bash

###############################
# Setup Matterhorn from trunk #
###############################

# Checks this script is being run from install.sh
if [[ ! $INSTALL_RUN ]]; then
    echo "You shouldn't run this script directly. Please use the install.sh instead"
    exit 1
fi

# Get the necessary matterhorn source code (the whole trunk, as specified in MH-3211)
unset answer
read -p "Do you wish to download the source code from the official trunk? (Y/n): " answer
while [[ -z "$(echo "${answer:-Y}" | grep -i '^[yn]')" ]]; do
    read -p "Please enter a valid answer (Y/n): " answer
done

if [[ $(echo "${answer:-Y}" | grep -i '^[n]') ]]; then
    read -p "Enter the complete download address (default: trunk): " address
fi

echo -n "Downloading matterhorn source from repository... "
svn co ${address:-$TRUNK_URL} $TRUNK > /dev/null
if [[ $? -ne 0 ]]; then
    echo "Error!"
    echo "Couldn't check out the matterhorn code. Aborting"
    exit 1
fi
echo "Done"


# Setup felix configuration
echo -n "Applying matterhorn configuration files to felix... "
cp -r $TRUNK/docs/felix/bin/* ${FELIX_HOME}/bin
cp -r $TRUNK/docs/felix/conf ${FELIX_HOME}
echo "Done"
