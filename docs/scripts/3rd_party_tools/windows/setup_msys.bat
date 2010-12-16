@echo off

SETLOCAL

rem ####################################################
rem ############### Configurations #####################
rem ####################################################

rem Without ending backslash
SET TEMP_DIR=%temp%\3rd_party_tmp

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
if exist %TEMP_DIR% (
	rmdir /s /q %TEMP_DIR%
)
mkdir %TEMP_DIR%

echo.
echo Copying installation scripts and patches
echo.
if exist install_3rd_party.sh (
	copy install_3rd_party.sh %TEMP_DIR%
) else (
 	echo Installation script install_3rd_party.sh is missing!
	goto EXCEPTION
)
if exist patches (
	xcopy /s /i patches %TEMP_DIR%\patches
) else (
	echo Directory with patches is missing!
	goto EXCEPTION
)

cd /d %TEMP_DIR%

echo.
echo Downloading Mingw required libraries
echo.
wget "http://switch.dl.sourceforge.net/project/mingw/MinGW/BaseSystem/GNU-Binutils/binutils-2.20.1/binutils-2.20.1-2-mingw32-bin.tar.gz"
if errorlevel 1 (
	cd ..
	goto EXCEPTION
)
wget "http://switch.dl.sourceforge.net/project/mingw/MinGW/BaseSystem/GCC/Version4/Previous Release gcc-4.4.0/gcc-full-4.4.0-mingw32-bin-2.tar.lzma"
if errorlevel 1 (
	cd ..
	goto EXCEPTION
)
wget "http://switch.dl.sourceforge.net/project/mingw/MinGW/BaseSystem/RuntimeLibrary/MinGW-RT/mingwrt-3.18/mingwrt-3.18-mingw32-dll.tar.gz"
if errorlevel 1 (
	cd ..
	goto EXCEPTION
)
wget "http://switch.dl.sourceforge.net/project/mingw/MinGW/BaseSystem/RuntimeLibrary/MinGW-RT/mingwrt-3.18/mingwrt-3.18-mingw32-dev.tar.gz"
if errorlevel 1 (
	cd ..
	goto EXCEPTION
)
wget "http://switch.dl.sourceforge.net/project/mingw/MinGW/BaseSystem/RuntimeLibrary/Win32-API/w32api-3.14/w32api-3.14-mingw32-dev.tar.gz"
if errorlevel 1 (
	cd ..
	goto EXCEPTION
)

echo.
echo Downloading MSys and required libraries
echo.
wget "http://switch.dl.sourceforge.net/project/mingw/MSYS/BaseSystem/msys-core/_obsolete/coreutils-5.97-MSYS-1.0.11-2/coreutils-5.97-MSYS-1.0.11-snapshot.tar.bz2"
if errorlevel 1 (
	cd ..
	goto EXCEPTION
)
wget "http://switch.dl.sourceforge.net/project/mingw/MSYS/BaseSystem/msys-core/msys-1.0.11/MSYS-1.0.11.exe"
if errorlevel 1 (
	cd ..
	goto EXCEPTION
)
wget "http://switch.dl.sourceforge.net/project/mingw/MSYS/Supplementary Tools/msysDTK-1.0.1/msysDTK-1.0.1.exe"
if errorlevel 1 (
	cd ..
	goto EXCEPTION
)

echo.
echo Script will now run installer for MSYS. On post installation
echo script answer no (n). After installation is complete uncheck both
echo boxes and close the installer.
echo.
pause

rem Installing MSYS
start /wait "Msys installation" MSYS-1.0.11.exe

rem Looking for msys location
for /f "tokens=6*" %%i in ('reg query "HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\MSYS-1.0_is1" /v "Inno Setup: App Path"') do (
	set MSYS_DIR=%%i
)
if "%MSYS_DIR%" == "" (
	for /f "tokens=6*" %%i in ('reg query "HKEY_LOCAL_MACHINE\SOFTWARE\Wow6432Node\Microsoft\Windows\CurrentVersion\Uninstall\MSYS-1.0_is1" /v "Inno Setup: App Path"') do (
		set MSYS_DIR=%%i
	)
)

mkdir %MSYS_DIR%\mingw

rem Unpacking
echo.
echo Unpacking and setting up MinGW environment
echo.
7z x -y binutils-2.20.1-2-mingw32-bin.tar.gz
if errorlevel 1 (
	cd ..
	goto EXCEPTION
)
7z x -y gcc-full-4.4.0-mingw32-bin-2.tar.lzma
if errorlevel 1 (
	cd ..
	goto EXCEPTION
)
7z x -y mingwrt-3.18-mingw32-dev.tar.gz
if errorlevel 1 (
	cd ..
	goto EXCEPTION
)
7z x -y mingwrt-3.18-mingw32-dll.tar.gz
if errorlevel 1 (
	cd ..
	goto EXCEPTION
)
7z x -y w32api-3.14-mingw32-dev.tar.gz
if errorlevel 1 (
	cd ..
	goto EXCEPTION
)
7z x -y coreutils-5.97-MSYS-1.0.11-snapshot.tar.bz2
if errorlevel 1 (
	cd ..
	goto EXCEPTION
)

7z x -y -o%MSYS_DIR%\mingw\ binutils-2.20.1-2-mingw32-bin.tar
if errorlevel 1 (
	cd ..
	goto EXCEPTION
)
7z x -y -o%MSYS_DIR%\mingw\ gcc-full-4.4.0-mingw32-bin-2.tar
if errorlevel 1 (
	cd ..
	goto EXCEPTION
)
7z x -y -o%MSYS_DIR%\mingw\ mingwrt-3.18-mingw32-dev.tar
if errorlevel 1 (
	cd ..
	goto EXCEPTION
)
7z x -y -o%MSYS_DIR%\mingw\ mingwrt-3.18-mingw32-dll.tar
if errorlevel 1 (
	cd ..
	goto EXCEPTION
)
7z x -y -o%MSYS_DIR%\mingw\ w32api-3.14-mingw32-dev.tar
if errorlevel 1 (
	cd ..
	goto EXCEPTION
)

rem configuring msys binding for mingw
echo %MSYS_DIR:\=/%/mingw /mingw> %MSYS_DIR%\etc\fstab

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
copy coreutils-5.97\bin\pr.exe %MSYS_DIR%\bin\
copy coreutils-5.97\bin\pwd.exe %MSYS_DIR%\bin\

echo.
echo Script will now launch MSYS Development Kit installer. Please
echo install under same directory as MSYS (%MSYS_DIR%) and
echo close installer after installation is complete.
echo.
pause

start /wait "MsysDTK installation" msysDTK-1.0.1.exe

rem Building script for required libraries and ffmpeg
mkdir %MSYS_DIR%\home\%username%
copy install_3rd_party.sh %MSYS_DIR%\home\%username%\
xcopy /s /i patches %MSYS_DIR%\home\%username%\patches

rem Cleaning
echo.
echo Cleaning up
echo.
cd ..
rmdir /s /q %TEMP_DIR%

echo.
echo Script finished setting up MSYS + MinGW environments.
echo New shell will open where you can execute: sh install_3rd_party.sh
echo.
pause
call %MSYS_DIR%\msys.bat -norxvt

ENDLOCAL

goto END

:EXCEPTION

if exist %TEMP_DIR% (
	echo.
	echo Cleaning temporary directory...
	echo.
	rmdir /s /q %TEMP_DIR%
)
echo ***************************************************
echo * ERROR: Msys + Mingw system installation failed! *
echo ***************************************************

:END