INSTALLING 3rd PARTY TOOLS

Prerequisites:
- 7zip archiver [http://www.7-zip.org/]
- wget for Windows OS [http://gnuwin32.sourceforge.net/packages/wget.htm]
- command line subversion client [http://subversion.apache.org/packages.html]

Make sure that all binaries (7z, wget and svn) can be reached from command line by putting paths to 7zip
and wget binaries in PATH environment variable.

Using scripts:
- OPTIONAL: you can specify location and name of temporary directory in setup_msys.bat (default: ./$temp)
- OPTIONAL: you can specify desired location of 3rd party tools by modifying install_3rd_party.sh with notepad
	    or similar program. Use Unix style paths, for example C:\Matterhorn is written as /c/Matterhorn
	    Default is /c (or C:)
- execute setup_msys.bat
- after downloading of required files is completed, setup will execute installer for MSys. Chose directory
  where you want to install it (IMPORTANT: use path without spaces). When post installation script asks you
  if you would like to set MinGW environment answer no (n). Uncheck both boxes and close the installer.
- after configuring MinGW environment installer for MSys development kit will be executed.
  IMPORTANT: Install in the same directory as you installed MSys. Directory will be also writen in command line.
- When environment is successfully set new shell will open. Execute $ sh install_3rd_party.sh
  IMPORTANT: Do not interupt script. Configuring ffmpeg on Windows OS takes quite a lot of time.
- 3rd party tools will be in directory you specified (MediaInfo and FFmpeg directory)