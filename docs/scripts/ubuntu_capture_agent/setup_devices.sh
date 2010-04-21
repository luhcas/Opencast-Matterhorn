#!/bin/bash

supportedDevices[0]="Hauppauge WinTV PVR-350"
supportedDevices[1]="BT878 video (ProVideo PV143)"
supportedDevices[2]="Epiphan VGA2USB"
supportedDevices[3]="Hauppauge HVR-1600"
supportedDevices[4]="Hauppauge WinTV PVR-150"
supportedDevices[5]="Hauppauge WinTV-HVR1300 DVB-T/H"

#ls the dev directory, then grep for video devices with *only* one number and dump the result to a file
ls /dev/video* | grep '/dev/video[0-9]$' > /tmp/devlist.txt

#Read each line in the file.  Note that if we move the call from above to this line the devices array gets all kinds of scoping problems.
#FIXME: The Hauppage, as it is two devices, appears duplicated. Should only appear once
i=0
while reline
 do         
	let aryLen=${#supportedDevices[@]}-1
	for item in $(seq 0 1 $aryLen)
	 do
		device="${supportedDevices[$item]}"
		test="$(sudo v4l-info $line 2> /dev/null | grep name | grep -o '".*"' | sed s/\"//g | grep "$device")"
	
		if [ "$test" == "$device" ];
		 then
			devices[$i]="$device|$line"
			i=$(($i+1))
		fi
	done
done < /tmp/devlist.txt


CAPTURE_PROPS=$1/conf/services/org.opencastproject.capture.impl.ConfigurationManager.properties
GEN_PROPS=$1/conf/config.properties
sed -i "/capture.device/d" $CAPTURE_PROPS

touch /home/$USERNAME/95-perso.rules
rm -f /home/$USERNAME/device_config.sh
touch /home/$USERNAME/device_config.sh
allDevices=""
let devAryLen=${#devices[@]}-1
for dev in $(seq 0 1 $devAryLen)
 do
	name="$(echo ${devices[$dev]} | grep -o '.*|' | sed s/\|//g)"
	cleanName="$(echo $name | sed s/\ //g | sed s/\(/_/g | sed s/\)/_/g)"
	device="$(echo ${devices[$dev]} | grep -o '|.*' | sed s/\|//g)"
  
  # setup device info using udevadm info
  realpath=/sys$(udevadm info --query=path --name=$device)
  symlinkName=$(echo $cleanName | cut -b -5 | tr "[:upper:]" "[:lower:]")
  vendor=$(cat $realpath/../../vendor 2> /dev/null)
  sysdevice=$(cat $realpath/../../device 2> /dev/null)
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
	echo "capture.device.$cleanName.src=$device" >> $CAPTURE_PROPS
	echo "capture.device.$cleanName.outputfile=$cleanName" >> $CAPTURE_PROPS
	echo "capture.device.$cleanName.flavor=presentation/source" >> $CAPTURE_PROPS
  allDevices="${allDevices}${cleanName},"
  if [ "$name" != "${supportedDevices[2]}" ];
    then
      echo -n "Would you like device $name to be (N)TSC or (P)AL? (N/p) "
      read mode
      if [[ "$mode" == "p" || "$mode" == "P" ]];
        then
          sudo v4l2-ctl -s 255 -d $device 2> /dev/null # set to PAL mode
          echo "sudo v4l2-ctl -s 255 -d $device" >> /home/$USERNAME/device_config.sh
      else
        sudo v4l2-ctl -s NTSC-M -d $device 2> /dev/null
        sudo echo "v4l2-ctl -s NTSC-M -d $device" >> /home/$USERNAME/device_config.sh
    fi

    if [ "$name" == "${supportedDevices[0]}" -o "$name" == "${supportedDevices[5]}" ]; then
      sudo v4l2-ctl -d $device -i 2
      echo "v4l2-ctl -d $device -i 2" >> /home/$USERNAME/device_config.sh
      echo "Please use input 2 with the $name."
    else
      sudo v4l2-ctl -d $device -i 0
      echo "v4l2-ctl -d $device -i 0" >> /home/$USERNAME/device_config.sh
      echo "Please use input 0 with the $name."
    fi
  fi
done

sudo mv /home/$USERNAME/95-perso.rules /etc/udev/rules.d
sudo chown root:video /etc/udev/rules.d/95-perso.rules

chmod 755 /home/$USERNAME/device_config.sh

audioDevice=hw:$(sudo arecord -l| grep Analog |  cut --delimiter=' ' -f 2 | sed  's/://g')
cleanAudioDevice=`echo $audioDevice | sed s/\://g`
echo "capture.device.$cleanAudioDevice.src=$audioDevice" >> $CAPTURE_PROPS
echo "capture.device.$cleanAudioDevice.outputfile=$cleanAudioDevice" >> $CAPTURE_PROPS
echo "capture.device.$cleanAudioDevice.flavor=presentation/source" >> $CAPTURE_PROPS

allDevices="${allDevices}${cleanAudioDevice}"
echo "capture.device.names=${allDevices}" >> $CAPTURE_PROPS

# setup opencast directories
OC_DIR="/opencast"
echo -n "Where you like the opencast configuration to be stored (Leave blank for /opencast)? "
read directory
if [ "$directory" != "" ];
  then
    OC_DIR="$directory"
fi
sudo mkdir -p $OC_DIR
sudo chown $USERNAME:$USERNAME /opencast
sudo -u $USERNAME mkdir -p $OC_DIR/cache
sudo -u $USERNAME mkdir -p $OC_DIR/config
sudo -u $USERNAME mkdir -p $OC_DIR/volatile
sudo -u $USERNAME mkdir -p $OC_DIR/cache/captures

# define capture agent name by using the hostname
sed -i s/capture\.agent\.name.*/capture\.agent\.name=`hostname`/g $CAPTURE_PROPS

# Prompt for core hostname. default to localhost:8080                                                                                                        
read -p "Please enter Matterhorn Core hostname: (http://localhost:8080) " core
# Escapes the special characters so that sed does not interpret them
core=$(echo ${core:-"http://localhost:8080"} | sed 's/\([\/\.\-]\)/\\\1/g')
sed -i "s/\(org\.opencastproject\.server\.url=\).*$/\1$core/" $GEN_PROPS
