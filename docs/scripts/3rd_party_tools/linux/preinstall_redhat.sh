#!/bin/sh

# parameters for installation of 3rd party tools
install_parameters=""

# Checking for jam package
# If not available set parameter for building out of source.
jam_check ()
{
	yum list jam
	if [ $? -eq 0 ]
	then
		su -c "yum -y install jam"
		return $?
	else
		echo
		echo "Jam not available from repository. It will be built from source."
		install_parameters="${install_parameters} jam"
		return 0
	fi
}

echo
echo "This is preinstallation script for installing required tools and libraries"
echo "for building 3rd party tools on Red Hat based linux system with yum as package"
echo "manager. If script was not executed as root, you will be asked to provide root"
echo "password for installing reqired packages."
echo
sleep 5

if
	# installing prerequisite tools
	su -c "yum -y install gcc gcc-c++ pkgconfig zlib-devel byacc subversion patch wget" &&
	# installing required libraries
	su -c "yum -y install libjpeg-devel libpng-devel libtiff-devel" &&
	# checking for jam package
	jam_check &&
	# installing gstreamer
	su -c "yum -y install gstreamer gstreamer-plugins-*"
then
	echo
	echo "Required libraries were installed. Executing main script..."
	echo
	sleep 1 
	su -c "sh install_3rd_party.sh $install_script_parameters 2>&1"
	exit $?
else
	echo
	echo "Installation of required libaries failed!"
	echo "3rd party tools will not be installed."
	echo
	exit 1
fi
