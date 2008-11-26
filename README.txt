This is a proof-of-concept for an OSGI / Servicemix-based model for developing Matterhorn.

To install and run this software:

1) Install the latest Servicemix 4 software
2) Check out the latest Slide software (http://svn.apache.org/repos/asf/incubator/sling/trunk)
3) Build this software using maven2 (mvn -DskipTests install)
4) Start servicemix (cd servicemix_home; java -jar bin/servicemix[.bat])
5) Install the Matterhorn components you want to run.  Currently, the only useful components are the JCR Server and the Sample Component.  The file 'servicemix_opencast_installation.txt' contains the commands to install all of the third party OSGI bundles needed to run the Matterhorn components.

