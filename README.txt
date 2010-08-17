---------------------------
 Installation instructions:
---------------------------

For the impatient, experienced, and brave, here's the short version:

1) Install an OSGi container, e. g. felix, equinox etc.
2) Install the 3rd party tools using the platform appropriate script in docs/scripts/3rd_party_tools
3) Run "mvn install -DdeployTo=<path to your osgi container's load directory>"
4) Start your OSGi container and navigate to http://localhost:8080

Detailed instructions for a variety of platforms and topologies are available at http://opencast.jira.com/wiki/display/MH/1.0+August+2010

Questions should go to the Matterhorn list (matterhorn@opencastproject.org) or the #opencast IRC channel at irc.freenode.net
