This is a proof-of-concept for a Distributed OSGI environment for developing Matterhorn.

To install and run this software:

1) Download Felix 1.6.0.  TODO: Test on Equinox.
2) Build this software using maven2 (mvn -DskipTests install).  The first time you install, maven will download all of the dependencies and transitive dependencies for all of the Matterhorn code.  Don't be surprised if this takes a long time. You may need to increase your JVM memory settings.  I do this in bash with:

export MAVEN_OPTS="-Xms256m -Xmx512m -XX:PermSize=64m -XX:MaxPermSize=128m"

3) Copy docs/felix_config.properties to <felix_root>/conf/config.properties and edit the matterhorn.* properties at the bottom of the file.
4) Start felix with cd <felix_root>; java -DM2_REPO=~/.m2/repository/ -jar bin/felix.jar
5) Visit http://localhost:8080/samplews?wsdl to see the sample web service endpoint.  TODO: expose a URL describing all of the service endpoints, static resource aliases, etc.
