This is a proof-of-concept for an OSGI / Servicemix-based model for developing Matterhorn.

To install and run this software:

1) Install the latest Servicemix 4 software
2) Configure jcr/jcr-server/src/main/resources/cluster-repository-template.xml to point to your data store and transaction-capable database.
3) Build this software using maven2 (mvn -DskipTests install)
4) Start servicemix (cd servicemix_home; java -jar bin/servicemix[.bat])
5) Install the Matterhorn components you want to run.  Currently, the only useful components are the JCR Server and the Sample Component.  The file 'servicemix_opencast_installation.txt' contains the commands to install all of the required third party OSGI bundles as well as the Matterhorn components.
6) Visit http://localhost:8080/cxf/sample?wsdl to see the sample web service endpoint.
