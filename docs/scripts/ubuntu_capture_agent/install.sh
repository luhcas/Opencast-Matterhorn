#! /bin/bash
# Configure capture agent for use with Matterhorn


SETUP_DEVICES=$PWD/setup_devices.sh
BUILD_MATTERHORN=$PWD/build_matterhorn.sh

# prompt for user name
USERNAME=matterhorn
echo -n "Input desired opencast username (matterhorn): "
read input
if [ "$input" != "" ];
  then
  USERNAME=$input
fi
export $USERNAME
echo "Username: $USERNAME"

# add user and give sudo priveleges
sudo useradd -m -s /bin/bash $USERNAME
if [ $? -ne 9 ]; then
  echo "Enter $USERNAME's new password"
  sudo passwd $USERNAME
fi
sudo usermod -aG admin,video,audio $USERNAME
cd /home/$USERNAME

lsmod | grep -e "^vga2usb" &> /dev/null
if [ "$?" -ne 0 ]; then

	# list of common drivers
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

	# list of kernel versions using `uname -r`;`uname -m`
	# these are mapped to drivers above
	kernels[0]="2.6.31-16-generic;i686|2.6.31-16-generic;i386"
	kernels[1]=""
	kernels[2]="2.6.31-14-generic;i686|2.6.31-14-generic;i386"
	kernels[3]="2.6.31-14-generic;x86_64"
	
	# determine which vga2usb driver to load for this kernel
	# if this variable remains 0, we attempt to load the driver
	LOAD_DRIVER=0
	EPIPHAN=""
	KERNEL=`uname -r`
	ARCH=`uname -m`
	EPIPHAN_HW="$KERNEL;$ARCH"
	EPIPHAN_DEFAULT=9

	# if current kernel matches common driver, suggest it
	for ((i = 0; i < ${#kernels[@]}; i++ ))
  	do
    	test="$(echo "${kernels[$i]}" | grep $EPIPHAN_HW)"
    	if [ "$test" != "" ]; then
        EPIPHAN_DEFAULT=$i
    	fi
	done

	# let user choose driver
	echo "System information: `uname -a`"
	echo "Here is a list of supported Epiphan VGA2USB drivers:"
	for ((i = 0; i < ${#drivers[@]}; i++ ))
	do
  	echo -e "\t($i)\t${drivers[$i]}"
	done
	echo -n "Choose an option (suggesting driver #$EPIPHAN_DEFAULT): "
	read opt
	if [[ $opt -ge 0 && $opt -lt 9 ]]; then
  	DRIVER_URL="http://www.epiphan.com/downloads/linux/${drivers[$opt]}"
  	EPIPHAN="${drivers[$opt]}"
	elif [ $opt -eq 9 ]; then
  	echo -n "Please input the URL of the driver you would like to load: "
  	read url
  	DRIVER_URL="$url"
  	EPIPHAN=${DRIVER_URL##*/}
	else
		LOAD_DRIVER=1
	fi
	
	# attempt to load the vga2usb driver
	SUCCESS=0
	if [ $LOAD_DRIVER -eq 0 ]; then
		echo "Loading driver $EPIPHAN"
  	sudo -u $USERNAME mkdir -p drivers
  	sudo -u $USERNAME wget $DRIVER_URL
  	if [ $? -ne 0 ]; then
    	SUCCESS=1
  	fi
  	sudo -u $USERNAME mv $EPIPHAN drivers/
  	cd drivers/
  	sudo -u $USERNAME tar jxf $EPIPHAN
  	sudo -u $USERNAME sed -i '/sudo \/sbin\/insmod/s|$| num_frame_buffers=2|' Makefile
  	sudo make load
  	if [ $? -ne 0 ]; then
    	SUCCESS=1
  	fi
  	cd ..
	fi

	if [ $SUCCESS -ne 0 ]; then
  	echo "Failed to load Epiphan driver. Try to do it manually."
		exit 1
	fi
else
	echo "VGA2USB driver already installed."
fi

sudo cp /etc/apt/sources.list /etc/apt/sources.list.backup
sudo echo "deb http://us.archive.ubuntu.com/ubuntu/ karmic main restricted universe multiverse" >> $HOME/sources.list
sudo echo "deb http://us.archive.ubuntu.com/ubuntu/ karmic-updates main restricted universe multiverse" >> $HOME/sources.list
sudo echo "deb http://security.ubuntu.com/ubuntu karmic-security main restricted universe multiverse" >> $HOME/sources.list
sudo mv $HOME/sources.list /etc/apt/sources.list

# auto set selections when installing postfix and jdk packages
# if this looks confusing to you, you're not alone
sudo debconf-set-selections <<\EOF
postfix postfix/mailname string fax
postfix postfix/main_mailer_type select Internet Site
EOF
sudo debconf-set-selections <<EOF
sun-java5-jdk shared/accepted-sun-dlj-v1-1 boolean true
?sun-java6-jdk shared/accepted-sun-dlj-v1-1 boolean true
EOF

echo -n "Installing third party packages from Ubuntu repository..."
sudo apt-get update > /dev/null
sudo apt-get -y --force-yes install alsa-utils v4l-conf ivtv-utils maven2 sun-java6-jdk subversion wget curl openssh-server gcc gstreamer0.10-plugins* gstreamer0.10-ffmpeg > /dev/null

export JAVA_HOME=/usr/lib/jvm/`ls /usr/lib/jvm | grep java-6-sun-1`
export FELIX_FILENAME=org.apache.felix.main.distribution-2.0.4.tar.gz
export FELIX_URL=http://apache.mirror.iweb.ca/felix/$FELIX_FILENAME
export FELIX_HOME=/home/$USERNAME/felix-framework-2.0.4
export M2_REPO=/home/$USERNAME/.m2/repository

# setup maven and felix environment for matterhorn user
EXPORT_M2_REPO="export M2_REPO=${M2_REPO}"
EXPORT_FELIX_HOME="export FELIX_HOME=${FELIX_HOME}"
EXPORT_JAVA_HOME="export JAVA_HOME=${JAVA_HOME}"
sudo chmod o+w /home/$USERNAME/.bashrc
grep -e "${EXPORT_M2_REPO}" /home/$USERNAME/.bashrc &> /dev/null
if [ "$?" -ne 0 ]; then
	sudo -u $USERNAME echo "${EXPORT_M2_REPO}" >> /home/$USERNAME/.bashrc
fi
grep -e "${EXPORT_FELIX_HOME}" /home/$USERNAME/.bashrc &> /dev/null
if [ "$?" -ne 0 ]; then
	sudo -u $USERNAME echo "${EXPORT_FELIX_HOME}" >> /home/$USERNAME/.bashrc
fi
grep -e "${EXPORT_JAVA_HOME}" /home/$USERNAME/.bashrc &> /dev/null
if [ "$?" -ne 0 ]; then
	sudo -u $USERNAME echo "${EXPORT_JAVA_HOME}" >> /home/$USERNAME/.bashrc
fi
echo "done"

# setup felix
echo -n "Setting up Felix..."
cd /home/$USERNAME
if [ -d ${FELIX_HOME} ]; then
  sudo rm -rf ${FELIX_HOME}
fi
sudo -u $USERNAME curl -s ${FELIX_URL} | sudo -u $USERNAME tar xz
sudo -u $USERNAME mkdir ${FELIX_HOME}/load
echo "done"

# setup jv4linfo
if [ ! -e /usr/lib/libjv4linfo.so ]; then
	echo -n "Installing jv4linfo..."
	sudo -u $USERNAME wget -q http://luniks.net/luniksnet/download/java/jv4linfo/jv4linfo-0.2.1-src.jar 
	sudo -u $USERNAME jar xf jv4linfo-0.2.1-src.jar
	cd jv4linfo/src
	# The ant build script has a hardcoded path to the openjdk, this sed line will
	# switch it to be whatever is defined in JAVA_HOME
	sudo -u $USERNAME sed -i '74i\\t<arg value="-fPIC"/>' build.xml
	sudo -u $USERNAME sed -i "s#\"\/usr\/lib\/jvm\/java-6-openjdk\/include\"#\"$JAVA_HOME\/include\"#g" build.xml
	
	sudo -u $USERNAME ant -lib ${JAVA_HOME}/lib &> /dev/null
	if [ "$?" -ne 0 ]; then
		echo "Error building libjv4linfo.so"
		exit
	fi
	sudo cp ../lib/libjv4linfo.so /usr/lib
	
	cd ../..
	echo "done"
else
	echo "libjv4linfo.so already installed"
fi

# build properties
sudo -u $USERNAME ${BUILD_MATTERHORN} ${FELIX_HOME} ${JAVA_HOME}

# setup properties by calling setup_devices.sh
sudo -u $USERNAME ${SETUP_DEVICES} ${FELIX_HOME} ${JAVA_HOME}

# set matterhorn to start on boot
cd $HOME
echo "# start the matterhorn capture agent" >> matterhorn.conf
echo "env FELIX_HOME=${FELIX_HOME}" >> matterhorn.conf
echo "env M2_REPO=${M2_REPO}" >> matterhorn.conf
echo "env JAVA_HOME=${JAVA_HOME}" >> matterhorn.conf
echo "start on runlevel [2345]" >> matterhorn.conf
echo "stop on runlevel [!2345]" >> matterhorn.conf
echo "expect fork" >> matterhorn.conf
echo "script" >> matterhorn.conf
echo "make -C /home/$USERNAME/drivers reload" >> matterhorn.conf
echo "/home/$USERNAME/device_config.sh" >> matterhorn.conf
echo "su matterhorn -c \"exec /bin/bash /home/matterhorn/felix-framework-2.0.4/bin/start_matterhorn.sh\" &" >> matterhorn.conf
echo "end script" >> matterhorn.conf
sudo mv matterhorn.conf /etc/init
sudo chown root:root /etc/init/matterhorn.conf

