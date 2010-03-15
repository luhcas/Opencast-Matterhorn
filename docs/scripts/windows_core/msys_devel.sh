#! /bin/sh

#################################################
############## Configurations ###################
#################################################

#path to your local msys + mingw installation
MSYS_PATH="/c/Matterhorn"

#################################################
################ Functions ######################
################################################# 

clean ()
{
	echo
	echo Cleaning...
	cd $working_dir
	if [ -f m4-1.4.12.tar.bz2 ]
	then
		rm -f m4-1.4.12.tar.bz2
	fi
	if [ -d m4-1.4.12 ]
	then
		rm -rf m4-1.4.12
	fi
}

#################################################
############## Building m4 ######################
#################################################

echo
echo This script will build m4 reqired for rebuilding
echo autoconfiguration tools on MSYS + MinGW system.
echo

sleep 5
working_dir=$(pwd) 
if wget http://ftp.gnu.org/gnu/m4/m4-1.4.12.tar.bz2 && 
tar xfvj m4-1.4.12.tar.bz2 && 
patch -p0 < M4-1.4.12-MSYS.diff &&
cd m4-1.4.12 && 
./configure && 
make && 
cp ./src/m4.exe -f $MSYS_PATH/msys/bin/m4.exe
then
	clean
	echo
	echo Building m4 successfully completed. You are now ready
	echo to build 3rd party tools. New shell will open where you
	echo can execute 'sh install_3rd_party.sh'. After this shell
	echo is closed you can delete directory $MSYS_PATH/msysDVLPR
	sleep 5
	# start $MSYS_PATH/msys/msys.bat -norxvt
	exit
else
	clean
	echo -----------------------
	echo - Building m4 failed! -
	echo -----------------------
fi