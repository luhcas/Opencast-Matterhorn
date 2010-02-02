#! /bin/bash
# Configure capture agent for use with Matterhorn



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

# determine which vga2usb driver to load for this kernel
# if this variable remains 0, we attempt to load the driver
LOAD_DRIVER=0
EPIPHAN=""
echo "System information: `uname -a`"
echo "Here is a list of supported Epiphan VGA2USB drivers:"
for ((i = 0; i < ${#drivers[@]}; i++ ))
do
  echo -e "\t($i)\t${drivers[$i]}"
done
echo -n "Choose an option: "
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
  echo "Not loading Epiphan VGA2USB driver."
  LOAD_DRIVER=1
fi

# attempt to load the vga2usb driver
SUCCESS=0
if [ $LOAD_DRIVER -eq 0 ]; then
  mkdir -p drivers
  wget $DRIVER_URL
  if [ $? -ne 0 ]; then
    SUCCESS=1
  fi
  mv $EPIPHAN drivers/
  cd drivers/
  tar jxf $EPIPHAN
  make load
  if [ $? -ne 0 ]; then
    SUCCESS=1
  fi
  cd ..
fi

if [ $SUCCESS -ne 0 ]; then
  echo "Failed to load Epiphan driver. Try to do it manually."
fi
exit

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
mkdir ${FELIX_HOME}/load

echo "Installing jv4linfo..."
wget http://luniks.net/luniksnet/download/java/jv4linfo/jv4linfo-0.2.1-src.jar
jar xf jv4linfo-0.2.1-src.jar
cd jv4linfo/src
# The ant build script has a hardcoded path to the openjdk, this sed line will
# switch it to be whatever is defined in JAVA_HOME
sed -i '74i\\t<arg value="-fPIC"/>' build.xml
sed -i "s#\"\/usr\/lib\/jvm\/java-6-openjdk\/include\"#\"$JAVA_HOME\/include\"#g" build.xml

ant
sudo cp ../lib/libjv4linfo.so /usr/lib

# configure users/directories
if [ -d /opencast ]; then
 mkdir /opencast
 sudo chown opencast:opencast /opencast
fi

# setup properties
$SETUP_PROPS
 
# get the necessary matterhorn source code
svn co http://source.opencastproject.org/svn/products/matterhorn/trunk/ $HOME/capture-agent --depth empty
cd $HOME/capture-agent
svn up pom.xml
svn co http://source.opencastproject.org/svn/products/matterhorn/trunk/docs/ docs
svn co http://source.opencastproject.org/svn/modules/opencast-runtime-tools/trunk opencast-runtime-tools
svn co http://source.opencastproject.org/svn/modules/opencast-build-tools/trunk/ opencast-build-tools
svn co http://source.opencastproject.org/svn/modules/opencast-util/trunk/ opencast-util
svn co http://source.opencastproject.org/svn/modules/opencast-media/trunk/ opencast-media
svn co http://source.opencastproject.org/svn/modules/opencast-capture-admin-service-api/trunk/ opencast-capture-admin-service-api
svn co http://source.opencastproject.org/svn/modules/opencast-capture-admin-service-impl/trunk/ opencast-capture-admin-service-impl
svn co http://source.opencastproject.org/svn/modules/opencast-capture-service-api/trunk/ opencast-capture-service-api
svn co http://source.opencastproject.org/svn/modules/opencast-capture-service-impl/trunk/ opencast-capture-service-impl

# setup felix configuration
cp -r docs/felix/bin/* $FELIX_HOME/bin
cp -r docs/felix/conf/* $FELIX_HOME/conf

# build matterhorn using Maven
cd opencast-runtime-tools
mvn clean install
cd ../opencast-build-tools
mvn clean install
cd ../opencast-capture-admin-service-impl
mvn clean install
cd ..
mvn clean install -Pcapture -DskipTests -DdeployTo=$FELIX_HOME/load

# start felix
echo "alias matterhorn=$FELIX_HOME/bin/start_matterhorn.sh" >> ~/.bashrc
chmod 755 $FELIX_HOME/bin/start_matterhorn.sh
sudo sed -i 's/exit 0//g' /etc/rc.local
sudo echo "$FELIX_HOME/bin/start_matterhorn.sh" >> /etc/rc.local
$FELIX_HOME/bin/start_matterhorn.sh

