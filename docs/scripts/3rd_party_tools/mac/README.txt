INSTALLING 3rd PARTY TOOLS

Prerequisites:
- installed Developers Tools
- installed MacPorts

To automatically install all other required tools execute:
./preinstall_mac.sh 

If you don't execute scripts as privileged user you will be asked for password
when needed. When reqired tools are installed, main installation script
(install_3rd_party.sh) will be automatically executed.

NOTE: If you would like to copy scripts and execute them from different directory,
make sure that all files (preinstallation script, install_3rd_party.sh and patches)
are in same directory. You may experience build failures otherwise.


You can also execute main script (install_3rd_party.sh) directly. In this case you will
need to manually install required tools and packages.

Tools that you need to install:
- zlib
- pgkconfig
- byacc
- subversion
- wget

If possible also install the following packages:
- jpeg (development version)
- libpng (development version)
- tiff (development version)
- jam

In case that certain package is not available you can pass argument to the main
installation script for installing from source:
- png for libpng
- jpeg for jpeg
- tiff for tiff
- jam for jam

NOTE: Main script should be executed as privileged user, otherwise you will receive
'permission denied' errors when trying to install 3rd party tools. If you would like
to copy script and execute it from different directory, make sure that you copied all
patches to the same directory as the main script.
