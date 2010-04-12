INSTALLING 3rd PARTY TOOLS

For installing 3rd party tools you will need certain tools on your system.
To automatically install all required tools you can execute preinstall_redhat.sh
for redhat based systems or preinstall_debian.sh for debian based systems.
If you don't execute scripts as privileged user you will be asked for password
when needed. When reqired tools are installed main installation script will be
automatically executed.

Second way is to install all packages manually and execute main script 
(install_3rd_party.sh) directly.
Tools that you need to install:
- c/c++ compiler
- zlib-devel
- pgk-config
- yacc
- subversion
- patch
- wget
If possible also install the following packages:
- libjpeg-devel
- libpng-devel
- libtiff-devel
- jam
In case that certain package is not available you can pass argument to the main
installation script for installing from source:
- jpeg for libjpeg-devel
- png for libpng-devel
- tiff for libtiff-devel
- jam for jam
Main script should be executed as privileged user, otherwise you will receive
'permission denied' errors when trying to install 3rd party tools.
