#!/bin/sh

HOME=`pwd`
UBUNTU_MIRROR=http://de.archive.ubuntu.com/ubuntu/
export M2=`pwd`/m2/

#install extras that we need if running this script
sudo apt-get -y install ubuntu-vm-builder subversion zip git-core maven2

if which vmware-mount >/dev/null; then
	echo "VMware Mounter is installed."
else
	echo "VMware Mounter is not installed!"
	exit
fi

#delete the old vm if it exists
if [ -z "$(mount | grep `pwd`/mnt)" ];
 then
	echo "Nothing mounted"
else
	sudo vmware-mount -d mnt
	sleep 2
fi
sudo rm -rf ubuntu-vmw6/
sudo rm -rf mnt

echo "=========================="
echo "========Building VM======="
echo "========Please Wait======="
echo "=========================="

#set the mirror that the vm should be using to download sources, making sure multiverse is in there for aac/etc
echo "deb $UBUNTU_MIRROR karmic main restricted universe multiverse" > sources.list
echo "deb $UBUNTU_MIRROR karmic-updates main restricted universe multiverse" >> sources.list
echo "deb http://archive.canonical.com/ubuntu karmic partner" >> sources.list
echo "deb http://security.ubuntu.com/ubuntu karmic-security main restricted universe multiverse" >> sources.list

#build the ubuntu vm
sudo ubuntu-vm-builder vmw6 karmic --arch 'i386' --mem '512' --cpus 1 \
--rootsize '8192' --swapsize '1024' --kernel-flavour='virtual' \
--hostname 'opencast' --mirror $UBUNTU_MIRROR \
--components 'main,universe,multiverse' \
--name 'opencast' --user 'opencast' \
--pass 'matterhorn' --tmpfs - --addpkg zlib1g-dev --addpkg patch \
--addpkg byacc --addpkg libcv1 --addpkg libcv-dev --addpkg opencv-doc \
--addpkg build-essential --addpkg locate --addpkg git-core \
--addpkg checkinstall --addpkg yasm --addpkg texi2html  --addpkg libsdl1.2-dev \
--addpkg libtheora-dev --addpkg libx11-dev \
--addpkg zlib1g-dev --addpkg libpng12-dev --addpkg libjpeg62-dev \
--addpkg libtiff4-dev --addpkg ssh --addpkg maven2 --addpkg subversion \
--addpkg wget --addpkg curl --addpkg update-motd \
--addpkg expect-dev --addpkg expect --addpkg vim --addpkg nano \
--addpkg acpid --exec $HOME/postinstall.sh

#change the vm to use nat networking instead of bridged
sed -i 's/bridged/nat/g' ubuntu-vmw6/opencast.vmx

#mount the vm image
mkdir mnt
sudo vmware-mount ubuntu-vmw6/disk0.vmdk 1 mnt
if [ $? -ne 0 ]
 then
	echo "Unable to mount drive, fatal error!"
	sudo vmware-mount -d mnt
	exit
else
	echo "Drive mounted."
fi

echo "=========================="
echo "==Copying Setup Scripts==="
echo "=========================="


#set the mirror that the vm should be using to download sources, making sure multiverse is in there for aac/etc
#echo "deb $UBUNTU_MIRROR karmic main restricted universe multiverse" >> sources.list
#echo "deb $UBUNTU_MIRROR karmic-updates main restricted universe multiverse" >> sources.list
#echo "deb http://archive.canonical.com/ubuntu karmic partner" >> sources.list
#echo "deb http://security.ubuntu.com/ubuntu karmic-security main restricted universe multiverse" >> sources.list

#copy sources list into the vm image
#sudo mv sources.list mnt/etc/apt/sources.list

#copy config scripts into vm
sudo cp matterhorn_setup.sh mnt/etc/profile.d/matterhorn_setup.sh
sudo chmod 755 mnt/etc/profile.d/matterhorn_setup.sh
sudo cp startup.sh mnt/home/opencast/startup.sh
sudo chmod 755 mnt/home/opencast/startup.sh
sudo cp shutdown.sh mnt/home/opencast/shutdown.sh
sudo chmod 755 mnt/home/opencast/shutdown.sh
sudo cp update-matterhorn.sh mnt/home/opencast/update-matterhorn.sh
sudo chmod 755 mnt/home/opencast/update-matterhorn.sh
sudo cp rc.local mnt/etc/rc.local
sudo chmod 755 mnt/etc/rc.local

echo "============================"
echo "==Installing Apache Felix==="
echo "============================"

if [ ! -e felix-framework-2.0.1 ]; then
  wget http://apache.linux-mirror.org/felix/felix-framework-2.0.1.tar.gz
  tar -xzf felix-framework-2.0.1.tar.gz
fi 

#copy felix files to vm
sudo cp -r felix-framework-2.0.1 mnt/usr/local/felix-framework-2.0.1
#create needed dirs
sudo mkdir mnt/usr/local/felix-framework-2.0.1/load
sudo chown -R 1000:1000 mnt/usr/local/felix-framework-2.0.1/
sudo chmod -R 777 mnt/usr/local/felix-framework-2.0.1/

echo "=========================="/
echo "=====Fetching Opencast===="
echo "=========================="

#check out svn
if [ -e opencast ]; then
  cd opencast
  svn up
  cd ..
else
  svn co http://source.opencastproject.org/svn/products/matterhorn/trunk/ opencast
fi
sudo cp -r opencast mnt/home/opencast/
sudo cp -rf opencast/docs/felix/conf/* mnt/usr/local/felix-framework-2.0.1/conf
export OC_REV=`svn info opencast | awk /Revision/ | cut -d " " -f 2`

#echo "=========================="
#echo "=====Fetching FFMpeg======"
#echo "=========================="

#check out ffmpeg
#if [ -e ffmpeg ]; then
#  cd ffmpeg
#  svn up -r 20641
#  cd libswscale
#  svn up -r 30380
#  cd ../..
#else
#  svn checkout -r 20641 svn://svn.ffmpeg.org/ffmpeg/trunk ffmpeg
#  cd ffmpeg
#  rm -rf libswscale
#  svn checkout -r 30380 svn://svn.ffmpeg.org/mplayer/trunk/libswscale libswscale
#  cd ..
#fi
#sudo cp -r ffmpeg mnt/home/opencast/

#echo "=========================="
#echo "=====Fetching x264========"
#echo "=========================="

#check out x264
#if [ -e x264 ]; then
#  cd x264
#  git pull
#  cd ..
#else
#  git clone -n git://git.videolan.org/x264.git
#fi
#sudo cp -r x264 mnt/home/opencast/

echo "=========================="
echo "=====Building Opencast===="
echo "=========================="

#get maven to update whatever dependancies we might have for opencast
pwd
cd opencast
export MAVEN_OPTS="-Xms256m -Xmx512m -XX:PermSize=64m -XX:MaxPermSize=128m"
mvn install -fn -DskipTests -Dmaven.repo.local=$M2/repository -DdeployTo=$HOME/mnt/usr/local/felix-framework-2.0.1/load
cd ..

#copy the maven repo across
sudo cp -r $M2 mnt/home/opencast/.m2

echo "=========================="
echo "========Final Setup======="
echo "=========================="

# copy mediainfo 0.7.19

sudo cp mediainfo mnt/usr/local/bin/
sudo cp libmediainfo.a mnt/usr/local/lib/
sudo cp libmediainfo.la mnt/usr/local/lib/

#create directory for log files
sudo mkdir mnt/var/log/matterhorn
sudo chown -R 1000:1000 mnt/var/log/matterhorn

#create directory for capture agent
sudo mkdir mnt/opencast
sudo chown -R 1000:1000 mnt/opencast
sudo chmod 777 mnt/opencast

#write environment variables to login file
echo "export OC=/home/opencast/opencast" >> mnt/home/opencast/.bashrc
echo "export FELIX_HOME=/usr/local/felix-framework-2.0.1" >> mnt/home/opencast/.bashrc
echo "export M2_REPO=/home/opencast/.m2/repository" >> mnt/home/opencast/.bashrc
echo "export OC_URL=http://source.opencastproject.org/svn/products/matterhorn/trunk/" >> mnt/home/opencast/.bashrc
echo "export FELIX_URL=http://apache.mirror.iweb.ca/felix/felix-framework-2.0.1.tar.gz" >> mnt/home/opencast/.bashrc
echo "export JAVA_HOME=/usr/lib/jvm/java-6-sun" >> mnt/home/opencast/.bashrc
echo "export MAVEN_OPTS=\"-Xms256m -Xmx512m -XX:PermSize=64m -XX:MaxPermSize=128m\"" >> mnt/home/opencast/.bashrc

#lets set opencast to own her files
sudo chown -R 1000:1000 mnt/home/opencast/*
sudo chmod -R 777 mnt/home/opencast/*
sudo chmod -R 777 mnt/home/opencast/.m2/

#unmount the vm disk image and cleanup
sudo vmware-mount -d mnt
sleep 2
sudo rm -rf mnt

echo "================================="
echo "=====Image Built, compressing===="
echo "================================="

#archive it all for download
echo "Building archive opencast-$OC_REV.zip."
zip -db -r -9 opencast-$OC_REV.zip ubuntu-vmw6

#copy it to the web
#scp opencast-$OC_REV.zip cab938@aries:/var/www/opencast/unofficial-vms/

