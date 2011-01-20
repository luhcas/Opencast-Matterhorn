This is a proof-of-concept for a Distributed OSGI environment for developing Matterhorn.

To install and run this software:

1) Download the binary distribution of Felix 2.0.0.  TODO: Test on Equinox.
2) Build this software using maven (mvn -DskipTests install).  The first time you install, maven will download all of the dependencies and transitive dependencies for all of the Matterhorn code.  Don't be surprised if this takes a long time. You may need to increase your JVM memory settings.  I do this in bash with:

export MAVEN_OPTS="-Xms256m -Xmx512m -XX:PermSize=64m -XX:MaxPermSize=128m"

3) Copy docs/felix_config.properties to <felix_root>/conf/config.properties and edit the matterhorn.* properties at the bottom of the file to match your environment.
4) Start felix with cd <felix_root>; java -DM2_REPO=[your home directory]/.m2/repository/ -jar bin/felix.jar
5) Visit http://localhost:8080/samplews?wsdl to see the sample web service endpoint.  TODO: expose a URL describing all of the service endpoints, static resource aliases, etc.

Other URLs of interest:

Sample HTML form to upload binary data to the repository: http://localhost:8080/samplehtml/upload.html (warning: uploading returns HTTP 200, so "nothing happens" in the browser)
Get binary data from the repository: http://localhost:8080/repository/data/[path] (warning: this returns a file with no filename or mime type)
(TODO) Get metadata from the repository: GET http://localhost:8080/repository/metadata/[key]/[path]
(TODO) Put metadata into the repository: POST or PUT http://localhost:8080/repository/metadata/[key]/[path]


Logging:

Logging configuration can be customized by modifying the properties file located at $FELIX/conf/services named org.ops4j.pax.logging.properties.

