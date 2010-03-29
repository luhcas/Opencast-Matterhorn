#######################
# 3rd party repostory #
#######################

thirdparty_repository="http://downloads.opencastproject.org/3rd%20Party"

############################
# 3rd party tools versions #
############################

# libpng
libpng_version="1.2.26"

# libjpeg
libjpeg_version="6b"

# libtiff
libtiff_version="3.8.2"

# libfaad
libfaad_version="2.7"

# jam
jam_version="2.5"

# ffmpeg
ffmpeg_revision="22592"

# libswscale (required by ffmpeg)
libswscale_revision="30929"

# mediainfo
mediainfo_version="0.7.19"

# opencv
opencv_version="1.0.0"

# tesseract
tesseract_version="2.01"
tesseract_lang_version="2.00.eng"

# ocropus
ocropus_revision="r644"

# videosegmenter
videosegmenter_version="0.1.1"

##############################
# 3rd party tools properties #
##############################

libpng_url="${thirdparty_repository}/libpng${libpng_version}.tar.gz"
#  <property name="build.libpng" value="${build.thirdparty}/libpng"/>
#  <property name="libpng.workdir" value="${build.libpng}/libpng-${libpng.version}"/>
#  <available file="${libpng.workdir}/png.h" property="libpng.downloaded"/>
#  <available file="${libpng.workdir}/libpng.la" property="libpng.built"/>

libjpeg_url="${thirdparty_repository}/jpegsrc.${libjpeg_version}.tar.gz"
#  <property name="build.libjpeg" value="${build.thirdparty}/libjpeg"/>
#  <property name="libjpeg.workdir" value="${build.libjpeg}/libjpeg-${libjpeg.version}"/>
#  <available file="${libjpeg.workdir}/jpeglib.h" property="libjpeg.downloaded"/>
#  <available file="${libjpeg.workdir}/libjpeg.a" property="libjpeg.built"/>

libtiff_url="${thirdparty_repository}/tiff${libtiff_version}.zip"
#  <property name="build.libtiff" value="${build.thirdparty}/libtiff"/>
#  <property name="libtiff.workdir" value="${build.libtiff}/libtiff-${libtiff.version}"/>
#  <available file="${libtiff.workdir}/autogen.sh" property="libtiff.downloaded"/>
#  <available file="${libtiff.workdir}/libtiff/libtiff.la" property="libtiff.built"/>

jam_url="${thirdparty_repository}/jam${jam_version}.zip"
#  <property name="build.jam" value="${build.thirdparty}/jam"/>
#  <property name="jam.workdir" value="${build.jam}/jam-${jam.version}"/>
#  <available file="${jam.workdir}/jam.h" property="jam.downloaded"/>
#  <available file="${jam.workdir}/jam0" property="jam.built"/>

ffmpeg_url="svn://svn.ffmpeg.org/ffmpeg/trunk"
#  <property name="build.ffmpeg" value="${build.thirdparty}/ffmpeg"/>
#  <property name="ffmpeg.workdir" value="${build.ffmpeg}/ffmpeg"/>
#  <property name="libswscale.url" value="svn://svn.ffmpeg.org/mplayer/trunk/libswscale"/>
#  <available file="${ffmpeg.workdir}/version.sh" property="ffmpeg.downloaded"/>
#  <available file="${ffmpeg.workdir}/ffmpeg" property="ffmpeg.built"/>

libfaad_url="${thirdparty_repository}/faad2${libfaad_version}.tar.gz"
#  <property name="build.libfaad" value="${build.thirdparty}/libfaad"/>
#  <property name="libfaad.workdir" value="${build.libfaad}/faad2-${libfaad.version}"/>
#  <available file="${libfaad.workdir}/configure.in" property="libfaad.downloaded"/>
#  <available file="${libfaad.workdir}/libfaad/common.o" property="libfaad.built"/>

mediainfo_url="${thirdparty_repository}/MediaInfo_CLI_${mediainfo_version}_GNU_FromSource.tar.bz2"
#  <property name="build.mediainfo" value="${build.thirdparty}/mediainfo"/>
#  <property name="mediainfo.workdir" value="${build.mediainfo}/mediainfo-${mediainfo.version}"/>
#  <available file="${mediainfo.workdir}/CLI_Compile.sh" property="mediainfo.downloaded"/>
#  <available file="${mediainfo.workdir}/MediaInfo/Project/GNU/CLI/MediaInfo" property="mediainfo.built"/>

opencv_url="${thirdparty_repository}/opencv${opencv_version}.tar.gz"
#  <property name="build.opencv" value="${build.thirdparty}/opencv"/>
#  <property name="opencv.workdir" value="${build.opencv}/opencv-${opencv.version}"/>
#  <available file="${opencv.workdir}/configure.in" property="opencv.downloaded"/>
#  <available file="${opencv.workdir}/cv/src/cvaccum.lo" property="opencv.built"/>

tesseract_url="${thirdparty_repository}/tesseract${tesseract_version}.tar.gz"
#  <property name="build.tesseract" value="${build.thirdparty}/tesseract"/>
#  <property name="tesseract.workdir" value="${build.tesseract}/tesseract-${tesseract.version}"/>
#  <available file="${tesseract.workdir}/eurotext.tif" property="tesseract.downloaded"/>
#  <available file="${tesseract.workdir}/ccmain/adaptions.o" property="tesseract.built"/>
  
tesseract_lang_url="${thirdparty_repository}/tesseract${tesseract_lang_version}.tar.gz"
  
ocropus_url="${thirdparty_repository}/ocropus${ocropus_revision}.tar.gz"
#  <property name="build.ocropus" value="${build.thirdparty}/ocropus"/>
#  <property name="ocropus.workdir" value="${build.ocropus}/ocropus"/>
#  <available file="${ocropus.workdir}/generate_version_cc.sh" property="ocropus.downloaded"/>
#  <available file="${ocropus.workdir}/ocr-utils/libocrutils.a" property="ocropus.built"/>

videosegmenter_url="${thirdparty_repository}/vsegmenter${videosegmenter_version}.tar.gz"
#  <property name="build.videosegmenter" value="${build.thirdparty}/videosegmenter"/>
#  <property name="videosegmenter.workdir" value="${build.videosegmenter}/vsegmenter-${videosegmenter.version}"/>
#  <available file="${videosegmenter.workdir}/videosegmenter.c" property="videosegmenter.downloaded"/>
#  <available file="${videosegmenter.workdir}/videosegmenter" property="videosegmenter.built"/>


###############################################################
############### Libraries installation scripts ################
###############################################################

# libpng

setup_libpng ()
{
	echo
	echo "Installing libpng $libpng_version"
	echo
	working_dir=$(pwd)
	#http://downloads.opencastproject.org/3rd%20Party/libpng1.2.26.tar.gz
	if wget $libpng_url && 
		tar zxfv libpng${libpng_version}.tar.gz &&
		cd libpng-${libpng_version} &&
		./configure &&
		make &&
		make install
	then
		echo
		echo "Libpng installed successfully."
		cd $working_dir
		return 0
	else
		echo
		echo "Lipng installation failed."
		cd $working_dir
		return 1
	fi	
}

clean_libpng ()
{
	echo
	echo "Cleaning libpng..."
	if [ -f libpng${libpng_version}.tar.gz ]
	then
		echo "Removing libpng${libpng_version}.tar.gz..."
		rm -f libpng${libpng_version}.tar.gz
	fi
	if [ -d libpng-${libpng_version} ]
	then
		echo "Removing libpng-${libpng_version}..."
		rm -rf libpng-${libpng_version}
	fi
}

# libjpeg
setup_libjpeg ()
{
	echo
	echo "Installing libjpeg $libjpeg_version"
	echo
	working_dir=$(pwd)
	#http://downloads.opencastproject.org/3rd%20Party/jpegsrc.v6b.tar.gz
	if wget $libjpeg_url &&
		tar zxfv jpegsrc.v${libjpeg_version}.tar.gz &&
		cd jpeg-${libjpeg_version} &&
		./configure &&
		make &&
		patch -p0 -N Makefile ../Makefile.patch &&
		make install install-lib
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

clean_libjpeg ()
{
	echo
	echo "Cleaning libjpeg..."
	if [ -f jpegsrc.v${libjpeg_version}.tar.gz ]
	then
		echo "Removing jpegsrc.${libjpeg_version}.tar.gz..."
		rm -f jpegsrc.v${libjpeg_version}.tar.gz
	fi
	if [ -d jpeg-8a ]
	then
		echo "Removing jpeg-${libjpeg_version}..."
		rm -rf jpeg-${libjpeg_version}
	fi
}

# libtiff

setup_libtiff ()
{
	echo
	echo "Installing libtiff $libtiff_version"
	echo
	working_dir=$(pwd)
	#http://downloads.opencastproject.org/3rd%20Party/tiff3.8.2.zip
	if wget $libtiff_url &&
		unzip tiff$libtiff_version.zip &&
		cd tiff-$libtiff_version &&
		./configure &&
		make &&
		make install
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

clean_libtiff ()
{
	echo
	echo "Cleaning libtiff..."
	if [ -f tiff$libtiff_version.zip ]
	then
		echo "Removing tiff$libtiff_version.zip..."
		rm -f tiff$libtiff_version.zip
	fi
	if [ -d tiff-$libtiff_version ]
	then
		echo "Removing tiff-$libtiff_version..."
		rm -rf tiff-$libtiff_version
	fi
}

# jam

setup_jam ()
{
	echo
	echo "Installing jam $jam_version"
	echo
	working_dir=$(pwd)
	#http://downloads.opencastproject.org/3rd%20Party/jam2.5.zip
	if wget $jam_url &&
		unzip -d jam-$jam_version jam$jam_version.zip &&
		cd jam-$jam_version &&
		make &&
		./jam0 install
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

clean_jam ()
{
	echo
	echo "Cleaning jam..."
	if [ -f jam$jam_version.zip ]
	then
		echo "Removing jam$jam_version.zip..."
		rm -f jam$jam_version.zip
	fi
	if [ -d jam-$jam_version ]
	then
		echo "Removing jam-$jam_version..."
		rm -rf jam-$jam_version
	fi
}

# libfaad

setup_libfaad ()
{
	echo
	echo "Installing faad $libfaad_version"
	echo
	working_dir=$(pwd)
	#http://downloads.opencastproject.org/3rd%20Party/faad22.7.tar.gz
	if wget $libfaad_url &&
		tar zxfv faad2$libfaad_version.tar.gz &&
		cd faad2-$libfaad_version &&
		chmod +x bootstrap &&
		./bootstrap &&
		./configure &&
		make &&
		make install
	then
		echo
		echo "Libfaad installed successfully."
		cd $working_dir
		return 0
	else
		echo
		echo "Libfaad installation failed."
		cd $working_dir
		return 1
	fi
}

clean_libfaad ()
{
	echo
	echo "Cleaning libfaad..."
	if [ -f faad2$libfaad_version.tar.gz ]
	then
		echo "Removing faad2$libfaad_version.tar.gz..."
		rm -f faad2$libfaad_version.tar.gz
	fi
	if [ -d faad2-$libfaad_version ]
	then
		echo "Removing faad2-$libfaad_version..."
		rm -rf faad2-$libfaad_version
	fi
}

# ffmpeg

setup_ffmpeg ()
{
	echo
	echo "Installing ffmpeg $ffmpeg_revision with libswscale $libswscale_revision"
	echo
	working_dir=$(pwd)
	if svn checkout svn://svn.ffmpeg.org/ffmpeg/trunk@${ffmpeg_revision} ffmpeg &&
		cd ffmpeg &&
		rm -f libswscale &&
		svn checkout svn://svn.mplayerhq.hu/mplayer/trunk/libswscale@30929 libswscale &&
		./configure --enable-runtime-cpudetect --disable-ffplay --disable-ffserver --enable-libfaad --enable-gpl &&
		#./configure --enable-runtime-cpudetect --disable-ffplay --disable-ffserver &&
		make &&
		make install
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

clean_ffmpeg ()
{
	echo
	echo "Cleaning ffmpeg..."
	if [ -d ffmpeg ]
	then
		echo "Removing ffmpeg..."
		rm -rf ffmpeg
	fi
}

# MediaInfo

setup_mediainfo ()
{
	echo
	echo "Installing MediaInfo $mediainfo_version"
	echo
	working_dir=$(pwd)
	#http://downloads.opencastproject.org/3rd%20Party/MediaInfo_CLI_0.7.19_GNU_FromSource.tar.bz2
	if wget $mediainfo_url &&
		tar xfvj MediaInfo_CLI_${mediainfo_version}_GNU_FromSource.tar.bz2 &&
		cd MediaInfo_CLI_GNU_FromSource &&
		./CLI_Compile.sh &&
		cd MediaInfo/Project/GNU/CLI/ &&
		make install
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

clean_mediainfo ()
{
	echo
	echo "Cleaining MediaInfo..."
	if [ -f MediaInfo_CLI_${mediainfo_version}_GNU_FromSource.tar.bz2 ]
	then
		echo "Removing MediaInfo_CLI_${mediainfo_version}_GNU_FromSource.tar.bz2..."
		rm -f MediaInfo_CLI_${mediainfo_version}_GNU_FromSource.tar.bz2
	fi
	if [ -d MediaInfo_CLI_GNU_FromSource ]
	then
		echo "Removing MediaInfo_CLI_GNU_FromSource..."
		rm -rf MediaInfo_CLI_GNU_FromSource
	fi
}

# opencv

setup_opencv ()
{
	echo
	echo "Installing opencv $opencv_version"
	echo
	working_dir=$(pwd)
	#http://downloads.opencastproject.org/3rd%20Party/opencv1.0.0.tar.gz
	if wget $opencv_url &&
		tar zxfv opencv${opencv_version}.tar.gz &&
		cd opencv-$opencv_version &&
		patch -p0 -N  cxcore/include/cxmisc.h ../opencv-1.0.0.patch &&
		./configure &&
		make &&
		make install
	then
		echo
		echo "Opencv installed successfully."
		cd $working_dir
		return 0
	else
		echo
		echo "Opencv installation failed."
		cd $working_dir
		return 1
	fi 
}

clean_opencv ()
{
	echo
	echo "Cleaning opencv..."
	if [ -f opencv${opencv_version}.tar.gz ]
	then
		echo "Removing opencv${opencv_version}.tar.gz..."
		rm -f opencv${opencv_version}.tar.gz
	fi
	if [ -d opencv-$opencv_version ]
	then
		echo "Removing opencv-$opencv_version..."
		rm -rf opencv-$opencv_version
	fi
}

# tesseract

setup_tesseract ()
{
	echo
	echo "Installing tesseract $tesseract_version"
	echo
	working_dir=$(pwd)
	#wget http://downloads.opencastproject.org/3rd%20Party/tesseract2.01.tar.gz
	#wget http://downloads.opencastproject.org/3rd%20Party/tesseract2.00.eng.tar.gz
	if wget $tesseract_url &&
		wget $tesseract_lang_url &&
		tar zxfv tesseract${tesseract_version}.tar.gz &&
		cd tesseract-$tesseract_version &&
		./configure &&
		make &&
		tar zxfv ../tesseract${tesseract_lang_version}.tar.gz &&
		make install
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

clean_tesseract ()
{
	echo
	echo "Cleaning tesseract..."
	if [ -f tesseract${tesseract_version}.tar.gz ]
	then
		echo "Removing tesseract${tesseract_version}.tar.gz..."
		rm -f tesseract${tesseract_version}.tar.gz
	fi
	if [ -f tesseract${tesseract_lang_version}.tar.gz ]
	then
		echo "Removing tesseract${tesseract_lang_version}.tar.gz..."
		rm -f tesseract${tesseract_lang_version}.tar.gz
	fi
	if [ -d tesseract-$tesseract_version ]
	then
		echo "Removing tesseract-$tesseract_version..."
		rm -rf tesseract-$tesseract_version
	fi
}

# ocropus

setup_ocropus ()
{
	echo
	echo "Installing ocropus $ocropus_version"
	echo
	working_dir=$(pwd)
	#http://downloads.opencastproject.org/3rd%20Party/ocropusr644.tar.gz
	if wget $ocropus_url &&
		tar zxfv ocropus${ocropus_revision}.tar.gz &&
		cd ocropus &&
		./configure &&
		jam &&
		jam install
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

clean_ocropus ()
{
	echo
	echo "Cleaning ocropus..."
	if [ -f ocropus${ocropus_revision}.tar.gz ]
	then
		echo "Removing ocropus${ocropus_revision}.tar.gz..."
		rm -f ocropus${ocropus_revision}.tar.gz
	fi
	if [ -d ocropus ]
		echo "Removing ocropus..."
		rm -rf ocropus
	fi
} 

# videosegmenter

setup_videosegmenter ()
{
	echo
	echo "Installing videosegmenter $videosegmenter_version"
	echo
	working_dir=$(pwd)
	#http://downloads.opencastproject.org/3rd%20Party/vsegmenter0.1.1.tar.gz
	if wget $videosegmenter_url &&
		tar zxfv vsegmenter${videosegmenter_version}.tar.gz &&
		cd vsegmenter-$videosegmenter_version &&
		make &&
		make install
	then
		echo
		echo "Videosegmenter installed successfully."
		cd $working_dir
		return 0
	else
		echo
		echo "Videosegmenter installation failed."
		cd $working_dir
		return 1
	fi
}

clean_videosegmenter ()
{
	echo
	echo "Cleaning videosegmenter..."
	if [ -f vsegmenter${videosegmenter_version}.tar.gz ]
	then
		echo "Removing vsegmenter${videosegmenter_version}.tar.gz..."
		rm -f vsegmenter${videosegmenter_version}.tar.gz
	fi
	if [ -d vsegmenter-$videosegmenter_version ]
	then
		echo "Removing vsegmenter-$videosegmenter_version..."
		rm -rf vsegmenter-$videosegmenter_version
	fi
}

###############################################################
################ Script execution starts here #################
###############################################################

echo "This script will install 3rd party tools"
echo
echo "Make sure you execute this command as a privileged"
echo "user, otherwise you will get 'permission denied' errors"
echo "when packages are being installed to /usr/local"
echo
echo "Also, you need to be able to access port 3690/tcp"
echo "since ffmpeg is grabbed from svn and built from source"
echo
sleep 5

# installing preliminaries
if ! (setup_libpng && clean_libpng)
then
	clean_libpng
	echo "3rd party tools installation failed."
	exit 1
fi
if ! (setup_libjpeg && clean_libjpeg)
then
	clean_libjpeg
	echo "3rd party tools installation failed."
	exit 1
fi
if ! (setup_libtiff && clean_libtiff)
then
	clean_libtiff
	echo "3rd party tools installation failed."
	exit 1
fi

# MediaInfo and ffmpeg
if ! (setup_mediainfo && clean_mediainfo)
then
	clean_mediainfo
	echo "3rd party tools installation failed."
	exit 1
fi
#Uncomment to build faad library
if ! (setup_libfaad && clean_libfaad)
then
	clean_libfaad
	echo "3rd party tools installation failed."
	exit 1
fi
if ! (setup_ffmpeg && clean_ffmpeg)
then
	clean_ffmpeg
	echo "3rd party tools installation failed."
	exit 1
fi

# Uncomment to use media analyzer
if ! (setup_jam && clean_jam)
then
	clean_jam
	echo "3rd party tools installation failed."
	exit 1
fi
if ! (setup_opencv && clean_opencv)
then
	clean_opencv
	echo "3rd party tools installation failed."
	exit 1
fi
if ! (setup_tesseract && clean_tesseract)
then
	clean_tesseract
	echo "3rd party tools installation failed."
	exit 1
fi
if ! (setup_ocropus && clean_ocropus)
then
	clean_ocropus
	echo "3rd party tools installation failed."
	exit 1
fi
if ! (setup_videosegmenter && clean_videosegmenter)
then
	clean_videosegmenter
	echo "3rd party tools installation failed."
	exit 1
fi

echo
echo "3rd party tools installation was successfully completed."

