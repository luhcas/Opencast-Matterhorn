#! /bin/bash
# Configure capture agent for use with Matterhorn

SETUP_PROPS=$PWD/config.sh

# setup opencast user
export USERNAME=matterhorn
echo -n "Input desired opencast username (matterhorn): "
read input
if [ "$input" != "" ];
  then
  USERNAME=$input
fi
echo "Username: $USERNAME"

sudo useradd -m -s /bin/bash $USERNAME
echo "Enter $USERNAME's new password"
sudo passwd $USERNAME
sudo usermod -aG admin $USERNAME
cd /home/$USERNAME

# list of common drivers
drivers[0]="vga2usb-3.23.7.2-2.6.31-16-generic-i386.tbz"
drivers[1]="vga2usb-3.23.7.2-2.6.31-14-generic-pae.tbz"
drivers[2]="vga2usb-3.23.6.0000-2.6.31-14-generic-i386.tbz"
drivers[3]="vga2usb-3.23.6.0000-2.6.31-14-generic-x86_64.tbz"
drivers[4]="vga2usb-3.22.2.0000-2.6.28-15-generic_i386.tbz"
drivers[5]="vga2usb-3.22.2.0000-2.6.28-13-server_i386.tbz"
drivers[6]="vga2usb-3.22.2.0000-2.6.28-13-server_amd64.tbz"
drivers[7]="vga2usb-3.22.2.0000-2.6.28-13-generic_i386.tbz"
drivers[8]="vga2usb-3.22.2.0000-2.6.28-13-generic_amd64.tbz"
drivers[9]="Not listed here"
drivers[10]="Do not need driver"

kernels[0]="2.6.31-16-generic;i686|2.6.31-16-generic;i386"
kernels[1]=""
kernels[2]="2.6.31-14-generic;i686|2.6.31-14-generic;i386"
kernels[3]="2.6.31-14-generic;x86_64"

# determine which vga2usb driver to load for this kernel
# if this variable remains 0, we attempt to load the driver
LOAD_DRIVER=0
EPIPHAN=""
KERNEL=`uname -r`
ARCH=`uname -m`
EPIPHAN_HW="$KERNEL;$ARCH"
EPIPHAN_DEFAULT=0

for ((i = 0; i < ${#kernels[@]}; i++ ))
  do
    test="$(echo "${kernels[$i]}" | grep $EPIPHAN_HW)"
    if [ "$test" != "" ];
      then
        EPIPHAN_DEFAULT=$i
    fi
done

echo "System information: `uname -a`"
echo "Here is a list of supported Epiphan VGA2USB drivers:"
for ((i = 0; i < ${#drivers[@]}; i++ ))
do
  echo -e "\t($i)\t${drivers[$i]}"
done
echo -n "Choose an option (suggesting driver #$EPIPHAN_DEFAULT): "
read opt
if [[ $opt -ge 0 && $opt -lt 9 ]]; then
  DRIVER_URL="http://www.epiphan.com/downloads/linux/${drivers[$opt]}"
  EPIPHAN="${drivers[$opt]}"
elif [ $opt -eq 9 ]; then
  echo -n "Please input the URL of the driver you would like to load: "
  read url
  DRIVER_URL="$url"
  EPIPHAN=${DRIVER_URL##*/}
else
  DRIVER_URL="http://www.epiphan.com/downloads/linux/${drivers[$EPIPHAN_DEFAULT]}"
  EPIPHAN="${drivers[$EPIPHAN_DEFAULT]}"
fi

echo "Loading driver $EPIPHAN"

# attempt to load the vga2usb driver
SUCCESS=0
if [ $LOAD_DRIVER -eq 0 ]; then
  sudo -u $USERNAME mkdir -p drivers
  sudo -u $USERNAME wget $DRIVER_URL
  if [ $? -ne 0 ]; then
    SUCCESS=1
  fi
  mv $EPIPHAN drivers/
  cd drivers/
  sudo -u $USERNAME tar jxf $EPIPHAN
  sudo make load
  if [ $? -ne 0 ]; then
    SUCCESS=1
  fi
  cd ..
fi

if [ $SUCCESS -ne 0 ]; then
  echo "Failed to load Epiphan driver. Try to do it manually."
fi

sudo echo "deb http://aifile.usask.ca/apt-mirror/mirror/archive.ubuntu.com/ubuntu/ karmic main restricted universe multiverse" >> sources.list
sudo echo "deb http://aifile.usask.ca/apt-mirror/mirror/archive.ubuntu.com/ubuntu/ karmic-updates main restricted universe multiverse" >> sources.list
sudo echo "deb http://security.ubuntu.com/ubuntu karmic-security main restricted universe multiverse" >> sources.list

sudo mv sources.list /etc/apt/sources.list

sudo debconf-set-selections <<\EOF
postfix postfix/mailname string fax
postfix postfix/main_mailer_type select Internet Site
EOF
sudo debconf-set-selections <<EOF
sun-java5-jdk shared/accepted-sun-dlj-v1-1 boolean true
?sun-java6-jdk shared/accepted-sun-dlj-v1-1 boolean true
EOF

echo "Installing third party packages from Ubuntu repository..."
sudo apt-get update
sudo apt-get -y --force-yes install v4l-conf ivtv-utils maven2 sun-java6-jdk subversion wget curl openssh-server

export JAVA_HOME=/usr/lib/jvm/java-6-sun-1.6.0.15
export FELIX_FILENAME=felix-framework-2.0.1.tar.gz
export FELIX_URL=http://apache.mirror.iweb.ca/felix/$FELIX_FILENAME
export FELIX_HOME=$HOME/felix-framework-2.0.1
export M2_REPO=$HOME/.m2/repository

cd
if [ -d $FELIX_HOME ]; then
  rm -rf $FELIX_HOME
fi
curl $FELIX_URL | tar xz 
sudo -u $USERNAME mkdir ${FELIX_HOME}/load

echo "Installing jv4linfo..."
sudo -u $USERNAME wget http://luniks.net/luniksnet/download/java/jv4linfo/jv4linfo-0.2.1-src.jar
sudo -u $USERNAME jar xf jv4linfo-0.2.1-src.jar
cd jv4linfo/src
# The ant build script has a hardcoded path to the openjdk, this sed line will
# switch it to be whatever is defined in JAVA_HOME
sudo -u $USERNAME sed -i '74i\\t<arg value="-fPIC"/>' build.xml
sudo -u $USERNAME sed -i "s#\"\/usr\/lib\/jvm\/java-6-openjdk\/include\"#\"$JAVA_HOME\/include\"#g" build.xml

sudo -u $USERNAME ant
sudo cp ../lib/libjv4linfo.so /usr/lib

cd ../..

# setup properties by calling config.sh
sudo -u $USERNAME cp $SETUP_PROPS .
sudo chown $USERNAME:$USERNAME ./config.sh
sudo -u $USERNAME ./config.sh

# build properties
sudo -u $USERNAME cp $HOME/build_matterhorn .
sudo chown $USERNAME:$USERNAME ./build_matterhorn.sh
sudo -u $USERNAME ./build_matterhorn.sh

# set matterhorn to start on boot
sudo -u $USERNAME echo "alias matterhorn=$FELIX_HOME/bin/start_matterhorn.sh" >> .bashrc
chmod 755 $FELIX_HOME/bin/start_matterhorn.sh
sudo echo "$FELIX_HOME/bin/start_matterhorn.sh" >> $HOME/matterhorn_capture.sh
sudo chown root:root $HOME/matterhorn_capture.sh
sudo chmod 777 $HOME/matterhorn_capture.sh
sudo mv $HOME/matterhorn_capture.sh /etc/init.d
sudo ln -s /etc/init.d/matterhorn_capture.sh /etc/rc2.d/S99matterhorn_capture
sudo chown root:root /etc/rc2.d/S99matterhorn_capture
sudo chmod 777 /etc/rc2.d/S99matterhorn_capture


