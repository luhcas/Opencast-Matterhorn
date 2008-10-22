This is a demonstration of a clustered Jackrabbit JCR implementation.  The current configuration requires an oracle JCBC driver and oracle server, but these could be replaced with MySQL (configured to use InnoDB tables by default) or some other transactional database.

1) Configure jcr-client/src/main/resources/cluster-repository-1.xml and jcr-client/src/main/resources/cluster-repository-2.xml.  These files are identical other than their node IDs.  (TODO: replace the node ID at runtime to avoid copying this file for each component).

2) I run the demonstration inside eclipse in order to configure the classpath automatically.  Run "mvn eclipse:eclipse" to generate the needed settings, then import the projects into eclipse.

3) Run 'mvn clean install' to generate the target directories (the demo uses these as scratch space for jackrabbit's settings files)

4) Run CommandLineJcrInterface.java as an application inside eclipse.  Enter '1' at the console prompt to indicate that this JVM will be node 1.  Run the application again to get another JVM running in parallel with the first.  Enter '2' at this prompt to indicate that this JVM will be node 2.  (The app only allows for 2 nodes... this is a proof-of-concept, afterall)

5) Try 'write foo' in one JVM, then switch to the other and 'read foo'.  You should see that even though the two JVMs each run their own JCR instance, they share a common data store.

