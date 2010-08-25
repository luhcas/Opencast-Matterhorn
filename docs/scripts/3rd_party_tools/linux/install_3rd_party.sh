#!/bin/sh

############################################
############# Configurations ###############
############################################

# ---- directories -----

base_dir=$(pwd)
mkdir -p /tmp/3rd_party
cd /tmp/3rd_party
working_dir=$(pwd)

# ---- 3rd party repository -----
thirdparty_repository="http://downloads.opencastproject.org/3rd%20Party"

# ---- 3rd party tools version ----

# libpng
libpng_version="1.2.26"

# libjpeg
libjpeg_version="6b"

# libtiff
libtiff_version="3.8.2"

# jam
jam_version="2.5"

# ffmpeg
ffmpeg_revision="22592"

# libswscale
libswscale_revision="30929"

# mediainfo
mediainfo_version="0.7.19"

# tesseract
tesseract_version="2.01"
tesseract_lang_version="2.00.eng"

# ocropus
ocropus_revision="r644"

# mp4v2
mp4v2_revision="r355"

# qtsbtlembedder
qtsbtlembedder_version="0.3"

# ---- 3rd party tools properties ----

libpng_url="${thirdparty_repository}/libpng${libpng_version}.tar.gz"

libjpeg_url="${thirdparty_repository}/jpegsrc.v${libjpeg_version}.tar.gz"

libtiff_url="${thirdparty_repository}/tiff${libtiff_version}.zip"

jam_url="${thirdparty_repository}/jam${jam_version}.zip"

ffmpeg_url="svn://svn.ffmpeg.org/ffmpeg/trunk"
libswscale_url="svn://svn.mplayerhq.hu/mplayer/trunk/libswscale"

mediainfo_url="${thirdparty_repository}/MediaInfo_CLI_${mediainfo_version}_GNU_FromSource.tar.bz2"

tesseract_url="${thirdparty_repository}/tesseract${tesseract_version}.tar.gz"
tesseract_lang_url="${thirdparty_repository}/tesseract${tesseract_lang_version}.tar.gz"

ocropus_url="${thirdparty_repository}/ocropus${ocropus_revision}.tar.gz"

mp4v2_url="${thirdparty_repository}/mp4v2trunk${mp4v2_revision}.tar.bz2"

qtsbtlembedder_url="${thirdparty_repository}/qt_sbtl_embedder${qtsbtlembedder_version}.tar.gz"

########################################################
######## 3rd party tools installation script ###########
########################################################

# libpng

setup_libpng ()
{
	echo
	echo "Installing libpng $libpng_version"
	echo
	mkdir -p ${working_dir}/libpng
	cd ${working_dir}/libpng
	if ! [ -f ./libpng-${libpng_version}/png.h ]
	then
		wget $libpng_url &&
		tar zxfv libpng${libpng_version}.tar.gz
		if ! [ $? -eq 0 ]
		then
			cd $working_dir
			rm -rf libpng
			return 1
		fi
	fi
	cd libpng-${libpng_version}
	if ! [ -f ./libpng.la ]
	then
		./configure &&
		make
		if ! [ $? -eq 0 ]
		then
			make clean
			cd $working_dir
			return 1
		fi
	fi
	if make install
	then
		echo
		echo "Libpng installed successfully."
		cd $working_dir
		return 0
	else
		echo
		echo "Libpng installation failed."
		cd $working_dir
		return 1
	fi
}

# libjpeg

setup_libjpeg ()
{
	echo
	echo "Installing libjpeg $libjpeg_version"
	echo
	mkdir -p ${working_dir}/libjpeg
	cd ${working_dir}/libjpeg
	if ! [ -f ./jpeg-${libjpeg_version}/jpeglib.h ]
	then
		cp ${base_dir}/patches/libjpeg-6b.patch . &&
		wget $libjpeg_url &&
		tar zxfv jpegsrc.v${libjpeg_version}.tar.gz
		if ! [ $? -eq 0 ]
		then
			cd $working_dir
			rm -rf libjpeg
			return 1
		fi
	fi
	cd jpeg-${libjpeg_version}
	if ! [ -f ./libjpeg.a ]
	then
		./configure &&
		make
		patch -p0 -N Makefile ../libjpeg-6b.patch
		if ! [ $? -eq 0 ]
		then
			make clean
			cd $working_dir
			return 1
		fi
	fi
	if make install install-lib
	then
		echo
		echo "Libjpeg installed successfully."
		cd $working_dir
		return 0
	else
		echo
		echo "Libjpeg installation failed."
		cd $working_dir
		return 1
	fi
}

# libtiff

setup_libtiff ()
{
	echo
	echo "Installing libtiff $libtiff_version"
	echo
	mkdir -p ${working_dir}/libtiff
	cd ${working_dir}/libtiff
	if ! [ -f ./tiff-$libtiff_version/autogen.sh ]
	then
		wget $libtiff_url &&
		unzip tiff$libtiff_version.zip
		if ! [ $? -eq 0 ]
		then 
			cd ${working_dir}
			rm -rf libtiff
			return 1
		fi
	fi
	cd tiff-$libtiff_version
	if ! [ -f ./libtiff/libtiff.la ]
	then
		./configure &&
		make
		if ! [ $? -eq 0 ]
		then
			make clean
			cd ${working_dir}
			return 1
		fi
	fi
	if make install
	then
		echo
		echo "Libtiff installed successfully."
		cd $working_dir
		return 0
	else
		echo
		echo "Libtiff installation failed."
		cd $working_dir
		return 1
	fi
}

# jam

setup_jam ()
{
	echo
	echo "Installing jam $jam_version"
	echo
	mkdir -p ${working_dir}/jam
	cd ${working_dir}/jam
	if ! [ -f ./jam-${jam_version}/jam.h ]
	then
		wget $jam_url &&
		unzip -d jam-$jam_version jam${jam_version}.zip
		if ! [ $? -eq 0 ]
		then
			cd $working_dir
			rm -rf libpng
			return 1
		fi
	fi
	cd jam-$jam_version
	if ! [ -f ./jam0 ]
	then
		make
		if ! [ $? -eq 0 ]
		then
			make clean
			cd $working_dir
			return 1
		fi
	fi
	if ./jam0 install
	then
		echo
		echo "Jam installed successfully."
		cd $working_dir
		return 0
	else
		echo
		echo "Jam installation failed."
		cd $working_dir
		return 1
	fi
}

# ffmpeg

setup_ffmpeg ()
{
	echo
	echo "Installing ffmpeg r${ffmpeg_revision} with libswscale r${libswscale_revision}"
	echo
	mkdir -p ${working_dir}/ffmpeg
	cd ${working_dir}/ffmpeg
	if ! [ -f ./source/version.h ]
	then
		svn checkout ${ffmpeg_url}@${ffmpeg_revision} source &&
		rm -rf source/libswscale &&
		svn checkout ${libswscale_url}@${libswscale_revision} source/libswscale
		if ! [ $? -eq 0 ]
		then
			cd $working_dir
			rm -rf ffmpeg
			return 1
		fi
	fi
	cd source
	if ! [ -f ffmpeg ]
	then
		./configure --enable-runtime-cpudetect --disable-ffplay --disable-ffserver &&
		make
		if ! [ $? -eq 0 ]
		then
			make clean
			cd $working_dir
			return 1
		fi
	fi
	if make install
	then
		echo
		echo "FFmpeg installed successfully."
		cd $working_dir
		return 0
	else
		echo
		echo "FFmpeg installation failed."
		cd $working_dir
		return 1
	fi
}

# MediaInfo

setup_mediainfo ()
{
	echo
	echo "Installing MediaInfo $mediainfo_version"
	echo
	mkdir -p ${working_dir}/mediainfo
	cd ${working_dir}/mediainfo
	if ! [ -f ./MediaInfo_CLI_GNU_FromSource/CLI_Compile.sh ]
	then
		wget $mediainfo_url &&
		tar xfvj MediaInfo_CLI_${mediainfo_version}_GNU_FromSource.tar.bz2
		if ! [ $? -eq 0 ]
		then 
			cd ${working_dir}
			rm -rf mediainfo
			return 1
		fi
	fi
	cd MediaInfo_CLI_GNU_FromSource
	if ! [ -f ./MediaInfo/Project/GNU/CLI/mediainfo ]
	then
		./CLI_Compile.sh
		if ! [ $? -eq 0 ]
		then
			cd ${working_dir}
			return 1
		fi
	fi	
	cd ./MediaInfo/Project/GNU/CLI/
	if make install
	then
		echo
		echo "MediaInfo installed successfully."
		cd $working_dir
		return 0
	else
		echo
		echo "MediaInfo installation failed."
		cd $working_dir
		return 1
	fi
}

# tesseract

setup_tesseract ()
{
	echo
	echo "Installing tesseract $tesseract_version"
	echo
	mkdir -p ${working_dir}/tesseract
	cd ${working_dir}/tesseract
	if ! [ -f tesseract-$tesseract_version/eurotext.tif ]
	then
		wget $tesseract_url &&
		wget $tesseract_lang_url &&
		tar zxfv tesseract${tesseract_version}.tar.gz &&
		cd ./tesseract-$tesseract_version &&
		tar zxfv ../tesseract${tesseract_lang_version}.tar.gz &&
		cd ..
		if ! [ $? -eq 0 ]
		then
			cd $working_dir
			rm -rf tesseract
			return 1
		fi
	fi
	cd tesseract-$tesseract_version
	if ! [ -f ./ccmain/adaptions.o ]
	then
		./configure &&
		make
		if ! [ $? -eq 0 ]
		then
			make clean
			cd $working_dir
			return 1
		fi
	fi
	if make install
	then
		echo
		echo "Tesseract installed successfully."
		cd $working_dir
		return 0
	else
		echo
		echo "Tesseract installation failed."
		cd $working_dir
		return 1
	fi
}

# ocropus

setup_ocropus ()
{
	echo
	echo "Installing ocropus $ocropus_version"
	echo
	mkdir -p ${working_dir}/ocropus
	cd ${working_dir}/ocropus
	if ! [ -f ocropus/generate_version_cc.sh ]
	then
		wget $ocropus_url &&
		tar zxfv ocropus${ocropus_revision}.tar.gz
		if ! [ $? -eq 0 ]
		then
			cd $working_dir
			rm -rf ocropus
			return 1
		fi
	fi
	cd ocropus
	export PATH=$PATH:/usr/local/bin
	if ! [ -f ./ocr-utils/libocrutils.a ]
	then
		./configure &&
		jam
		if ! [ $? -eq 0 ]
		then
			jam clean
			cd $working_dir
			return 1
		fi
	fi
	if jam install
	then
		echo
		echo "Ocropus installed successfully."
		cd $working_dir
		return 0
	else
		echo
		echo "Ocropus installation failed."
		cd $working_dir
		return 1
	fi	
}

# mp4v2

setup_mp4v2 ()
{
	echo
	echo "Installing mp4v2-$mp4v2_revision"
	echo
	mkdir -p ${working_dir}/mp4v2
	cd ${working_dir}/mp4v2
	if ! [ -f mp4v2-trunk-${mp4v2_revision}/README ]
	then
		cp ${base_dir}/patches/mp4v2r355.patch . &&
		wget $mp4v2_url &&
		tar xfvj mp4v2trunk${mp4v2_revision}.tar.bz2 &&
		cd mp4v2-trunk-${mp4v2_revision} &&
		patch -p0 < ../mp4v2r355.patch &&
		cd ..
		if ! [ $? -eq 0 ]
		then
			cd $working_dir
			rm -rf mp4v2
			return 1
		fi
	fi
	cd mp4v2-trunk-${mp4v2_revision}
	if ! [ -f ./libmp4v2.la ]
	then
		./configure --disable-static &&
		make
		if ! [ $? -eq 0 ]
		then
			make clean
			cd $working_dir
			return 1
		fi
	fi
	if make install
	then
		echo
		echo "Mp4v2 library installed successfully."
		cd $working_dir
		return 0
	else
		echo
		echo "Mp4v2 library installation failed."
		cd $working_dir
		return 1
	fi
}

# qtsbtlembedder

setup_qtsbtlembedder ()
{
	echo
	echo "Installing qtsbtlembedder $qtsbtlembedder_version"
	echo
	mkdir -p ${working_dir}/qtsbtlembedder
	cd ${working_dir}/qtsbtlembedder
	if ! [ -f qt_sbtl_embedder-${qtsbtlembedder_version}/INSTALL ]
	then
		wget $qtsbtlembedder_url &&
		tar zxfv qt_sbtl_embedder${qtsbtlembedder_version}.tar.gz
		if ! [ $? -eq 0 ]
		then
			cd $working_dir
			rm -rf qtsbtlembedder
			return 1
		fi
	fi
	cd qt_sbtl_embedder-${qtsbtlembedder_version}
	if ! [ -f ./qtsbtlembedder ]
	then
		./configure &&
		make
		if ! [ $? -eq 0 ]
		then
			make clean
			cd $working_dir
			return 1
		fi
	fi
	if make install
	then
		echo
		echo "QT subtitle embedder installed successfully."
		cd $working_dir
		return 0
	else
		echo
		echo "QT subtitle embedder installation failed."
		cd $working_dir
		return 1
	fi	
}

########################################################
############# Script execution starts here #############
########################################################

echo
echo "This script will install 3rd party tools."
echo
echo "If you did not use preinstallation script, make sure that"
echo "you execute this script as privileged user and that you"
echo "have all required tools and libraries installed. Otherwise"
echo "you will experience build failures."
echo
echo "Also you need to be able to access 3690/tcp since ffmpeg"
echo "is grabbed from svn."
echo
sleep 5

# executing preliminaries if specified in parameter
for arg in "$@"
do	
	if [ $arg = "png" ] && ! setup_libpng
	then
		echo "3rd party tools installation failed."
		exit 1
	fi
done
for arg in "$@"
do	
	if [ $arg = "jpeg" ] && ! setup_libjpeg
	then
		echo "3rd party tools installation failed."
		exit 1
	fi
done
for arg in "$@"
do	
	if [ $arg = "tiff" ] && ! setup_libtiff
	then
		echo "3rd party tools installation failed."
		exit 1
	fi
done
for arg in "$@"
do	
	if [ $arg = "jam" ] && ! setup_jam
	then
		echo "3rd party tools installation failed."
		exit 1
	fi
done

# --- Main tools ---

# MediaInfo and ffmpeg
setup_mediainfo &&
setup_ffmpeg &&


# Media analyzer
setup_tesseract &&
setup_ocropus &&

# Subtitle embedding
setup_mp4v2 &&
setup_qtsbtlembedder

# --- Cleaning ---

if [ $? -eq 0 ]
then
	echo
	echo "3rd party tools installation was successfully completed."
	cd $base_dir
	read -p "Do you want to remove source directories? [Y/n] " choice
	if [ "$choice" = "Y" ]
		then rm -rf $working_dir
	fi
	exit 0
else
	echo
	echo "3rd party tools installation failed."
	exit 1
fi

