INSTALLING 3rd PARTY TOOLS

For installing 3rd party tools you will need certain tools on your system.
To automatically install all required tools you can execute:
- ./preinstall_redhat.sh for redhat based system with yum as package manager
- ./preinstall_debian.sh for debian based system with apt-get as package manager

If you don't execute scripts as privileged user you will be asked for password
when needed. When reqired tools are installed main installation (install_3rd_party.sh)
script will be automatically executed.

NOTE: If you would like to copy scripts and execute them from different directory,
make sure that all files (preinstallation script, install_3rd_party.sh and patches)
are in same directory. You may experience build failures otherwise.


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
- png for libpng-devel
- jpeg for libjpeg-devel
- tiff for libtiff-devel
- jam for jam

NOTE: Main script should be executed as privileged user, otherwise you will receive
'permission denied' errors when trying to install 3rd party tools. If you would like
to copy script and execute it from different directory, make sure that you copied all
patches to the same directory as the main script.
