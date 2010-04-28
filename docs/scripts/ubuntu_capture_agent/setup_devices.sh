#!/bin/bash

############################
# Set up the video devices #
############################

if [[ -z "$INSTALL_RUN" ]]; then
    echo "You shouldn't run this script directly. Please use the install.sh instead"
    exit 1
fi

supportedDevices[0]="Hauppauge WinTV PVR-350"
supportedDevices[1]="BT878 video (ProVideo PV143)"
supportedDevices[2]="Epiphan VGA2USB"
supportedDevices[3]="Hauppauge HVR-1600"
supportedDevices[4]="Hauppauge WinTV PVR-150"
supportedDevices[5]="Hauppauge WinTV-HVR1300 DVB-T/H"

# ls the dev directory, then grep for video devices with *only* one number
for line in `ls /dev/video* | grep '/dev/video[0-9][0-9]*$'`; do
    devlist[${#devlist[@]}]=$line
done

# Make sure that the epiphan cards have an input connected
echo -e "\n\nWARNING: Please, make sure that your VGA2USB cards, if any, have an input connected. Otherwise they will NOT be detected"
echo -e "Press any key to continue...\n\n"
read -n 1 -s

# Read each line in the file. Using this C-like structure because the traditional 'for var in $list' does not get well with whitespaces in the names
#FIXME: The Hauppage, as it is two devices, appears duplicated. Should only appear once
for (( i = 0; i < ${#devlist[@]}; i++ )); do
    for (( j = 0; j < ${#supportedDevices[@]}; j++ )); do
	# The following line filters the first occurrence in the v4l-info output containing 'card' or 'name'
	# Then, it filters whatever string enclosed in double quotes, which in such lines correspond to the device name
	# Finally, it gets rid of the quotes
	testLine=$(v4l-info ${devlist[$i]} 2> /dev/null | grep -e name -e card -m 1 | grep -o '".*"' | grep -o "${supportedDevices[$j]}")
	# Add the matches to an array. This construction avoids 'gaps' --unset positions
	# Not both arrays devices and devNames will have the same size!
	if [[ "$testLine" == "${supportedDevices[$j]}" ]]; then
	    device[${#device[@]}]="${devlist[$i]}"
	    devName[${#devName[@]}]="${supportedDevices[$j]}"
	fi
    done
done

sed -i "/capture.device/d" $CAPTURE_PROPS

config=$CA_DIR/$CONFIG_SCRIPT
rules=tmp.rules

touch $rules
rm -f $config
touch $config

unset allDevices
for (( i = 0; i < ${#device[@]}; i++ )); do
    # sed erases all the blank spaces and substitutes the parentheses "(" & ")" by underscores "_"
    cleanName="$(echo ${devName[$i]} | sed -e 's/\s//g' -e 's/[\(|\)]/_/g')"
    
    # Setup device info using udevadm info
    realpath=/sys$(udevadm info --query=path --name=${device[$i]})
    symlinkName=$(echo $cleanName | cut -b -5 | tr "[:upper:]" "[:lower:]")
    vendor=$(cat $realpath/../../vendor 2> /dev/null)
    sysdevice=$(cat $realpath/../../device 2> /dev/null)

    if [[ $? -eq 0 ]]; then
	echo "KERNEL==\"video[0-9]\", SYSFS{vendor}==\"$vendor\", SYSFS{device}==\"$sysdevice\", SYMLINK+=\"$symlinkName\"" >> $rules
	#device="/dev/$symlinkName"
    fi

    # Ask the user whether or not they want to configure this device
    unset response
    read -p "Device ${devName[$i]} has been found. Do you want to configure it for matterhorn (Y/n)? " response
    while [[ -z "$(echo ${response:-Y} | grep -i '^[yn]')" ]]; do
	read -p "Please enter (Y)es or (n)o: " response
    done

    if [[ -n "$(echo ${response:-Y} | grep -i '^n')" ]]; then
	echo
	continue;
    fi

    # Ask for the "cleanName" -- the name this device will have in the config files
    read -p "Please enter the matterhorn name for the ${devName[$i]} ($cleanName): " tempName
    while [[ -z "$(echo ${tempName:-$cleanName} | grep -v '[()\ ]')" ]]; do
	read -p "Please enter a name without parentheses or whitespaces ($cleanName): " tempName
    done
    cleanName=${tempName:-$cleanName}

    read -p "Please enter the flavor assigned to the device ${devName[$i]}: " flavor
    # Grep matches anything that has two fields consisting of exclusively alphanumeric characters or underscores, separated by a single slash '/'
    while [[ -z $(echo $flavor | grep '^[^/][^/]*/[^/][^/]*$') ]]; do
	read -p "Invalid syntax. The flavors follow the pattern <prefix>/<suffix>: " flavor
    done
    echo

    echo "capture.device.$cleanName.src=${device[$i]}" >> $CAPTURE_PROPS
    echo "capture.device.$cleanName.outputfile=$cleanName" >> $CAPTURE_PROPS
    echo "capture.device.$cleanName.flavor=$flavor" >> $CAPTURE_PROPS
    allDevices="${allDevices}${cleanName},"

    # Prompt for choosing the video standard
    # First expression: filters the lines within the paragraph starting with the word "standards" and ending in a empty line
    # Second expression: filters the lines containing the word "name"
    # Third expression: matches the whole line, but substitutes it by only the standard name
    # Fourth expression: substitutes the whitespaces in the name by underscores, to avoid problems with arrays in bash 
    standards=( $(v4l-info ${device[$i]} 2> /dev/null | sed -e '/^standards/,/^$/!d' -e '/name/!d' -e 's/^\s*name\s*:\s*\"\(.*\)\"/\1/' -e 's/ /_/g') )
    if [[ ${#standards[@]} -gt 1 ]]; then
	unset std
	echo "Please choose the output standard for the device ${devName[$i]}"
	for (( j = 0; j < ${#standards[@]}; j++ )); do
	    echo "   $j) ${standards[$j]}"
	done
	echo
	read -p "Selection: " std
	
	until [[ $(echo $std | grep -o '^[0-9][0-9]*$') && $std -ge 0 && $std -lt ${#standards[@]} ]]; do 
	    read -p "Please choose one of the numbers in the list: " std
	done
	
	v4l2-ctl -s ${standards[$std]} -d ${device[$i]} > /dev/null
	if [[ $? -ne 0 ]]; then
	    echo "Error. Standard ${standards[$std]} not set. Please try to set it manually"
	else
	    echo "v4l2-ctl -s ${standards[$std]} -d ${device[$i]}" >> $config
	    echo "Standard ${standards[$std]} set for the device ${devName[$i]}"
	fi
	echo
    fi

    #Select input to use with the card
    # First expression: filters the lines within the paragraph "channels", ending in a emptyline
    # Second expression: filters the lines containing the word 'name'
    # Third expression: matches the whole line, but substitutes it by only the device name
    # Fourth expression: substitutes the whitespaces in the name by underscores, to avoid problems with arrays in bash
    inputs=( $(v4l-info ${device[$i]} 2> /dev/null | sed -e '/^channels/,/^$/!d' -e '/name/!d' -e 's/^\sname\s*:\s*\"\(.*\)\"/\1/' -e 's/ /_/g') )
    if [[ ${#inputs[@]} -gt 1 ]]; then 
	echo "Please select the input number to be used with the ${devName[$i]}"
	for (( j = 0; j < ${#inputs[@]}; j++ )); do
	    echo "   $j) ${inputs[$j]}"
	done
	echo
	read -p "Selection: " chosen_input
	
	until [[ $(echo $chosen_input | grep -o '^[0-9][0-9]*$') && $chosen_input -ge 0 && $chosen_input -lt ${#inputs[@]} ]]; do 
	    read -p "Please choose one of the numbers in the list: " chosen_input
	done

	v4l2-ctl -d ${device[$i]} -i $chosen_input > /dev/null
	echo "v4l2-ctl -d ${device[$i]} -i $chosen_input" >> $config
	echo "Using input $chosen_input with the ${devName[$i]}."
	echo
    fi
    echo
done

mv $rules $DEV_RULES
chown root:video /$DEV_RULES


# Audio device
audioLine=$(arecord -l| grep Analog -m 1)
audioDevice="hw:$(echo $audioLine | cut -d ':' -f 1 | cut -d ' ' -f 2)"
# The syntax is cumbersome, but it just keeps the fields surrounded by "[" and "]" and outputs them in the form "first (second)"
audioDevName=$(echo $audioLine | sed 's/^[^[]*\[\([^]]*\)\][^[]*\[\([^]]*\)\]$/\1 (\2)/')
cleanAudioDevice=$(echo $audioDevName | sed -e 's/\s//g' -e 's/[\(|\)]/_/g')

# Ask the user whether or not they want to configure this device
unset response
read -p "Audio device ${audioDevName} has been found. Do you want to configure it for matterhorn (Y/n)? " response
while [[ -z "$(echo ${response:-Y} | grep -i '^[yn]')" ]]; do
    read -p "Please enter (Y)es or (n)o: " response
done

if [[ -n "$(echo ${response:-Y} | grep -i '^y')" ]]; then
    # Ask for the "cleanName" -- the name this device will have in the config files
    read -p "Please enter the matterhorn name for the ${audioDevName} ($cleanAudioDevice): " response
    while [[ -z "$(echo "${response:-$cleanName}" | grep -v '[()\ ]')" ]]; do
	read -p "Please enter a name without parentheses or whitespaces ($cleanAudioDevice): " response
	cleanAudioDevice=${response:-$cleanAudioDevice}
    done
    echo
    
    read -p "Please enter the flavor assigned to ${cleanAudioDevice}: " flavor
    # Grep matches anything that has two fields consisting of exclusively alphanumeric characters or underscores, separated by a single slash '/'
    while [[ -z $(echo $flavor | grep '^[^/][^/]*/[^/][^/]*$') ]]; do
	read - p "Invalid syntax. The flavors follow the pattern <prefix>/<suffix>: " flavor
    done
    echo
    
    echo "capture.device.$cleanAudioDevice.src=$audioDevice" >> $CAPTURE_PROPS
    echo "capture.device.$cleanAudioDevice.outputfile=$cleanAudioDevice" >> $CAPTURE_PROPS
    echo "capture.device.$cleanAudioDevice.flavor=$flavor" >> $CAPTURE_PROPS

    allDevices="${allDevices}${cleanAudioDevice}"
fi


echo "capture.device.names=${allDevices}" >> $CAPTURE_PROPS
