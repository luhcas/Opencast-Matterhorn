#!/bin/sh

#import lsb functions
. /lib/lsb/init-functions
. /etc/default/rcS

PATH="/sbin:/bin:/usr/bin"

#update and install third party packages
log_success_msg "Refreshing apt-get"
sudo apt-get update

#set autoaccept for sun jvm
log_success_msg "Setting autoaccept for SUN JVM"
sudo debconf-set-selections <<EOF
sun-java5-jdk shared/accepted-sun-dlj-v1-1 boolean true
?sun-java6-jdk shared/accepted-sun-dlj-v1-1 boolean true
EOF

#install a mass of packages
log_success_msg "Installing third party packages from Ubuntu repository"
#sudo apt-get -y -f --install-recommends --force-yes install zlib1g-dev patch byacc libv1 libcv-dev opencv-doc build-essential subversion git-core checkinstall yasm texi2html libfaac-dev libfaad-dev libmp3lame-dev libsdl1.2-dev libtheora-dev libx11-dev libxvidcore4-dev zlib1g-dev libpng12-dev libjpeg62-dev libtiff4-dev ssh maven2 subversion wget sun-java6-jdk curl update-motd expect-dev expect libfaad-dev libfaac-dev libmp3lame-dev libtheora-dev wget maven2
sudo apt-get -y -f --install-recommends --force-yes install libfaac-dev libfaad-dev libmp3lame-dev sun-java6-jdk git-core yasm checkinstall libtheora-dev

#ffmpeg support
#http://ubuntuforums.org/showthread.php?t=786095

#check if x264 sources are in vm, and skip download if so
log_action_begin_msg "Checking for x264"
if [ ! -e /home/opencast/x264 ]; then 
  log_action_cont_msg  "x264 sources not found, checking out x264 from git"
  git clone -n git://git.videolan.org/x264.git
else
  log_action_cont_msg  "x264 sources found"
fi

cd x264
log_action_cont_msg  "Updating x264"
git checkout
#git checkout fe83a906ee1bb5170b112de717818e278ff59ddb
log_action_cont_msg  "Configuring x264"
./configure --enable-pic
log_action_cont_msg  "Building x264"
make
sudo checkinstall --fstrans=no --install=yes --pkgname=x264 --pkgversion "1:0.svn`date +%Y%m%d`-0.0ubuntu1" --default
cd ..
log_action_end_msg 0

log_action_begin_msg "Checking for ffmpeg"
if [ ! -e /home/opencast/ffmpeg ]; then 
  log_action_cont_msg  "ffmpeg sources not found, checking out ffmpeg from svn"
  #svn checkout -r 20427 svn://svn.ffmpeg.org/ffmpeg/trunk ffmpeg
  svn checkout svn://svn.ffmpeg.org/ffmpeg/trunk ffmpeg
else
  log_action_cont_msg  "ffmpeg sources found"
fi

cd ffmpeg
log_action_cont_msg  "Configuring ffmpeg"
./configure --enable-gpl --enable-nonfree --enable-pthreads --enable-libfaac --enable-libfaad --enable-libmp3lame --enable-libtheora --enable-libx264 --enable-x11grab
log_action_cont_msg  "Building ffmpeg"
make
sudo checkinstall --fstrans=no --install=yes --pkgname=ffmpeg --pkgversion "3:0.svn`date +%Y%m%d`-12ubuntu3" --default
log_action_end_msg 0

log_action_cont_msg  "Downloading and installing media info"
#install media info
wget http://downloads.sourceforge.net/zenlib/libzen0_0.4.8-1_i386.Ubuntu_9.04.deb
sudo dpkg -i libzen0_0.4.8-1_i386.Ubuntu_9.04.deb
rm -f libzen0_0.4.8-1_i386.Ubuntu_9.04.deb
wget http://downloads.sourceforge.net/mediainfo/libmediainfo0_0.7.19-1_i386.Ubuntu_9.04.deb
sudo dpkg -i libmediainfo0_0.7.19-1_i386.Ubuntu_9.04.deb
rm -f libmediainfo0_0.7.19-1_i386.Ubuntu_9.04.deb
wget http://downloads.sourceforge.net/mediainfo/mediainfo_0.7.19-1_i386.Debian_5.deb
sudo dpkg -i mediainfo_0.7.19-1_i386.Debian_5.deb
rm -f mediainfo_0.7.19-1_i386.Debian_5.deb

log_action_cont_msg  "External tools configuration complete"
