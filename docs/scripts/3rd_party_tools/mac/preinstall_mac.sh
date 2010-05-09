#!/bin/sh

echo
echo "This is preinstallation script for installing required tools and libraries"
echo "for building 3rd party tools on MacOS with installed developer tools and"
echo "MacPorts. If script was not executed as root, you will be asked to"
echo "provide root password for installing required packages."
echo
sleep 5

if
	# installing prerequisite tools
	sudo port install zlib pkgconfig byacc subversion &&
	# installing required libraries
	sudo port install jpeg libpng tiff jam &&
	# installing gstreamer
	sudo port install gstreamer gst-plugins-*
then
	echo
	echo "Required libraries were installed. Executing main script..."
	echo
	sleep 1
	sudo sh install_3rd_party.sh 2>&1
	exit $?
else
	echo
	echo "Installation of required libaries failed!"
	echo
	exit 1
fi
