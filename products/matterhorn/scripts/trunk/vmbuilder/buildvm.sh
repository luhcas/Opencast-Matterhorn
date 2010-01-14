#!/bin/sh

#install extras that we need if running this script
sudo apt-get install ubuntu-vm-builder subversion zip git-core maven2

#delete the old vm if it exists
sudo rm -rf ubuntu-vmw6/
sudo rm -rf mnt

#build the ubuntu vm
sudo ubuntu-vm-builder vmw6 karmic --arch 'i386' --mem '512' --cpus 1 \
--rootsize '8192' --swapsize '1024' --kernel-flavour='virtual' \
--hostname 'opencast' --mirror 'http://aifile.usask.ca/apt-mirror/mirror/archive.ubuntu.com/ubuntu/' \
--components 'main,universe' --name 'opencast' --user 'opencast' \
--pass 'matterhorn' --tmpfs - --addpkg zlib1g-dev --addpkg patch \
--addpkg byacc --addpkg libcv1 --addpkg libcv-dev --addpkg opencv-doc \
--addpkg build-essential --addpkg subversion --addpkg git-core \
--addpkg checkinstall --addpkg yasm --addpkg texi2html  --addpkg libsdl1.2-dev \
--addpkg libtheora-dev --addpkg libx11-dev \
--addpkg zlib1g-dev --addpkg libpng12-dev --addpkg libjpeg62-dev \
--addpkg libtiff4-dev --addpkg ssh --addpkg maven2 --addpkg subversion \
--addpkg wget --addpkg curl --addpkg update-motd \
--addpkg expect-dev --addpkg expect --addpkg samba --addpkg nano

#change the vm to use nat networking instead of bridged
sed -i 's/bridged/nat/g' ubuntu-vmw6/opencast.vmx

#mount the vm image
mkdir mnt
sudo vmware-mount ubuntu-vmw6/disk0.vmdk 1 mnt

#set the mirror that the vm should be using to download sources, making sure multiverse is in there for aac/etc
echo "deb http://aifile.usask.ca/apt-mirror/mirror/archive.ubuntu.com/ubuntu/ karmic main restricted universe multiverse" > sources.list
echo "deb http://aifile.usask.ca/apt-mirror/mirror/archive.ubuntu.com/ubuntu/ karmic-updates main restricted universe multiverse" >> sources.list
echo "deb http://security.ubuntu.com/ubuntu karmic-security main restricted universe multiverse" >> sources.list

#copy sources list into the vm image
sudo mv sources.list mnt/etc/apt/sources.list

#copy config scripts into vm
sudo cp config.sh mnt/home/opencast/config.sh
sudo chmod 755 mnt/home/opencast/config.sh
sudo cp external_tools.sh mnt/home/opencast/external_tools.sh
sudo chmod 755 mnt/home/opencast/external_tools.sh

#check out svn
if [ -e opencast ]; then
  cd opencast
  svn up
  cd ..
else
  svn co http://source.opencastproject.org/svn/products/matterhorn/trunk/ opencast
fi
sudo cp -r opencast mnt/home/opencast/
export OC_REV=`svn info opencast | awk /Revision/ | cut -d " " -f 2`

#check out ffmpeg
if [ -e ffmpeg ]; then
  cd ffmpeg
  svn up
  cd ..
else
  svn checkout svn://svn.ffmpeg.org/ffmpeg/trunk ffmpeg
fi
sudo cp -r ffmpeg mnt/home/opencast/

#check out x264
if [ -e x264 ]; then
  cd x264
  git pull
  cd ..
else
  git clone -n git://git.videolan.org/x264.git
fi
sudo cp -r x264 mnt/home/opencast/

#get maven to update whatever dependancies we might have for opencast
cd opencast
#todo: username is hard coded, how can I get around this?
sudo -u cab938 mvn -fn -DskipTests
cd ..

#copy the maven repo across
sudo cp -r .m2 mnt/home/opencast/

#try and mount the first boot script
sudo cp -r firstboot.sh mnt/etc/init.d/
ln -s /etc/init.d/firstboot.sh mnt/etc/rc2.d/S99firstboot
sudo chmod 755 mnt/etc/init.d/firstboot.sh
sudo chmod 755 mnt/etc/rc2.d/S99firstboot

#mediainfo bug
ln -s mnt/usr/bin/mediainfo mnt/usr/local/bin/mediainfo

#lets set opencast to own her files
sudo chown 1000:1000 mnt/home/opencast/*
cd mnt/home/opencast/
sudo find .svn -exec chown 1000:1000 '{}' \;
sudo find .m2 -exec chown 1000:1000 '{}' \;
cd ../../..

#unmount the vm disk image and cleanup
sudo vmware-mount -d mnt
sleep 2
sudo rm -rf mnt

#archive it all for download
echo "Building archive opencast-$OC_REV.zip."
zip -db -r opencast-$OC_REV.zip ubuntu-vmw6

#copy it to the web
#scp opencast-$OC_REV.zip cab938@aries:/var/www/opencast/unofficial-vms/

