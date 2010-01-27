#!/bin/bash

supportedDevices[0]="Hauppauge WinTV PVR-350"
supportedDevices[1]="BT878 video (ProVideo PV143)"
supportedDevices[2]="Epiphan VGA2USB"

#ls the dev directory, then grep for video devices with *only* one number and dump the result to a file
ls /dev/video* | grep '/dev/video[0-9]$' > /tmp/devlist.txt

#Read each line in the file.  Note that if we move the call from above to this line the devices array gets all kinds of scoping problems.
while read line
 do
	let aryLen=${#supportedDevices[@]}-1
	for item in $(seq 0 1 $aryLen)
	 do
		device="${supportedDevices[$item]}"
		test="$(v4l-info $line 2> /dev/null | grep name | grep -o '".*"' | sed s/\"//g | grep "$device")"
	
		if [ "$test" == "$device" ];
		 then
			devices[$item]="$device|$line"
		fi
	done
done < /tmp/devlist.txt


let devAryLen=${#devices[@]}-1
for dev in $(seq 0 1 $devAryLen)
 do
	name="$(echo ${devices[$dev]} | grep -o '.*|' | sed s/\|//g)"
	cleanName="$(echo $name | sed s/\ //g)"
	device="$(echo ${devices[$dev]} | grep -o '|.*' | sed s/\|//g)"
	echo -n "Device $name has been found, should it be called $cleanName? (Y/n) "
	read response
	if [ "$response" == "n" ];
	 then
		echo -n "Please enter the new name: "
		read cleanName
	fi
	echo "capture.device.$cleanName.src=$device" >> capture.properties
	echo "capture.device.$cleanName.outputfile=$cleanName" >> capture.properties
 done

audioDevice=hw:$(sudo arecord -l | grep Analog | cut -c 6)
cleanAudioDevice=`echo $audioDevice | sed s/\://g`
echo "capture.device.$cleanAudioDevice.src=$audioDevice" >> capture.properties
echo "capture.device.$cleanAudioDevice.outputfile=$cleanAudioDevice" >> capture.properties
