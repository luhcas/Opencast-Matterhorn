#! /bin/bash

##########################################################################################
# Tries to guess the most suitable vga2usb driver for the current system and installs it #
##########################################################################################

# Checks this script is being run from install.sh
if [[ ! $INSTALL_RUN ]]; then
    echo "You shouldn't run this script directly. Please use the install.sh instead"
    exit 1
fi


if [[ -z "$(lsmod | grep -e "^vga2usb")" ]]; then
  
    # List the most recent drivers
    DRIVER_NR=20
    FILE_NAME=driver_list
    
    wget -q -O $FILE_NAME $EPIPHAN_URL
    #FIXME: Add some sort of retry system
    if [[ $? -ne 0 ]]; then
	echo "Error. The list of available vga2usb drivers could not be retrieved. Aborting..."
	exit 1
    fi
    
    # Gets the first $DRIVER_NR lines -- the first $DRIVER_NR links to driver files
    drivers=( $(grep -m $DRIVER_NR "vga2usb" $FILE_NAME | sed 's#^.*<a\s*href="\(.*\)".*>.*</a>.*$#\1#') )


##### FIXME: Default choice detection temporarily disabled. We need to make it more "intelligent" to allow small kernel differences#################
    # Determine which vga2usb driver to load for this kernel
    #EPIPHAN=""
    #KERNEL=`uname -r`
    #ARCH=`uname -m`
    #EPIPHAN_DEFAULT=
  

  
    # If current kernel matches common driver, suggest it
    #for ((i = 0; i < ${#kernels[@]}; i++ )); do
#	test="$(echo ${kernel[$i]} | grep $EPIPHAN_HW)"
#	if [ "$test" != "" ]; then
 #           EPIPHAN_DEFAULT=$i
#	fi
 #   done
####################################################################################################################################################

    # Let user choose driver
    echo "System information: `uname -mors`"
    echo "Here is a list of the most recent Epiphan VGA2USB drivers:"
    for ((i = 0; i < ${#drivers[@]}; i++ )); do
	echo -e "\t($i)\t${drivers[$i]}"
    done
    echo -e "\t($i)\tNot listed here"
    (( i+=1 ))
    echo -e "\t($i)\tDo not need driver"

    # Prompts for an option
    read -p "Choose an option from the list: " opt

    # Main loop: if some error happens, the user will be prompted again for another option
    while [[ true ]]; do

	until [[ -n "$(echo "${opt:-$EPIPHAN_DEFAULT}" | grep -o '^[0-9][0-9]*$')" && $opt -ge 0 && $opt -lt $(( ${#drivers[@]} + 2 )) ]]; do 
	    read -p "Invalid value. Please enter a value from the list: " opt
	done
	
        # If opt is null or empty, assigns the value $EPIPHAN_DEFAULT to it
        #: ${opt:=$EPIPHAN_DEFAULT}
	if [[ $opt -ge 0 && $opt -le ${#drivers[@]} ]]; then
 	    if [[ $opt -eq ${#drivers[@]} ]]; then
	        # Ask the user for the driver url
		echo "You might want to check $EPIPHAN_URL to see a complete list of the available drivers."
		read -p "Please input the URL of the driver you would like to load: " DRIVER_URL
  	        EPIPHAN=${DRIVER_URL##*/}
	    else
		# Download the driver from the epiphan page
  		DRIVER_URL="$EPIPHAN_URL/${drivers[$opt]}"
  		EPIPHAN="${drivers[$opt]}"
	    fi
	    
	    # Attempt to load the vga2usb driver
	    echo -n "Downloading driver $EPIPHAN... "
	    
	    mkdir -p $CA_DIR/$VGA2USB_DIR
  	    wget -q -P $CA_DIR/$VGA2USB_DIR $DRIVER_URL
	    
  	    if [[ $? -eq 0 ]]; then
		echo -n "Loading driver... "
  		cd $CA_DIR/$VGA2USB_DIR
  		tar jxf $EPIPHAN
  		sed -i '/sudo \/sbin\/insmod/s|$| num_frame_buffers=2|' Makefile
  		make load &> /dev/null
  		if [[ $? -ne 0 ]]; then
    		    echo "Error!"
		    echo "Failed to load Epiphan driver. Maybe your machine kernel or architecture were not compatible?"
		    rm -r $(tar jtf $EPIPHAN 2> /dev/null) 2> /dev/null
		    rm $EPIPHAN
  		else
		    echo "Done."
		    # Exit the loop
		    break;
		fi
		cd $WORKING_DIR
  	    else
		echo "Error!"
		echo "Failed to retrieve the driver from the URL. Please check it is correct and try again."
	    fi
	    
	else
	    # Skip driver installation
	    echo "Skipping the vga2usb driver installation. Please note that if no driver is present, the vga2usb card(s) will not be detected."
	    read -p "Are you sure you want to proceed (y|N)? " answer
	    while [[ -z "$(echo ${answer:-N} | grep -i '^[yn]')" ]]; do
		read -p "Please enter (y)es or (N)o: " answer
		break;
	    done
	    if [[ -n "$(echo ${answer:-N} | grep -i '^y')" ]]; then
		break
	    fi
	fi
		
	echo
	read -p "Please try another option: " opt
    done
    
    cd $WORKING_DIR
else
    echo "VGA2USB driver already installed."
fi
