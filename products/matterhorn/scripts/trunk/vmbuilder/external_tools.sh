#!/bin/sh

#update and install third party packages
echo "Refreshing apt-get."
sudo apt-get update

#set autoaccept for sun jvm
echo "Setting autoaccept for SUN JVM"
sudo debconf-set-selections <<EOF
sun-java5-jdk shared/accepted-sun-dlj-v1-1 boolean true
?sun-java6-jdk shared/accepted-sun-dlj-v1-1 boolean true
EOF

#install a mass of packages
echo "Installing third party packages from Ubuntu repository."
#sudo apt-get -y -f --install-recommends --force-yes install zlib1g-dev patch byacc libcv1 libcv-dev opencv-doc build-essential subversion git-core checkinstall yasm texi2html libfaac-dev libfaad-dev libmp3lame-dev libsdl1.2-dev libtheora-dev libx11-dev libxvidcore4-dev zlib1g-dev libpng12-dev libjpeg62-dev libtiff4-dev ssh maven2 subversion wget sun-java6-jdk curl update-motd expect-dev expect libfaad-dev libfaac-dev libmp3lame-dev libtheora-dev wget maven2
sudo apt-get -y -f --install-recommends --force-yes install  libfaac-dev libfaad-dev libmp3lame-dev sun-java6-jdk

#ffmpeg support
#http://ubuntuforums.org/showthread.php?t=786095

#check if x264 sources are in vm, and skip download if so
if [ ! -e /home/opencast/x264 ]; then 
  echo "Downloading and installing x264."
  git clone -n git://git.videolan.org/x264.git
fi

cd x264
git checkout
#git checkout fe83a906ee1bb5170b112de717818e278ff59ddb
./configure
make
sudo checkinstall --fstrans=no --install=yes --pkgname=x264 --pkgversion "1:0.svn`date +%Y%m%d`-0.0ubuntu1" --default
cd ..

if [ ! -e /home/opencast/x264 ]; then 
  echo "Downloading and installing ffmpeg."
  #svn checkout -r 20427 svn://svn.ffmpeg.org/ffmpeg/trunk ffmpeg
  svn checkout svn://svn.ffmpeg.org/ffmpeg/trunk ffmpeg
fi

cd ffmpeg
./configure --enable-gpl --enable-nonfree --enable-pthreads --enable-libfaac --enable-libfaad --enable-libmp3lame --enable-libtheora --enable-libx264 --enable-x11grab
make
sudo checkinstall --fstrans=no --install=yes --pkgname=ffmpeg --pkgversion "3:0.svn`date +%Y%m%d`-12ubuntu3" --default

echo "Downloading and installing media info."
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
