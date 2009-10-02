If you use maven for building instead of FlexBuilder:


Make sure that Flash Player is available on your clathpath; 
Important to run the FlexUnitTests

on MacOSX
export PATH=$PATH:/pathtoflashplayer/Flash\ Player.app/Contents/MacOS



-------

Maven flex:mojo has currently issues with Flash player 10 + FlexUnit4
A workaround is currently:

Comment the lines: 
<testSourceDirectory>src/test/flex</testSourceDirectory>
<!-- <testSourceDirectory>src/test/flex</testSourceDirectory> -->
AND

<!--
<dependency>
		<groupId>org.sonatype.flexmojos</groupId>
		<artifactId>flexmojos-unittest-support</artifactId>
		<version>3.3.0</version>
		<type>swc</type>
		<scope>test</scope>
</dependency>
-->

-------


mvn clean install


-------

Uncomment the lines again
mvn install



Missing Maven Plugins for the Swiz and the MediaFramework
Install this missing libs in your local repository

Go to EngageApp_Player lib folder in your terminal:

Example:
mvn install:install-file -DgroupId=swiz-0.6.2 -DartifactId=swiz-0.6.2_framework -Dversion=3.3.0.4852 -Dpackaging=swc -Dfile=swiz-0.6.2.swc
mvn install:install-file -DgroupId=MediaFramework -DartifactId=MediaFramework_framework -Dversion=3.3.0.4852 -Dpackaging=swc -Dfile=MediaFramework.swc


Help to fix this problems is welcome!
