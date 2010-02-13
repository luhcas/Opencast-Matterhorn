#! /bin/sh

# Where to install ffmpeg
FPATH="/c"

# Building autoconfig
cd autoconf-2.63
./configure --prefix=/usr
make
make install
cd ..

# Building automake
cd automake-1.10.2
./configure --prefix=/usr
make
make install
cd ..

# Building libtool
cd libtool-1.5.26
./configure --prefix=/usr
make
make install
cd ..

# Building faad
patch -p0 < faad2-2.6.1.patch
cd faad2
sh bootstrap
./configure --prefix=/static --enable-static --disable-shared
make LDFLAGS="-no-undefined"
make install
cd ..

# Building ffmpeg
mkdir $FPATH/ffmpeg
mkdir ffmpeg
cd ffmpeg
svn checkout svn://svn.ffmpeg.org/ffmpeg/trunk@20641 source
rm -rf source/libswscale
svn checkout svn://svn.mplayerhq.hu/mplayer/trunk/libswscale@30380 source/libswscale
mkdir build
cd build
../source/configure --enable-memalign-hack --target-os=mingw32 --enable-shared --enable-libfaad --enable-gpl --extra-ldflags=-L/usr/static/lib --extra-cflags=-I/usr/static/include --prefix=$FPATH/ffmpeg
make
make install