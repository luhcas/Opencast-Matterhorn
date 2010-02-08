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
  
  # setup device info using udevadm info
  realpath=/sys$(udevadm info --query=path --name=$device)
  symlinkName=$(echo $cleanName | cut -b -5 | tr "[:upper:]" "[:lower:]")
  vendor=$(cat $realpath/../../vendor)
  sysdevice=$(cat $realpath/../../device)
  if [ $? -eq 0 ]; then
    echo "KERNEL==\"video[0-9]\", SYSFS{vendor}==\"$vendor\", SYSFS{device}==\"$sysdevice\", SYMLINK+=\"$symlinkName\"" >> /home/$USERNAME/95-perso.rules
    device="/dev/$symlinkName"
  fi

	echo -n "Device $name has been found, should it be called $cleanName? (Y/n) "
	read response
	if [ "$response" == "n" ];
	 then
		echo -n "Please enter the new name: "
		read cleanName
	fi
	echo "capture.device.$cleanName.src=$device" >> /home/$USERNAME/capture.properties
	echo "capture.device.$cleanName.outputfile=$cleanName" >> /home/$USERNAME/capture.properties
  if [ "$name" != "${supportedDevices[2]}" ];
    then
      echo -n "Would you like device $name to be (N)TSC or (P)AL? (N/p) "
      read mode
      if [[ "$mode" == "p" || "$mode" == "P" ]];
        then
          sudo v4l2-ctl -s 255 -d $device 2> /dev/null # set to PAL mode
          echo "sudo v4l2-ctl -s 255 -d $device" >> /home/$USERNAME/matterhorn_capture.sh
      else
        sudo v4l2-ctl -s NTSC-M -d $device 2> /dev/null
        sudo echo "v4l2-ctl -s NTSC-M -d $device" >> /home/$USERNAME/matterhorn_capture.sh
    fi

    if [ "$name" == "${supportedDevices[0]}" ]; then
      sudo v4l2-ctl -d $device -i 2
      echo "v4l2-ctl -d $device -i 2" >> /home/$USERNAME/matterhorn_capture.sh
      echo "Please use input 2 with the $name."
    else
      sudo v4l2-ctl -d $device -i 0
      echo "v4l2-ctl -d $device -i 0" >> /home/$USERNAME/matterhorn_capture.sh
      echo "Please use input 0 with the $name."
    fi
  fi
done

sudo mv /home/$USERNAME/95-perso.rules /etc/udev/rules.d
sudo chown root:root /etc/udev/rules.d/95-perso.rules

chmod 755 /home/$USERNAME/matterhorn_capture.sh

audioDevice=hw:$(sudo arecord -l | grep Analog | cut -c 6)
cleanAudioDevice=`echo $audioDevice | sed s/\://g`
echo "capture.device.$cleanAudioDevice.src=$audioDevice" >> /home/$USERNAME/capture.properties
echo "capture.device.$cleanAudioDevice.outputfile=$cleanAudioDevice" >> /home/$USERNAME/capture.properties

# setup opencast directories
OC_DIR="/opencast"
echo -n "Where you like the opencast configuration to be stored (Leave blank for /opencast)? "
read directory
if [ "$directory" != "" ];
  then
    OC_DIR="$directory"
fi
sudo mkdir -p $OC_DIR
sudo mkdir -p $OC_DIR/cache
echo "capture.filesystem.cache.url=$OC_DIR/cache" >> /home/$USERNAME/capture.properties
sudo mkdir -p $OC_DIR/config
echo "capture.filesystem.config.url=$OC_DIR/config" >> /home/$USERNAME/capture.properties
sudo mkdir -p $OC_DIR/volatile
echo "capture.filesystem.volatile.url=$OC_DIR/volatile" >> /home/$USERNAME/capture.properties
sudo mkdir -p $OC_DIR/cache/captures
echo "capture.filesystem.cache.capture.url=$OC_DIR/cache/captures" >> /home/$USERNAME/capture.properties

# define capture agent name by using the hostname
echo "capture.agent.name=`hostname`" >> capture.properties

sudo mv /home/$USERNAME/capture.properties $OC_DIR/cache/capture.properties
