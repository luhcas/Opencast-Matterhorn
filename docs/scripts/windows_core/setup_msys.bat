@echo off

SETLOCAL

rem ####################################################
rem ############### Configurations #####################
rem ####################################################

rem Without ending backslash
SET MSYS_PATH=C:\Matterhorn

rem ####################################################
rem ################ Execution script ##################
rem ####################################################

echo This script will setup and install MSys + MinGW environment
echo for building third party tools for Matterhorn. You should
echo have administrative rights before executing this script.
echo IMPORTANT: 7Zip and Wget for Windows OS are reqired
echo.
echo ***WARNING***: If you have cygwin installed it is recommended
echo that you uninstall it. Otherwise you may experience build failures.
echo.
pause

rem creating temporary directory and coping all required files in there
mkdir %MSYS_PATH%\$temp

echo.
echo Copying installation scripts and patches
echo.
if exist install_3rd_party.sh (
	copy install_3rd_party.sh %MSYS_PATH%\$temp\
) else (
	echo Installation script install_3rd_party.sh is missing!
	goto EXCEPTION
)
rem Uncomment for rebuilding autoconfiguration tools (required for faad lib)
rem if exist msys_devel.sh ( 
rem 	copy msys_devel.sh %MSYS_PATH%\$temp\
rem ) else (
rem 	echo Installation script msys_devel.sh is missing!
rem 	goto EXCEPTION
rem )

rem patches
rem Uncomment for rebuilding autoconfiguration tools (required for faad lib)
rem if exist m4-1.4.12-MSYS.diff (
rem 	copy m4-1.4.12-MSYS.diff %MSYS_PATH%\$temp\
rem ) else (
rem 	echo Patch m4-1.4.12-MSYS.diff is missing!
rem 	goto EXCEPTION
rem )
rem if exist faad2-2.7.patch (
rem 	copy faad2-2.7.patch %MSYS_PATH%\$temp\
rem ) else (
rem 	echo Patch faad2-2.7.patch is missing!
rem 	goto EXCEPTION
rem )

cd /d %MSYS_PATH%\$temp

echo.
echo Downloading Mingw required libraries
echo.
wget http://prdownloads.sourceforge.net/mingw/binutils-2.19.1-mingw32-bin.tar.gz
if errorlevel 1 (
	cd ..
	goto EXCEPTION
)
wget http://prdownloads.sourceforge.net/mingw/gcc-core-4.2.1-sjlj-2.tar.gz
if errorlevel 1 (
	cd ..
	goto EXCEPTION
)
wget http://prdownloads.sourceforge.net/mingw/gcc-g++-4.2.1-sjlj-2.tar.gz
if errorlevel 1 (
	cd ..
	goto EXCEPTION
)
wget http://prdownloads.sourceforge.net/mingw/mingwrt-3.15.2-mingw32-dev.tar.gz
if errorlevel 1 (
	cd ..
	goto EXCEPTION
)
wget http://prdownloads.sourceforge.net/mingw/mingwrt-3.15.2-mingw32-dll.tar.gz
if errorlevel 1 (
	cd ..
	goto EXCEPTION
)
wget http://prdownloads.sourceforge.net/mingw/w32api-3.13-mingw32-dev.tar.gz
if errorlevel 1 (
	cd ..
	goto EXCEPTION
)

echo.
echo Downloading MSys and required libraries
echo.
wget http://prdownloads.sourceforge.net/mingw/coreutils-5.97-MSYS-1.0.11-snapshot.tar.bz2
if errorlevel 1 (
	cd ..
	goto EXCEPTION
)
wget http://prdownloads.sourceforge.net/mingw/MSYS-1.0.11.exe
if errorlevel 1 (
	cd ..
	goto EXCEPTION
)
wget http://prdownloads.sourceforge.net/mingw/msysDTK-1.0.1.exe
if errorlevel 1 (
	cd ..
	goto EXCEPTION
)

rem Uncomment for rebuilding autoconfiguration tools (required for faad lib)
rem echo.
rem echo Downloading MSys Development libraries
rem echo.
rem wget http://prdownloads.sourceforge.net/mingw/msysDVLPR-1.0.0-alpha-1.tar.gz
rem if errorlevel 1 (
rem 	cd ..
rem 	goto EXCEPTION
rem )

cd ..

echo.
echo Creating directory tree
echo.
if not exist msys (
	goto continue
)

rem Workaround from nested if - seems to have problem with it
echo WARNING: msys directory already exists. Possibly uninstalled program.
set /p c=Do you wish to continue [y or n]? 
if %c%==y (
	rmdir /s /q msys
) else (
	goto END
)

:continue
mkdir msys

rem Uncomment for rebuilding autoconfiguration tools (required for faad lib)
rem if exist msysDVLPR (
rem 	rmdir /s /q msysDVLPR
rem )
rem mkdir msysDVLPR

echo.
echo Script will now run installer for MSYS. Please install MSYS under
echo %MSYS_PATH%\msys (without directory "1.0"). On post installation
echo script answer no (n). After installation is complete uncheck both
echo boxes and close the installer.
pause

rem Installing MSYS
start /wait "Msys installation" $temp\MSYS-1.0.11.exe

rem Creating additional building environment for building m4
rem Uncomment for rebuilding autoconfiguration tools (required for faad lib)
rem echo Creating second building environment
rem xcopy /e /y msys\* msysDVLPR\

mkdir msys\mingw

rem Unpacking
echo Unpacking and setting up MinGW environment
cd $temp
7z x -y binutils-2.19.1-mingw32-bin.tar.gz
if errorlevel 1 (
	cd ..
	goto EXCEPTION
)
7z x -y gcc-core-4.2.1-sjlj-2.tar.gz
if errorlevel 1 (
	cd ..
	goto EXCEPTION
)
7z x -y "gcc-g++-4.2.1-sjlj-2.tar.gz"
if errorlevel 1 (
	cd ..
	goto EXCEPTION
)
7z x -y mingwrt-3.15.2-mingw32-dev.tar.gz
if errorlevel 1 (
	cd ..
	goto EXCEPTION
)
7z x -y mingwrt-3.15.2-mingw32-dll.tar.gz
if errorlevel 1 (
	cd ..
	goto EXCEPTION
)
7z x -y w32api-3.13-mingw32-dev.tar.gz
if errorlevel 1 (
	cd ..
	goto EXCEPTION
)
7z x -y coreutils-5.97-MSYS-1.0.11-snapshot.tar.bz2
if errorlevel 1 (
	cd ..
	goto EXCEPTION
)
rem Uncomment to build faad library
rem 7z x -y msysDVLPR-1.0.0-alpha-1.tar.gz
rem if errorlevel 1 (
rem 	cd ..
rem 	goto EXCEPTION
rem )

7z x -y -o..\msys\mingw\ binutils-2.19.1-mingw32-bin.tar
if errorlevel 1 (
	cd ..
	goto EXCEPTION
)
7z x -y -o..\msys\mingw\ gcc-core-4.2.1-sjlj-2.tar
if errorlevel 1 (
	cd ..
	goto EXCEPTION
)
7z x -y -o..\msys\mingw\ "gcc-g++-4.2.1-sjlj-2.tar"
if errorlevel 1 (
	cd ..
	goto EXCEPTION
)
7z x -y -o..\msys\mingw\ mingwrt-3.15.2-mingw32-dev.tar
if errorlevel 1 (
	cd ..
	goto EXCEPTION
)
7z x -y -o..\msys\mingw\ mingwrt-3.15.2-mingw32-dll.tar
if errorlevel 1 (
	cd ..
	goto EXCEPTION
)
7z x -y -o..\msys\mingw\ w32api-3.13-mingw32-dev.tar

rem Renaming compiler names
ren ..\msys\mingw\bin\c++-sjlj.exe c++.exe
ren ..\msys\mingw\bin\cpp-sjlj.exe cpp.exe
ren ..\msys\mingw\bin\g++-sjlj.exe g++.exe
ren ..\msys\mingw\bin\gcc-sjlj.exe gcc.exe

rem configuring msys binding for mingw
echo %MSYS_PATH:\=/%/msys/mingw /mingw> ..\msys\etc\fstab

7z x -y coreutils-5.97-MSYS-1.0.11-snapshot.tar.bz2
if errorlevel 1 (
	cd ..
	goto EXCEPTION
)
7z x -y coreutils-5.97-MSYS-1.0.11-snapshot.tar
if errorlevel 1 (
	cd ..
	goto EXCEPTION
)
copy coreutils-5.97\bin\pr.exe ..\msys\bin\
copy coreutils-5.97\bin\pwd.exe ..\msys\bin\

echo.
echo Script will now launch MSYS Development Kit installer. Please
echo install under same directory as MSYS (%MSYS_PATH%\msys) and
echo close installer after installation is complete.
pause

start /wait "MsysDTK installation" msysDTK-1.0.1.exe

rem Setting up msys devel system for buildling m4
rem Uncomment for rebuilding autoconfiguration tools (required for faad lib)
rem echo.
rem echo Setting up second development environment
rem echo.
rem 7z x -y -o..\msysDVLPR\ msysDVLPR-1.0.0-alpha-1.tar
rem if errorlevel 1 (
rem 	cd ..
rem 	goto EXCEPTION
rem )
rem mkdir ..\msysDVLPR\home\%username%
rem copy m4-1.4.12-MSYS.diff ..\msysDVLPR\home\%username%\
rem copy msys_devel.sh ..\msysDVLPR\home\%username%\

rem Building script for required libraries and ffmpeg
mkdir ..\msys\home\%username%
copy install_3rd_party.sh ..\msys\home\%username%\
copy faad2-2.7.patch ..\msys\home\%username%\

rem Cleaning
echo.
echo Cleaning up
cd ..
rmdir /s /q $temp

rem Uncomment for rebuilding autoconfiguration tools (required for faad lib)
rem echo.
rem echo Script finished setting up MSYS + MinGW environments.
rem echo New shell will open where you can execute: sh msys_devel.sh
rem pause 
rem call .\msysDVLPR\msys.bat -norxvt

rem Comment out when building with faad 
echo.
echo Script finished setting up MSYS + MinGW environments.
echo New shell will open where you can execute: sh install_3rd_party.sh
pause
call %MSYS_PATH%\msys\msys.bat -norxvt

ENDLOCAL

goto END

:EXCEPTION

if exist $temp (
	echo Cleaning temporary directory
	rmdir /s /q $temp
)
echo ***************************************************
echo * ERROR: Msys + Mingw system installation failed! *
echo ***************************************************

:END