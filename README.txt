This is a proof-of-concept for a Distributed OSGI environment for developing Matterhorn.

To install and run this software:

1) Download Felix 1.4.1.  Equinox should work too, but I haven't put a config file together for equinox.
2) Configure jcr/jcr-server/src/main/resources/cluster-repository-template.xml to point to your data store and transaction-capable database.  The default points to mysql on localhost, user=root, password=root.
3) Build this software using maven2 (mvn -DskipTests install)
4) Copy felix_config.properties to <felix_root>/conf/config.properties
5) Start felix with cd <felix_root>; java -DM2_REPO=~/.m2/repository/ -jar bin/felix.jar
6) Visit http://localhost:8080/samplews?wsdl to see the sample web service endpoint or http://localhost:8080/static/js/sample.js to see a js file mounted to the servlet container.
