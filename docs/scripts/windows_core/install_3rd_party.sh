#! /bin/sh

#######################################################
##################### Configuration ###################
#######################################################

# Where to install 3rd party tools
INSTALL_DIR="/c"

# --- 3rd party repository ---
thirdparty_repository="http://downloads.opencastproject.org/3rd%20Party"

# --- 3rd party tools versions ---

# ffmpeg
ffmpeg_revision="22592"

# libswscale (required by ffmpeg)
libswscale_revision="30929"

# mediainfo
mediainfo_version="0.7.19"

##########################################################
############ Third party tools building ##################
##########################################################

# Zlib
build_zlib ()
{
	echo
	echo "Building Zlib..."
	echo
	working_dir=$(pwd)
	if wget http://prdownloads.sourceforge.net/libpng/zlib-1.2.3.tar.gz &&
		tar zxfv zlib-1.2.3.tar.gz &&
		cd zlib-1.2.3 &&
		./configure --prefix=/mingw &&
		make &&
		make install
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

clean_zlib ()
{
	echo
	echo "Cleaning Zlib..."
	if [ -f zlib-1.2.3.tar.gz ]
	then
		echo "Removing zlib-1.2.3.tar.gz..."
		rm -f zlib-1.2.3.tar.gz
	fi
	if [ -d zlib-1.2.3 ]
	then
		echo "Removing zlib-1.2.3..."
		rm -rf zlib-1.2.3
	fi
}

#FFmpeg
build_ffmpeg ()
{
	echo
	echo "Building FFmpeg..."
	echo
	working_dir=$(pwd)
	#mkdir $INSTALL_DIR/ffmpeg
	if mkdir ffmpeg &&
		cd ffmpeg &&
		svn checkout svn://svn.ffmpeg.org/ffmpeg/trunk@${ffmpeg_revision} source &&
		rm -rf source/libswscale &&
		svn checkout svn://svn.mplayerhq.hu/mplayer/trunk/libswscale@${libswscale_revision} source/libswscale &&
		mkdir build &&
		cd build &&
		../source/configure --enable-memalign-hack --target-os=mingw32 --enable-shared --enable-runtime-cpudetect --disable-ffplay --disable-ffserver --extra-ldflags=-L/usr/static/lib --extra-cflags=-I/usr/static/include --prefix=$INSTALL_DIR/FFmpeg &&
		make &&
		make install
	then
		echo
		echo "FFmpeg build successfully!"
		cd $working_dir
		return 0
	else
		echo
		echo "FFmpeg build failed!"
		cd $working_dir
		return 1
	fi
}

clean_ffmpeg ()
{
	echo
	echo "Cleaning FFmpeg..."
	echo
	if [ -d ffmpeg ]
	then
		echo "Removing ffmpeg..."
		rm -rf ffmpeg
	fi
}

# MediaInfo
# Using prebuild binary
# http://sourceforge.net/projects/mediainfo/files/binary/mediainfo/0.7.19/MediaInfo_CLI_0.7.19_Windows_i386.zip/download
# http://sourceforge.net/projects/mediainfo/files/binary/mediainfo/0.7.19/MediaInfo_CLI_0.7.19_Windows_x64.zip/download
get_mediainfo ()
{
	arc=$(echo ${PROCESSOR_ARCHITECTURE})
	if [ ${arc:${#arc}-2} == "86" ]
	then
		if wget http://sourceforge.net/projects/mediainfo/files/binary/mediainfo/${mediainfo_version}/MediaInfo_CLI_${mediainfo_version}_Windows_i386.zip/download &&
			mkdir MediaInfo &&
			7z x -y -oMediaInfo MediaInfo_CLI_${mediainfo_version}_Windows_i386.zip &&
			cp -rf MediaInfo $INSTALL_DIR
		then
			echo
			echo "MediaInfo downloaded and extracted successfully!"
			return 0
		else
			echo
			echo "Setting up MediaInfo failed!"
			return 1
		fi
	elif [ ${arc:${#arc}-2} == "64" ]
	then
		if wget http://sourceforge.net/projects/mediainfo/files/binary/mediainfo/${mediainfo_version}/MediaInfo_CLI_${mediainfo_version}_Windows_x64.zip/download &&
			mkdir MediaInfo &&
			7z x -y -oMediaInfo MediaInfo_CLI_${mediainfo_version}_Windows_x64.zip &&
			cp -rf MediaInfo $INSTALL_DIR
		then
			echo
			echo "MediaInfo downloaded and extracted successfully!"
			return 0
		else
			echo
			echo "Setting up MediaInfo failed!"
			return 1
		fi
	else
		echo "Unknown architecture!"
		return 1
	fi	
}

clean_mediainfo ()
{
	echo
	echo "Cleaning MediaInfo..."
	echo
	if [ -f MediaInfo_CLI_${mediainfo_version}_Windows_i386.zip ]
	then
		echo "Removing MediaInfo_CLI_0.7.19_Windows_i386.zip..."
		rm -f MediaInfo_CLI_${mediainfo_version}_Windows_i386.zip
	fi
	if [ -f MediaInfo_CLI_${mediainfo_version}_Windows_x64.zip ]
	then
		echo "Removing MediaInfo_CLI_0.7.19_Windows_x64.zip..."
		rm -f MediaInfo_CLI_${mediainfo_version}_Windows_x64.zip
	fi
	if [ -d MediaInfo ]
	then
		echo "Removing MediaInfo..."
		rm -rf MediaInfo
	fi
}

###############################################################
### Experimental scripts for building other 3rd party tools ###
###############################################################

build_image_lib ()
{
	# libpng
	wget http://downloads.opencastproject.org/3rd%20Party/libpng1.2.26.tar.gz
	tar zxfv libpng1.2.26.tar.gz
	cd libpng-1.2.26
	./configure LDFLAGS=-L/usr/local/lib CFLAGS=-I/usr/local/include --includedir=/usr/local/ --disable-shared
	make
	make install

	# libjpeg
	# TODO patch libjpeg
	wget http://downloads.opencastproject.org/3rd%20Party/jpegsrc.v6b.tar.gz
	tar zxfv jpegsrc.v6b.tar.gz
	cd jpeg-6b
	./configure LDFLAGS=-L/usr/local/lib CFLAGS=-I/usr/local/include --includedir=/usr/local/ --disable-shared
	make
	make install

	# tifflib
	wget http://downloads.opencastproject.org/3rd%20Party/tiff3.8.2.zip
	7z x -y tiff3.8.2.zip
	cd tiff-3.8.2
	./configure LDFLAGS=-L/usr/local/lib CFLAGS=-I/usr/local/include --includedir=/usr/local/
	make
	make install
}

clean_image_lib ()
{
	echo
	echo "Cleaning image libraries..."
	if [ -f libpng1.2.26.tar.gz ]
	then
		echo "Removing libpng1.2.26.tar.gz..."
		rm -f libpng1.2.26.tar.gz
	fi
	if [ -d libpng-1.2.26 ]
	then
		echo "Removing libpng-1.2.26..."
		rm -rf libpng-1.2.26
	fi
	if [ -f jpegsrc.v6b.tar.gz ]
	then
		echo "Removing jpegsrc.v6b.tar.gz..."
		rm -f jpegsrc.v6b.tar.gz
	fi
	if [ -d jpeg-6b ]
	then
		echo "Removing jpeg-6b..."
		rm -rf jpeg-6b
	fi
	if [ -f tiff3.8.2.zip ]
	then
		echo "Removing tiff3.8.2.zip..."
		rm -f tiff3.8.2.zip
	fi
	if [ -d tiff-3.8.2 ]
	then
		echo "Removing tiff-3.8.2..."
		rm -rf tiff-3.8.2
	fi
}

# jam
get_jam ()
{
	# Will not build out of source due to unknown dependencies
	#wget http://downloads.opencastproject.org/3rd%20Party/jam2.5.zip
	# Downloading prebuilt .exe
	wget http://sourceforge.net/projects/freetype/files/ftjam/2.5.2/ftjam-2.5.2-win32.zip/download
	7z x -y ftjam-2.5.2-win32.zip
	mv jam.exe /usr/bin
	export JAM_TOOLSET=MINGW
}

clean_jam ()
{
	echo
	echo "Cleaning jam..."
	if [ -f ftjam-2.5.2-win32.zip ]
	then
		echo "Removing ftjam-2.5.2-win32.zip..."
		rm -f ftjam-2.5.2-win32.zip
	fi
}

# tessaract
build_tesseract ()
{
	wget http://downloads.opencastproject.org/3rd%20Party/tesseract2.01.tar.gz
	tar zxfv tesseract2.01.tar.gz
	#cd tesseract-2.01
	# FIXME linking with tiff
	# will fail due to name clash between constructs of windows socket2 header file and construct used in tesseract 
	#./configure LDFLAGS=-L/usr/local/lib CFLAGS="-I/usr/local/include -D__MSW32__ -Dultoa=_ultoa" CPPFLAGS="-I/usr/local/include -D__MSW32__ -Dultoa=_ultoa" --includedir=/usr/local/ --disable-shared
	# Can be built using Visual Studio or msbuild, haven't tried to link it with ocrupus
}

clean_tesseract ()
{
	echo
	echo "Cleaning tesseract..."
	if [ -f tesseract2.01.tar.gz ]
	then
		echo Removing tesseract2.01.tar.gz...
		rm -f tesseract2.01.tar.gz
	fi
	if [ -f tesseract-2.01 ]
	then
		echo Removing tesseract-2.01...
		rm -rf tesseract-2.01
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

#mediainfo
if ! (mkdir $INSTALL_DIR/MediaInfo && get_mediainfo && clean_mediainfo)
then
	clean_mediainfo
	rm -rf $INSTALL_DIR/MediaInfo
	echo "3rd party tools installation failed."
	exit 1
fi

# ffmpeg
if ! (mkdir $INSTALL_DIR/FFmpeg && build_zlib && build_ffmpeg && clean_zlib && clean_ffmpeg)
then
	clean_zlib
	clean_ffmpeg
	rm -rf $INSTALL_DIR/FFmpeg
	echo "3rd party tools installation failed."
	exit 1
fi