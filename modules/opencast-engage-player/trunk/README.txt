FlexBuilder + maven
----------------------

Project uses flexmojos in order to build and maintain the code with maven. (http://code.google.com/p/flex-mojos/)

Make sure that you include

<mirror>
      <!--This sends everything else to /public -->
      <id>nexus</id>
      <mirrorOf>*</mirrorOf>
      <url>http://repository.sonatype.org/content/groups/public</url>
</mirror>

in your maven settings.xml


After checkout please run

mvn org.sonatype.flexmojos:flexmojos-maven-plugin:3.3.0:flexbuilder

in your opencast-engage-player folder.


After this you should be able to include the project in Eclipse.



-------------------


If you use maven for building instead of FlexBuilder:


Make sure that Flash Player is available on your clathpath; 
Important to run the FlexUnitTests

on MacOSX
export PATH=$PATH:/pathtoflashplayer/Flash\ Player.app/Contents/MacOS


--------------------



Missing Maven Plugins for the Swiz and the MediaFramework
Install this missing libs in your local repository

(this plugins will shortly be availbale in the opnecast maven repository)

Go to EngageApp_Player lib folder in your terminal:

Example:
mvn install:install-file -DgroupId=swiz-0.6.2 -DartifactId=swiz-0.6.2_framework -Dversion=3.3.0.4852 -Dpackaging=swc -Dfile=swiz-0.6.2.swc
mvn install:install-file -DgroupId=MediaFramework -DartifactId=MediaFramework_framework -Dversion=3.3.0.4852 -Dpackaging=swc -Dfile=MediaFramework.swc



---------------------------


IF MAVEN ISN`T YOUR FIRST CHOICE: 


0. Check out as a project in the workspace

1. Flex Project Nature --> Add Flex Project Nature

2. Properties --> Flex Build Path --> Main source folder: "src/main/flex/Videodisplay"

3. .actionScriptProperties --> 2x "main.mxml" in "Videodisplay.mxml" umbenennen

4. Properties --> Flex Build Path --> Add Folder --> "src/main/resources"

5. Properties --> Flex Build Path --> Add Folder --> "src/main/html/html-debug"

6. Properties --> Flex Compiler --> Require Flash Player version: "10.0.0"

7. Right click html-template --> Replace With --> Latest from repository

8. Delete File main.mxml

9. Properties --> Flex Build Path --> Output folder: --> <any_folder_of_your_local_server>

10. Properties --> Flex Build Path --> Output folder: --> <the_localhost_url_your_server_points_to>







