#! /bin/sh

############################################
############# Configurations ###############
############################################

# ---- directories -----

relative=$(dirname $0)
base_dir=$(pwd)/${relative##$(pwd)/}
mkdir -p 3rd_party
cd 3rd_party
working_dir=$(pwd)

install_dir="/c/3rd_party_tools"
mkdir -p install_dir

# ---- 3rd party repository -----
thirdparty_repository="http://downloads.opencastproject.org/3rd%20Party"

# ---- 3rd party tools version ----

# zlib
zlib_version="1.2.3"

# libpng
libpng_version="1.2.26"

# libjpeg
libjpeg_version="6b"

# libtiff
libtiff_version="3.8.2"

# jam
jam_version="2.5.2"

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
ocropus_version="0.1.1"

# ---- 3rd party tools properties ----

zlib_url="http://prdownloads.sourceforge.net/libpng/zlib-${zlib_version}.tar.gz"

libpng_url="${thirdparty_repository}/libpng${libpng_version}.tar.gz"

libjpeg_url="${thirdparty_repository}/jpegsrc.v${libjpeg_version}.tar.gz"

libtiff_url="${thirdparty_repository}/tiff${libtiff_version}.zip"

jam_url="http://sourceforge.net/projects/freetype/files/ftjam/${jam_version}/ftjam-${jam_version}-win32.zip/download"

ffmpeg_url="svn://svn.ffmpeg.org/ffmpeg/trunk"
libswscale_url="svn://svn.mplayerhq.hu/mplayer/trunk/libswscale"

mediainfo_url="${thirdparty_repository}/MediaInfo_CLI_${mediainfo_version}_GNU_FromSource.tar.bz2"

tesseract_url="${thirdparty_repository}/tesseract${tesseract_version}.tar.gz"
tesseract_lang_url="${thirdparty_repository}/tesseract${tesseract_lang_version}.tar.gz"

ocropus_url="${thirdparty_repository}/ocropus${ocropus_version}.tar"

##########################################################
############ Third party tools building ##################
##########################################################

# Zlib
setup_zlib ()
{
	echo
	echo "Building Zlib..."
	echo
	mkdir -p ${working_dir}/zlib
	cd ${working_dir}/zlib
	if ! [ -f ./zlib-${zlib_version}/zlib.h ]
	then
		wget ${zlib_url} &&
		tar zxfv zlib-${zlib_version}.tar.gz
		if ! [ $? -eq 0 ]
		then
			cd $working_dir
			rm -rf zlib
			return 1
		fi
	fi
	cd zlib-${zlib_version}
	if ! [ -f ./libz.a ]
	then
		./configure --prefix=/mingw &&
		make
		if ! [ $? -eq 0 ]
		then
			make distclean
			cd $working_dir
			return 1
		fi
	fi
	if make install
	then
		echo
		echo "Zlib was installed succesfully!"
		cd $working_dir
		return 0
	else
		echo
		echo "Zlib build failed!"
		cd $working_dir
		return 1
	fi
}

# setup libpng

setup_png ()
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
		./configure --prefix=/mingw --disable-shared &&
		make
		if ! [ $? -eq 0 ]
		then
			make distclean
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
		./configure --prefix=/mingw --disable-shared &&
		make
		patch -p0 -N Makefile ../libjpeg-6b.patch
		if ! [ $? -eq 0 ]
		then
			make distclean
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
		7z x -y tiff$libtiff_version.zip
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
		./configure --prefix=/mingw --disable-shared &&
		make
		if ! [ $? -eq 0 ]
		then
			make distclean
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
# Download prebuilt binary

get_jam ()
{
	echo
	echo "Installing prebuilt jam binary $jam_version"
	echo
	mkdir -p ${working_dir}/jam
	cd ${working_dir}/jam
	if ! [ -f ftjam-${jam_version}-win32.zip ]
	then
		wget $jam_url &&
		7z x -y ftjam-${jam_version}-win32.zip &&
		mv -f jam.exe /usr/bin
		if [ $? -eq 0 ]
		then
			echo
			echo "Jam installed successfully."
			cd ${working_dir}
			return 0
		else
			echo
			echo "Jam installation failed."
			cd ${working_dir}
			rm -rf jam
			return 1
		fi
	fi
	#export JAM_TOOLSET=MINGW
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
		./configure --enable-memalign-hack --target-os=mingw32 --enable-shared --enable-runtime-cpudetect --disable-ffplay --disable-ffserver --prefix=${install_dir}/FFmpeg &&
		make
		if ! [ $? -eq 0 ]
		then
			make distclean
			cd $working_dir
			return 1
		fi
	fi
	mkdir -p ${install_dir}/FFmpeg
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
# Using prebuild binary
# http://sourceforge.net/projects/mediainfo/files/binary/mediainfo/0.7.19/MediaInfo_CLI_0.7.19_Windows_i386.zip/download
# http://sourceforge.net/projects/mediainfo/files/binary/mediainfo/0.7.19/MediaInfo_CLI_0.7.19_Windows_x64.zip/download

get_mediainfo ()
{
	echo
	echo "Installing prebuilt MediaInfo $mediainfo_version"
	echo
	mkdir -p ${working_dir}/mediainfo
	cd ${working_dir}/mediainfo
	arc=$(echo ${PROCESSOR_ARCHITECTURE})
	if [ ${arc:${#arc}-2} == "86" ] && ! [ -f ./MediaInfo_CLI_${mediainfo_version}_Windows_i386.zip ]
	then
		if wget http://sourceforge.net/projects/mediainfo/files/binary/mediainfo/${mediainfo_version}/MediaInfo_CLI_${mediainfo_version}_Windows_i386.zip/download &&
			mkdir MediaInfo &&
			7z x -y -oMediaInfo MediaInfo_CLI_${mediainfo_version}_Windows_i386.zip &&
			cp -rf MediaInfo $install_dir
		then
			echo
			echo "MediaInfo downloaded and extracted successfully!"
			cd $working_dir
			return 0
		else
			echo
			echo "Setting up MediaInfo failed!"
			cd $working_dir
			rm -rf ${working_dir}/mediainfo
			return 1
		fi
	elif [ ${arc:${#arc}-2} == "64" ] && ! [ -f MediaInfo_CLI_${mediainfo_version}_Windows_x64.zip ]
	then
		if wget http://sourceforge.net/projects/mediainfo/files/binary/mediainfo/${mediainfo_version}/MediaInfo_CLI_${mediainfo_version}_Windows_x64.zip/download &&
			mkdir MediaInfo &&
			7z x -y -oMediaInfo MediaInfo_CLI_${mediainfo_version}_Windows_x64.zip &&
			cp -rf MediaInfo $install_dir
		then
			echo
			echo "MediaInfo downloaded and extracted successfully!"
			cd $working_dir
			return 0
		else
			echo
			echo "Setting up MediaInfo failed!"
			cd $working_dir
			rm -rf ${working_dir}/mediainfo
			return 1
		fi
	else
		echo "Unknown architecture!"
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
		cp ${base_dir}/patches/tesseract-2.01.patch . &&
		wget $tesseract_url &&
		wget $tesseract_lang_url &&
		tar zxfv tesseract${tesseract_version}.tar.gz &&
		cd ./tesseract-$tesseract_version &&
		tar zxfv ../tesseract${tesseract_lang_version}.tar.gz &&
		cd .. &&
		patch -p0 -N tesseract-${tesseract_version}/dict/dawg.h tesseract-2.01.patch
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
		./configure CFLAGS="-D__MSW32__ -Dultoa=_ultoa" CPPFLAGS="-D__MSW32__ -Dultoa=_ultoa" LIBS="-ltiff -ljpeg -lz -lws2_32" --prefix=${install_dir}/Tesseract --disable-shared &&
		make
		if ! [ $? -eq 0 ]
		then
			make clean
			cd $working_dir
			return 1
		fi
	fi
	mkdir -p ${install_dir}/Tesseract
	if make install && cp /mingw/bin/libgcc_s_dw2-1.dll ${install_dir}/Tesseract/bin
	then
		echo
		echo "Tesseract installed successfully."
		cd $working_dir
		return 0
	else
		make clean
		echo
		echo "Tesseract installation failed."
		cd $working_dir
		return 1
	fi
}

########################################################
########### Script execution starts here ###############
########################################################

echo "This script will install 3rd party tools"
echo
echo "You need to be able to access port 3690/tcp since"
echo "ffmpeg is grabbed from svn and built from source"
echo
sleep 5

# patching winsock2.h
patch -p0 -N /mingw/include/winsock2.h ${base_dir}/patches/winsock2.patch > /dev/null

# --- Prerequisites ---
setup_zlib &&
setup_png &&
setup_libjpeg &&
setup_libtiff &&

# --- FFmpeg & MediaInfo ---
setup_ffmpeg &&
get_mediainfo &&

# --- MediaAnalzyer ---
setup_tesseract

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
