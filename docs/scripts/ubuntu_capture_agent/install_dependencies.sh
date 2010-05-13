#!/bin/bash

##########################################################################
# Install and configure the necessary dependencies for the capture agent #
##########################################################################

# Checks this script is being run from install.sh
if [[ ! $INSTALL_RUN ]]; then
    echo "You shouldn't run this script directly. Please use the install.sh instead"
    exit 1
fi

# Prompt the user to modify the default repositories
while [[ true ]]; do
    echo
    read -p "Do you wish to use a custom software mirror instead of the Ubuntu defaults (y|N)? " answer
    while [[ -z "$(echo ${answer:-N} | grep -i '^[yn]')" ]]; do
	read -p "Please answer (y)es or (N)o: " answer
    done
    
    # If 'yes', prompt a menu to modify any of the parameters
    if [[ -n "$(echo ${answer:-N} | grep -i "^y")" ]]; then
	while [[ true ]]; do
	    echo
	    echo "Please choose which mirror you want to modify:"
	    echo -e "\t0)\t Archive Mirror \t(current value: ${mirrors[0]:-$DEFAULT_MIRROR})"
	    echo -e "\t1)\t Security Mirror \t(current value: ${mirrors[1]:-$DEFAULT_SECURITY})"
	    echo -e "\t2)\t Partner Mirror \t(current value: ${mirrors[2]:-$DEFAULT_PARTNER})"
	    read -p "Selection (leave blank to continue installation): " answer
	    
	    while [[ -n "$answer" && -z "$(echo "$answer" | grep "^[012]$")" ]]; do
		read -p "Please choose a value from the list: " answer
	    done
	    
	    if [[ -n "$answer" ]]; then
		read -p "Please enter the mirror URL: " mirrors[$answer]
	    else
		break;
	    fi
	done
    fi
    
    # Create a backup of the sources.list file and create its own (preserves any existing sources.list.backup)
    mv -n $SRC_LIST $SRC_LIST.$BKP_SUFFIX
    
    # Applies configuration changes
    DIST_NAME=$(cat /etc/lsb-release | grep "^DISTRIB_CODENAME" | cut -d "=" -f 2)
    echo "deb ${mirrors[0]:-$DEFAULT_MIRROR} ${DIST_NAME} main restricted universe multiverse" > $SRC_LIST
    echo "deb ${mirrors[0]:-$DEFAULT_MIRROR} ${DIST_NAME}-updates main restricted universe multiverse" >> $SRC_LIST
    echo "deb ${mirrors[1]:-$DEFAULT_SECURITY} ${DIST_NAME}-security main restricted universe multiverse" >> $SRC_LIST
    echo "deb ${mirrors[2]:-$DEFAULT_PARTNER} ${DIST_NAME} partner" >> $SRC_LIST
    
    apt-get -qq update 2> /dev/null
    
    if [[ $? -eq 0 ]]; then
	break
    else
	echo
	echo "Error. Couldn't update properly from the current Ubuntu mirrors. Reverting..."
	unset mirrors
    fi
done

# Auto set selections when installing postfix and jdk packages
# The <<EOF tag indicates an input with several lines, ending with an EOF line (this is bash syntax)
# The lines indicate which answers, that otherwise would be prompted to the user in the package configuration, will be answered automatically
# -- First is the package name
# -- Second is the name of the question to be answered
# -- Third is the type of answer this questions expects
# -- Fourth is the answer that will be given to this question
debconf-set-selections <<EOF
postfix postfix/mailname string fax
postfix postfix/main_mailer_type select Internet Site
sun-java5-jdk shared/accepted-sun-dlj-v1-1 boolean true
?sun-java6-jdk shared/accepted-sun-dlj-v1-1 boolean true
EOF

# Check which required packages are already installed
for (( i = 0; i < ${#PKG_LIST[@]}; i++ )); do
    if [[ -z $(dpkg -l | grep "\<${PKG_LIST[$i]}\>") ]]; then
	noinst[${#noinst[@]}]=${PKG_LIST[$i]}
    fi
done

# Install the required 3rd party packages
echo -n "Installing third party packages from Ubuntu repository... "
apt-get -y --force-yes install ${noinst[@]} > /dev/null
echo "Done"

# Set up java-6-sun as the default alternative
echo -n "Setting up java-6-sun as the default jvm... "
update-java-alternatives -s $JAVA_PATTERN 2> /dev/null
echo "Done"

# Define some enviroment variables
export JAVA_HOME=$JAVA_PREFIX/`ls $JAVA_PREFIX | grep ^$JAVA_PATTERN$`
export PKG_LIST=${noinst[@]}

cd $CA_DIR

# Setup felix
echo -n "Downloading Felix... "
while [[ true ]]; do 
    if [[ ! -s ${FELIX_FILENAME} ]]; then
	wget -q ${FELIX_URL}
    fi
    # On success, uncompress the felix files in their location
    if [[ $? -eq 0 ]]; then
	echo -n "Uncompressing... "
	tar xzf ${FELIX_FILENAME}
	mkdir -p ${FELIX_HOME}/load
	echo "Done"
	break
    fi
    # Else, ask for the actions to take
    echo
    read -p "Error retrieving the Felix files from the web. Retry (Y/n)?" answer
    if [[ -n "$(echo ${answer:-Y} | grep -i "^n")" ]]; then
	echo "You must download Felix manually and install it under $CA_DIR, in order for matterhorn to work"
	break;
    else
	echo -n "Retrying... "
    fi
done

# Setup jv4linfo
if [[ ! -e "$JV4LINFO_PATH/$JV4LINFO_LIB" ]]; then
    echo -n "Installing jv4linfo... "
    if [[ ! -e "$JV4LINFO_JAR" ]]; then
	wget -q $JV4LINFO_URL/$JV4LINFO_JAR
    fi
    jar xf $JV4LINFO_JAR
    cd jv4linfo/src
    # The ant build script has a hardcoded path to the openjdk, this sed line will
    # switch it to be whatever is defined in JAVA_HOME
    sed -i '74i\\t<arg value="-fPIC"/>' build.xml
    sed -i "s#\"/usr/lib/jvm/java-6-openjdk/include\"#\"$JAVA_HOME/include\"#g" build.xml
    
    ant -lib ${JAVA_HOME}/lib &> /dev/null
    if [[ "$?" -ne 0 ]]; then
	echo "Error building libjv4linfo.so"
	exit 1
    fi
    cp ../lib/$JV4LINFO_LIB $JV4LINFO_PATH
    
    cd ../..
    echo "Done"
else
    echo "libjv4linfo.so already installed"
fi

cd $WORKING_DIR

# Setup ntdp
echo 
read -p "Which NTP server would you like to use (default: ntp.ubuntu.com)? " server
sed -i "s#^server .*#server ${server:-DEFAULT_NTP_SERVER}#" $NTP_CONF
echo "NTP server set to ${server:-$DEFAULT_NTP_SERVER}"
echo "Consider editing the file $NTP_CONF for manually changing the default NTP server or adding more servers to the list"
echo
