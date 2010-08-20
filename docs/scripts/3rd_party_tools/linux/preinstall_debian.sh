#!/bin/sh

echo
echo "This is preinstallation script for installing required tools and libraries"
echo "for building 3rd party tools on Debian based linux system with apt-get as"
echo "package manager. If script was not executed as root, you will be asked to"
echo "provide root password for installing required packages."
echo
sleep 5

if
	# installing prerequisite tools
	sudo apt-get -y install zlib1g-dev gcc g++ build-essential bzip2 pkg-config byacc subversion patch &&
	# installing required libraries
	sudo apt-get -y install libjpeg-dev libpng12-dev libtiff4-dev jam libaspell-dev &&
	# installing gstreamer
	sudo apt-get -y install libgstreamer0.10-0 gstreamer0.10-plugins-base gstreamer0.10-plugins-good
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
