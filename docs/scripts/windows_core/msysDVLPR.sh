#! /bin/sh

#path to your local msys + mingw installation
MSYS_PATH="/c/msys"

patch -p0 < M4-1.4.12-MSYS.diff
cd m4-1.4.12
./configure
make
cp src/m4.exe $MSYS_PATH/bin/m4.exe