@echo off

SETLOCAL

SET ARC_BIN="C:\Program Files\7-Zip\7z.exe"
SET ARC_OPT=x -y
SET ARC_DEST_OPT=-o
rem Without ending backslash
SET MSYS_PATH="C:"

rem creating temporary directory and copy all required files in there
mkdir %MSYS_PATH%\$temp

rem Mingw required libraries
copy binutils-2.19.1-mingw32-bin.tar.gz %MSYS_PATH%\$temp\
copy gcc-core-4.2.1-sjlj-2.tar.gz %MSYS_PATH%\$temp\
copy "gcc-g++-4.2.1-sjlj-2.tar.gz" %MSYS_PATH%\$temp\
copy mingwrt-3.15.2-mingw32-dev.tar.gz %MSYS_PATH%\$temp\
copy mingwrt-3.15.2-mingw32-dll.tar.gz %MSYS_PATH%\$temp\
copy w32api-3.13-mingw32-dev.tar.gz %MSYS_PATH%\$temp\

rem MSys and required libraries
copy coreutils-5.97-MSYS-1.0.11-snapshot.tar.bz2 %MSYS_PATH%\$temp\
copy MSYS-1.0.11.exe %MSYS_PATH%\$temp\
copy msysDTK-1.0.1.exe %MSYS_PATH%\$temp\

rem MsysDVLPR requred libraries
copy msysDVLPR-1.0.0-alpha-1.tar.gz %MSYS_PATH%\$temp\

rem libraries for updating autotools
copy m4-1.4.12.tar.bz2 %MSYS_PATH%\$temp\
copy autoconf-2.63.tar.bz2 %MSYS_PATH%\$temp\
copy automake-1.10.2.tar.bz2 %MSYS_PATH%\$temp\
copy libtool-1.5.26.tar.gz %MSYS_PATH%\$temp\

rem faad library
copy faad2-2.6.1.tar.gz %MSYS_PATH%\$temp\

rem patches
copy m4-1.4.12-MSYS.diff %MSYS_PATH%\$temp\
copy faad2-2.6.1.patch %MSYS_PATH%\$temp\

rem Building scripts script
copy install_ffmpeg.sh %MSYS_PATH%\$temp\
copy msysDVLPR.sh %MSYS_PATH%\$temp\

rem Creating directory tree
cd /d %MSYS_PATH%\
mkdir msys\mingw
mkdir msysDVLPR\home\%username%

rem Unpacking
cd $temp
%ARC_BIN% %ARC_OPT% binutils-2.19.1-mingw32-bin.tar.gz
%ARC_BIN% %ARC_OPT% gcc-core-4.2.1-sjlj-2.tar.gz
%ARC_BIN% %ARC_OPT% "gcc-g++-4.2.1-sjlj-2.tar.gz"
%ARC_BIN% %ARC_OPT% mingwrt-3.15.2-mingw32-dev.tar.gz
%ARC_BIN% %ARC_OPT% mingwrt-3.15.2-mingw32-dll.tar.gz
%ARC_BIN% %ARC_OPT% w32api-3.13-mingw32-dev.tar.gz
%ARC_BIN% %ARC_OPT% coreutils-5.97-MSYS-1.0.11-snapshot.tar.bz2
%ARC_BIN% %ARC_OPT% msysDVLPR-1.0.0-alpha-1.tar.gz
%ARC_BIN% %ARC_OPT% m4-1.4.12.tar.bz2
%ARC_BIN% %ARC_OPT% autoconf-2.63.tar.bz2
%ARC_BIN% %ARC_OPT% automake-1.10.2.tar.bz2
%ARC_BIN% %ARC_OPT% libtool-1.5.26.tar.gz
%ARC_BIN% %ARC_OPT% faad2-2.6.1.tar.gz

%ARC_BIN% %ARC_OPT% %ARC_DEST_OPT%..\msys\mingw\ binutils-2.19.1-mingw32-bin.tar
%ARC_BIN% %ARC_OPT% %ARC_DEST_OPT%..\msys\mingw\ gcc-core-4.2.1-sjlj-2.tar
%ARC_BIN% %ARC_OPT% %ARC_DEST_OPT%..\msys\mingw\ "gcc-g++-4.2.1-sjlj-2.tar"
%ARC_BIN% %ARC_OPT% %ARC_DEST_OPT%..\msys\mingw\ mingwrt-3.15.2-mingw32-dev.tar
%ARC_BIN% %ARC_OPT% %ARC_DEST_OPT%..\msys\mingw\ mingwrt-3.15.2-mingw32-dll.tar
%ARC_BIN% %ARC_OPT% %ARC_DEST_OPT%..\msys\mingw\ w32api-3.13-mingw32-dev.tar

%ARC_BIN% %ARC_OPT% %ARC_DEST_OPT%..\msysDVLPR\home\%username%\ m4-1.4.12.tar

%ARC_BIN% %ARC_OPT% %ARC_DEST_OPT%..\msys\home\%username%\ autoconf-2.63.tar
%ARC_BIN% %ARC_OPT% %ARC_DEST_OPT%..\msys\home\%username%\ automake-1.10.2.tar
%ARC_BIN% %ARC_OPT% %ARC_DEST_OPT%..\msys\home\%username%\ libtool-1.5.26.tar
%ARC_BIN% %ARC_OPT% %ARC_DEST_OPT%..\msys\home\%username%\ faad2.tar

rem Renaming compiler names
ren ..\msys\mingw\bin\c++-sjlj.exe c++.exe
ren ..\msys\mingw\bin\cpp-sjlj.exe cpp.exe
ren ..\msys\mingw\bin\g++-sjlj.exe g++.exe
ren ..\msys\mingw\bin\gcc-sjlj.exe gcc.exe

rem Setting up Mingw + msys system + development kit
start /wait "Msys installation" MSYS-1.0.11.exe

%ARC_BIN% %ARC_OPT% coreutils-5.97-MSYS-1.0.11-snapshot.tar.bz2
%ARC_BIN% %ARC_OPT% coreutils-5.97-MSYS-1.0.11-snapshot.tar
copy coreutils-5.97\bin\pr.exe ..\msys\bin\

start /wait "MsysDTK installation" msysDTK-1.0.1.exe

rem Setting up msys devel system for buildling m4
start /wait "MsysDVLPR installation" MSYS-1.0.11.exe
%ARC_BIN% %ARC_OPT% -o..\msysDVLPR\ msysDVLPR-1.0.0-alpha-1.tar
copy m4-1.4.12-MSYS.diff ..\msysDVLPR\home\%username%\
copy msysDVLPR.sh ..\msysDVLPR\home\%username%\

rem Building script for required libraries and ffmpeg
copy install_ffmpeg.sh ..\msys\home\%username%\
copy faad2-2.6.1.patch ..\msys\home\%username%\

rem Cleaning
cd ..
rmdir /s /q $temp

ENDLOCAL

call msysDVLPR\msys.bat -norxvt
