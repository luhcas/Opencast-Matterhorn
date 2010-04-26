#! /bin/bash

##########################################################################################
# Tries to guess the most suitable vga2usb driver for the current system and installs it #
##########################################################################################

# Checks this script is being run from install.sh
if [[ ! $INSTALL_RUN ]]; then
    echo "You shouldn't run this script directly. Please use the install.sh instead"
    exit 1
fi

# List of common drivers
drivers[0]="vga2usb-3.23.7.2-2.6.31-16-generic-i386.tbz"
drivers[1]="vga2usb-3.23.7.2-2.6.31-14-generic-pae.tbz"
drivers[2]="vga2usb-3.23.6.0000-2.6.31-14-generic-i386.tbz"
drivers[3]="vga2usb-3.23.6.0000-2.6.31-14-generic-x86_64.tbz"
drivers[4]="vga2usb-3.22.2.0000-2.6.28-15-generic_i386.tbz"
drivers[5]="vga2usb-3.22.2.0000-2.6.28-13-server_i386.tbz"
drivers[6]="vga2usb-3.22.2.0000-2.6.28-13-server_amd64.tbz"
drivers[7]="vga2usb-3.22.2.0000-2.6.28-13-generic_i386.tbz"
drivers[8]="vga2usb-3.22.2.0000-2.6.28-13-generic_amd64.tbz"
drivers[9]="Not listed here"
drivers[10]="Do not need driver"

# List of kernel versions using `uname -r`;`uname -m`
# These are mapped to drivers above
kernels[0]="2.6.31-16-generic;i686|2.6.31-16-generic;i386"
kernels[1]=""
kernels[2]="2.6.31-14-generic;i686|2.6.31-14-generic;i386"
kernels[3]="2.6.31-14-generic;x86_64"

lsmod | grep -e "^vga2usb" &> /dev/null
if [[ "$?" -ne 0 ]]; then
  
    # Determine which vga2usb driver to load for this kernel
    # If this variable remains 0, we attempt to load the driver
    LOAD_DRIVER=0
    EPIPHAN=""
    KERNEL=`uname -r`
    ARCH=`uname -m`
    EPIPHAN_HW="$KERNEL;$ARCH"
    EPIPHAN_DEFAULT=9
    
    # If current kernel matches common driver, suggest it
    for ((i = 0; i < ${#kernels[@]}; i++ )); do
	test="$(echo ${kernel[$i]} | grep $EPIPHAN_HW)"
	if [ "$test" != "" ]; then
            EPIPHAN_DEFAULT=$i
	fi
    done
    
    # Let user choose driver
    echo "System information: `uname -mors`"
    echo "Here is a list of supported Epiphan VGA2USB drivers:"
    for ((i = 0; i < ${#drivers[@]}; i++ )); do
	echo -e "\t($i)\t${drivers[$i]}"
    done

    read -p "Choose an option: ($EPIPHAN_DEFAULT) " opt
    until [[ $(echo "${opt:-$EPIPHAN_DEFAULT}" | grep -o '^[0-9][0-9]*$') && $opt -ge 0 && $chosen_input -lt ${#drivers[@]} ]]; do 
	read -p "Invalid value. Please enter a value from the list: " opt
    done

    # If opt is null or empty, assigns the value $EPIPHAN_DEFAULT to it
    : ${opt:=$EPIPHAN_DEFAULT}

    if [[ $opt -ge 0 && $opt -lt 9 ]]; then
        # Downloads the driver from the epiphan page
  	DRIVER_URL="http://www.epiphan.com/downloads/linux/${drivers[$opt]}"
  	EPIPHAN="${drivers[$opt]}"
    elif [[ $opt -eq 9 ]]; then
  	echo -n "Please input the URL of the driver you would like to load: "
  	read url
  	DRIVER_URL="$url"
  	EPIPHAN=${DRIVER_URL##*/}
    else
	LOAD_DRIVER=1    
    fi
    
    # Attempt to load the vga2usb driver
    FAILURE=0

    if [[ $LOAD_DRIVER -eq 0 ]]; then
	echo "Loading driver $EPIPHAN"

	mkdir -p $CA_DIR/$VGA2USB_DRV
  	wget -P $CA_DIR/$VGA2USB_DRV $DRIVER_URL
	
  	if [[ $? -ne 0 ]]; then
    	    FAILURE=1
  	else
  	    cd $CA_DIR/$VGA2USB_DRV
  	    tar jxf $EPIPHAN
  	    sed -i '/sudo \/sbin\/insmod/s|$| num_frame_buffers=2|' Makefile
  	    make load
  	    if [[ $? -ne 0 ]]; then
    		FAILURE=1
  	    fi
	fi
    fi
    
    if [[ $FAILURE -ne 0 ]]; then
  	echo "Failed to load Epiphan driver. Try to do it manually."
	exit 1
    fi
else
    echo "VGA2USB driver already installed."
fi
