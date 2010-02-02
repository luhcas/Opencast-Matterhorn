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
		test="$(sudo v4l-info $line 2> /dev/null | grep name | grep -o '".*"' | sed s/\"//g | grep "$device")"
	
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
  if [ "$name" == "${supportedDevices[1]}" ];
    then
      echo -n "Would you like device $name to use the NTSC standard? (Y/n) "
      read mode
      if [ "$mode" == "Y" ];
        then
          v4l2-ctl -s NTSC-M -d $device
      fi
  fi
 done

audioDevice=hw:$(sudo arecord -l | grep Analog | cut -c 6)
cleanAudioDevice=`echo $audioDevice | sed s/\://g`
echo "capture.device.$cleanAudioDevice.src=$audioDevice" >> capture.properties
echo "capture.device.$cleanAudioDevice.outputfile=$cleanAudioDevice" >> capture.properties

# setup opencast directories
OC_DIR=$PWD
echo -n "Where you like the opencast configuration to be stored (Leave blank for `echo $PWD`)? "
read directory
if [ "$directory" != "" ];
  then
    OC_DIR=$directory
    mkdir -p $OC_DIR
fi
mkdir -p $OC_DIR/cache
echo "capture.filesystem.cache.url=$OC_DIR/cache" >> capture.properties
mkdir -p $OC_DIR/config
echo "capture.filesystem.config.url=$OC_DIR/config" >> capture.properties
mkdir -p $OC_DIR/volatile
echo "capture.filesystem.volatile.url=$OC_DIR/volatile" >> capture.properties
mkdir -p $OC_DIR/cache/captures
echo "capture.filesystem.cache.capture.url=$OC_DIR/cache/captures" >> capture.properties

mv capture.properties $OC_DIR/cache

# define capture agent name by using the hostname
echo "capture.agent.name=`hostname`" >> capture.properties
