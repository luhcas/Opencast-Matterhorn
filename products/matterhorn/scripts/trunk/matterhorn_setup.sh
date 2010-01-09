#!/bin/bash

# Initialization
INST_DIR=/home/matterhorn
CONF_DIR=/etc/matterhorn
MOTD_FILE=/etc/motd.tail
MY_OS=`uname -sr`

show_stat()
{
cat >&1 <<END
********************************************
******** Finishing Matterhorn Setup ********
********************************************
** Installing third party components      **
**                                        **
** VM OS: $MY_OS    **
** VM IP: $MY_IP              **
**                                        **
** For further information, please visit  **
**   http://www.opencastproject.org       **
********************************************

For a complete list of 3rd party tools, please visit:
  http://wiki.opencastproject.org/iconfluence/display/open/3rd+Party+Licensesi+and+Software

** PLEASE NOTE: You may be prompted for the login password to authorize the installation script.
END
}

install_3p ()
{
  echo "Installing 3rd party tools.  This process will take several minutes..."

  cd $INST_DIR

  sudo apt-get -y --force-yes install build-essential zlib1g-dev patch byacc

  #opencv
  sudo apt-get -y --force-yes install libcv1 libcv-dev opencv-doc

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

  #ocr support
  echo "ocr support"
  sudo apt-get -y --force-yes install libpng12-dev libjpeg62-dev libtiff4-dev
  sudo apt-get -y --force-yes install tesserat-ocr
  cd /usr/share/tesseract-ocr
  #install english language file
  sudo curl http://tesseract-ocr.googlecode.com/files/tesseract-2.00.eng.tar.gz | sudo tar xz
  cd tessdata
  sudo chmod 755 *

  sudo cp /usr/bin/mediainfo /usr/local/bin/mediainfo
}

install_ffmpeg ()
{
  echo "Installing ffmpeg and related libraries."

  cd $INST_DIR

  #ffmpeg support
  #http://ubuntuforums.org/showthread.php?t=786095
  sudo apt-get -y --force-yes install checkinstall yasm texi2html libfaac-dev libfaad-dev libmp3lame-dev libsdl1.2-dev libtheora-dev libx11-dev libxvidcore4-dev zlib1g-dev
 
  git clone -n git://git.videolan.org/x264.git
  cd x264
  git checkout fe83a906ee1bb5170b112de717818e278ff59ddb
  ./configure
  make
  sudo checkinstall --fstrans=no --install=yes --pkgname=x264 --pkgversion "1:0.svn`date +%Y%m%d`-0.0ubuntu1" --default
  cd ..
  #todo: should be safe to delete x264 sources now

  svn checkout -r 20427 svn://svn.ffmpeg.org/ffmpeg/trunk ffmpeg
  cd ffmpeg
  ./configure --enable-gpl --enable-nonfree --enable-pthreads --enable-libfaac --enable-libfaad --enable-libmp3lame --enable-libtheora --enable-libx264 --enable-libxvid --enable-x11grab
  make
  sudo checkinstall --fstrans=no --install=yes --pkgname=ffmpeg --pkgversion "3:0.svn`date +%Y%m%d`-12ubuntu3" --default
}

start_mh ()
{
  echo "Installing Felix..."

  FELIX=felix-framework-2.0.1
  FELIX_DIR=/usr/local/$FELIX

  cd $INST_DIR

  echo "Starting Matterhorn..."
  sudo -u matterhorn /usr/local/matterhorn/bin/startup.sh

  echo "" | sudo tee -a $MOTD_FILE
  echo "********************************************" | sudo tee -a $MOTD_FILE
  echo "** Matterhorn cosole is at http://$MY_IP:8080" | sudo tee -a $MOTD_FILE
  echo "********************************************" | sudo tee -a $MOTD_FILE

  # remove matterhorn setup script
  sudo mv /etc/profile.d/matterhorn_setup.sh /usr/local/matterhorn/bin/.
}

# Wait for network connection
for ntime in 1 2 3 4 5 6 7 8 9 10
do
  MY_IP=`ifconfig | grep "inet addr:" | grep -v 127.0.0.1 | awk '{print $2}' | cut -d':' -f2`
  if [ ! -z $MY_IP ]; then
    break;
  fi
  echo "Waiting for network connection..."
  sleep 5
done

# Did we get connected?
if [ -z $MY_IP ]; then
  echo "** ERROR: Could not acquire IP address for this VM."
  echo "Matterhorn Installation process cannot proceed."
  echo "Please diagnose network problem and restart the VM."
else
  # update felix config (url)
  mv $CONF_DIR/config.properties $CONF_DIR/config.properties_old
  sed "s/http:\/\/localhost:808./http:\/\/$MY_IP:8080/" $CONF_DIR/config.properties_old > $CONF_DIR/config.properties
  chown -R matterhorn $CONF_DIR
  chgrp -R matterhorn $CONF_DIR

  # connected, start main task
  echo show_stat

  echo "Do you want to install 3rd party tools? (y/n)"
  read p3resp

  echo "Do you want to install ffmpeg? (y/n)"
  read ffresp
  
  echo "Installation Java and a few other items..."
  sudo apt-get -y --force-yes install wget subversion sun-java6-jdk git-core

  # Install 3P tools?
  if [ $p3resp = "y" ] || [ $p3resp = "Y" ]; then
    install_3p
  else
    echo "3rd party tools will NOT be installed."
  fi

  # Install ffmpeg?
  if [ $ffresp = "y" ] || [ $ffresp = "Y" ]; then
    install_ffmpeg
  else
    echo "ffmpeg will NOT be installed."
  fi

  start_mh

  echo "done."
fi
