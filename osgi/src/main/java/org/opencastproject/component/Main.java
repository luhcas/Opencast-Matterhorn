package org.opencastproject.component;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.felix.framework.Felix;
import org.apache.felix.framework.cache.BundleCache;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.framework.util.StringMap;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.startlevel.StartLevel;

public class Main implements BundleActivator {

	private static final String REPO = "file:/Users/josh/.m2/repository";
	private static final String REPO_FELIX = REPO + "/org/apache/felix";
	private static final String REPO_SLING = REPO + "/org/apache/sling";
	private static final String REPO_SERVICEMIX = REPO + "/org/apache/servicemix";
	private static final String REPO_CXF = REPO + "/org/apache/cxf";
	private static final String REPO_JR = REPO + "/org/apache/jackrabbit";
	private static final String REPO_OC = REPO + "/org/opencastproject";

	private BundleContext context;

	@SuppressWarnings("unchecked")
	public static void main(String... args) throws Exception {
		// Set up any system properties
		System.setProperty("org.apache.cxf.bus.factory", "org.apache.cxf.bus.CXFBusFactory");
		
		final File cachedir = File.createTempFile("octmp", null);
		cachedir.delete();
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				deleteFileOrDir(cachedir);
			}
		});
		StringMap configMap = new StringMap(false);
		configMap.put(Constants.FRAMEWORK_SYSTEMPACKAGES,
				"org.osgi.framework; version=1.3.0,"
				+ "org.osgi.service.packageadmin; version=1.2.0,"
				+ "org.osgi.service.startlevel; version=1.0.0,"
				+ "org.osgi.service.deploymentadmin; version=1.0.0,"
				+ "org.osgi.service.url; version=1.0.0,"
				+ "org.osgi.service.http; version=1.2.0,"
				+ "org.osgi.service.prefs; version=1.1.0,"
				+ "org.osgi.util.tracker; version=1.3.2,"
				+ "org.omg.CosNaming; version=0.0.0,"
				+ "org.omg.CORBA; version=0.0.0,"
				+ "org.omg.CORBA.portable; version=0.0.0,"
				+ "org.omg.CORBA.TypeCodePackage; version=0.0.0,"
				+ "org.omg.PortableServer; version=0.0.0,"
				+ "org.omg.PortableServer.POAPackage; version=0.0.0,"
				+ "org.w3c.dom; version=0.0.0,"
				+ "org.w3c.dom.bootstrap; version=0.0.0,"
				+ "org.w3c.dom.ls; version=0.0.0,"
				+ "org.w3c.dom.events; version=0.0.0,"
				+ "org.w3c.dom.ranges; version=0.0.0,"
				+ "org.w3c.dom.traversal; version=0.0.0,"
				+ "org.xml.sax; version=0.0.0,"
				+ "org.xml.sax.ext; version=0.0.0,"
				+ "org.xml.sax.helpers; version=0.0.0,"
				+ "javax.activation; version=1.1.0,"
				+ "javax.annotation; version=1.0.0,"
				+ "javax.imageio; version=0.0.0,"
				+ "javax.imageio.stream; version=0.0.0,"
				+ "javax.imageio.metadata; version=0.0.0,"
				+ "javax.imageio.spi; version=0.0.0,"
				+ "javax.jws; version=2.0.0,"
				+ "javax.jws.soap; version=2.0.0,"
				+ "javax.mail; version=1.4.0,"
				+ "javax.mail.internet; version=1.4.0,"
				+ "javax.mail.util; version=1.4.0,"
				+ "javax.management; version=0.0.0,"
				+ "javax.management.modelmbean; version=0.0.0,"
				+ "javax.management.remote; version=0.0.0,"
				+ "javax.naming; version=0.0.0,"
				+ "javax.naming.spi; version=0.0.0,"
				+ "javax.naming.directory; version=0.0.0,"
				+ "javax.net; version=0.0.0,"
				+ "javax.net.ssl; version=0.0.0,"
				+ "javax.resource; version=0.0.0,"
				+ "javax.resource.spi; version=0.0.0,"
				+ "javax.resource.spi.endpoint; version=0.0.0,"
				+ "javax.resource.spi.security; version=0.0.0,"
				+ "javax.security.auth; version=0.0.0,"
				+ "javax.security.auth.spi; version=0.0.0,"
				+ "javax.security.auth.x500; version=0.0.0,"
				+ "javax.security.auth.login; version=0.0.0,"
				+ "javax.security.auth.callback; version=0.0.0,"
				+ "javax.security.cert; version=0.0.0,"
				+ "javax.servlet; version=2.5.0,"
				+ "javax.servlet.http; version=2.5.0,"
				+ "javax.servlet.resources; version=2.5.0,"
				+ "javax.sql; version=0.0.0,"
				+ "javax.sql.rowset; version=0.0.0,"
				+ "javax.sql.rowset.spi; version=0.0.0,"
				+ "javax.swing; version=0.0.0,"
				+ "javax.swing.text; version=0.0.0,"
				+ "javax.swing.text.rtf; version=0.0.0,"
				+ "javax.swing.tree; version=0.0.0,"
				+ "javax.swing.event; version=0.0.0,"
				+ "javax.swing.border; version=0.0.0,"
				+ "javax.swing.filechooser; version=0.0.0,"
				+ "javax.transaction.xa; version=0.0.0,"
				+ "javax.xml.datatype; version=0.0.0,"
				+ "javax.xml.namespace; version=0.0.0,"
				+ "javax.xml.transform; version=0.0.0,"
				+ "javax.xml.transform.dom; version=0.0.0,"
				+ "javax.xml.transform.sax; version=0.0.0,"
				+ "javax.xml.transform.stream; version=0.0.0,"
				+ "javax.xml.parsers; version=0.0.0,"
				+ "javax.xml.soap; version=0.0.0,"
				+ "javax.xml.stream; version=1.0.0,"
				+ "javax.xml.stream.events; version=1.0.0,"
				+ "javax.xml.stream.util; version=1.0.0,"
				+ "javax.xml.validation; version=0.0.0,"
				+ "javax.xml.xpath; version=0.0.0,"
				+ "javax.xml.ws; version=2.1.0,"
				+ "javax.xml.ws.wsaddressing; version=2.1.0,"
				+ "javax.xml.ws.handler; version=2.1.0,"
				+ "javax.xml.ws.handler.soap; version=2.1.0,"
				+ "javax.xml.ws.http; version=2.1.0,"
				+ "javax.xml.ws.spi; version=2.1.0,"
				+ "javax.xml.ws.soap; version=2.1.0,"
				+ "javax.ws.rs; version=1.0.0,"
				+ "javax.ws.rs.core; version=1.0.0,"
				+ "javax.ws.rs.ext; version=1.0.0,"
				+ "javax.wsdl; version=1.2.0,"
				+ "javax.wsdl.extensions; version=1.2.0,"
				+ "javax.wsdl.extensions.http; version=1.2.0,"
				+ "javax.wsdl.extensions.mime; version=1.2.0,"
				+ "javax.wsdl.extensions.schema; version=1.2.0,"
				+ "javax.wsdl.extensions.soap; version=1.2.0,"
				+ "javax.wsdl.extensions.soap12; version=1.2.0,"
				+ "javax.wsdl.factory; version=0.0.0,"
				+ "javax.wsdl.xml; version=1.2.0,"
				);

		// A space-separated set of bundles to install
		configMap.put("felix.auto.start.1",
			REPO + "/commons-collections/commons-collections/3.2.1/commons-collections-3.2.1.jar "
			+ REPO_FELIX + "/org.apache.felix.bundlerepository/1.0.3/org.apache.felix.bundlerepository-1.0.3.jar "
			+ REPO_FELIX + "/org.apache.felix.configadmin/1.0.4/org.apache.felix.configadmin-1.0.4.jar "
			+ REPO_FELIX + "/org.apache.felix.scr/1.0.5-SNAPSHOT/org.apache.felix.scr-1.0.5-SNAPSHOT.jar "
			+ REPO_FELIX + "/org.apache.felix.eventadmin/1.0.0/org.apache.felix.eventadmin-1.0.0.jar "
			+ REPO_FELIX + "/org.apache.felix.metatype/1.0.0/org.apache.felix.metatype-1.0.0.jar "
			+ REPO_FELIX + "/org.apache.felix.webconsole/1.2.0/org.apache.felix.webconsole-1.2.0.jar "
			+ REPO_CXF + "/cxf-bundle/2.2-SNAPSHOT/cxf-bundle-2.2-SNAPSHOT.jar "

			+ REPO_SERVICEMIX + "/bundles/org.apache.servicemix.bundles.jaxb-api-2.0/4.0-m1/org.apache.servicemix.bundles.jaxb-api-2.0-4.0-m1.jar "
			+ REPO_SERVICEMIX + "/bundles/org.apache.servicemix.bundles.asm/2.2.3_1/org.apache.servicemix.bundles.asm-2.2.3_1.jar "
			+ REPO_SERVICEMIX + "/bundles/org.apache.servicemix.bundles.xmlschema/1.4.2_1/org.apache.servicemix.bundles.xmlschema-1.4.2_1.jar "
			+ REPO_SERVICEMIX + "/bundles/org.apache.servicemix.bundles.xmlresolver/1.2_1/org.apache.servicemix.bundles.xmlresolver-1.2_1.jar "
			+ REPO_SERVICEMIX + "/bundles/org.apache.servicemix.bundles.neethi-2.0.2/4.0-m1/org.apache.servicemix.bundles.neethi-2.0.2-4.0-m1.jar "

			+ REPO_SLING + "/org.apache.sling.jcr.api/2.0.3-incubator-SNAPSHOT/org.apache.sling.jcr.api-2.0.3-incubator-SNAPSHOT.jar "
			+ REPO_SLING + "/org.apache.sling.launchpad.base/2.0.3-incubator-SNAPSHOT/org.apache.sling.launchpad.base-2.0.3-incubator-SNAPSHOT.jar "
			+ REPO_SLING + "/org.apache.sling.jcr.jackrabbit.api/2.0.3-incubator-SNAPSHOT/org.apache.sling.jcr.jackrabbit.api-2.0.3-incubator-SNAPSHOT.jar "
			+ REPO_SLING + "/org.apache.sling.commons.log/2.0.3-incubator-SNAPSHOT/org.apache.sling.commons.log-2.0.3-incubator-SNAPSHOT.jar "

			
			+ REPO_JR + "/jackrabbit-jcr-commons/1.4.2/jackrabbit-jcr-commons-1.4.2.jar "

			+ REPO_OC + "/opencast-jcr-server/0.1-SNAPSHOT/opencast-jcr-server-0.1-SNAPSHOT.jar "
			+ REPO_OC + "/opencast-sample-component/0.1-SNAPSHOT/opencast-sample-component-0.1-SNAPSHOT.jar "
		);

		configMap.put(FelixConstants.LOG_LEVEL_PROP, "1");
		configMap.put(BundleCache.CACHE_PROFILE_DIR_PROP, cachedir
				.getAbsolutePath());

		// Create list to hold custom framework activators.
		List list = new ArrayList();
		// Add activator to process auto-start/install properties.
		list.add(new AutoActivator(configMap));
		// Add our own activator.
		list.add(new Main());

		try {
			// Now create an instance of the framework.
			Felix felix = new Felix(configMap, list);
			felix.start();
		} catch (Exception ex) {
			System.err.println("Could not create framework: " + ex);
			ex.printStackTrace();
			System.exit(-1);
		}
	}

	private static void deleteFileOrDir(File file) {
		if (file.isDirectory()) {
			File[] childs = file.listFiles();
			for (int i = 0; i < childs.length; i++) {
				deleteFileOrDir(childs[i]);
			}
		}
		file.delete();
	}

	public void start(BundleContext context) {
		this.context = context;
	}

	public void stop(BundleContext context) throws Exception {
	}
}

/**
 * See org.apache.felix.main.AutoActivator
 */
class AutoActivator implements BundleActivator {
	public static final String AUTO_INSTALL_PROP = "felix.auto.install";
	public static final String AUTO_START_PROP = "felix.auto.start";

	StringMap configMap;

	AutoActivator(StringMap configMap) {
		this.configMap = configMap;
	}

	@SuppressWarnings("unchecked")
	public void start(BundleContext context) throws Exception {

		StartLevel sl = (StartLevel) context
				.getService(context
						.getServiceReference(org.osgi.service.startlevel.StartLevel.class
								.getName()));
		for (Iterator i = configMap.keySet().iterator(); i.hasNext();) {
			String key = ((String) i.next()).toLowerCase();

			// Ignore all keys that are not an auto property.
			if (!key.startsWith(AUTO_INSTALL_PROP)
					&& !key.startsWith(AUTO_START_PROP)) {
				continue;
			}

			// If the auto property does not have a start level,
			// then assume it is the default bundle start level, otherwise
			// parse the specified start level.
			int startLevel = sl.getInitialBundleStartLevel();
			if (!key.equals(AUTO_INSTALL_PROP) && !key.equals(AUTO_START_PROP)) {
				try {
					startLevel = Integer.parseInt(key.substring(key
							.lastIndexOf('.') + 1));
				} catch (NumberFormatException ex) {
					System.err.println("Invalid property: " + key);
				}
			}

			// Parse and install the bundles associated with the key.
			StringTokenizer st = new StringTokenizer((String) configMap
					.get(key), "\" ", true);
			for (String location = nextLocation(st); location != null; location = nextLocation(st)) {
				try {
					Bundle b = context.installBundle(location, null);
					sl.setBundleStartLevel(b, startLevel);
				} catch (Exception ex) {
					System.err.println("Auto-properties install: " + ex);
				}
			}
		}
		// Now loop through the auto-start bundles and start them.
		for (Iterator i = configMap.keySet().iterator(); i.hasNext();) {
			String key = ((String) i.next()).toLowerCase();
			if (key.startsWith(AUTO_START_PROP)) {
				StringTokenizer st = new StringTokenizer((String) configMap
						.get(key), "\" ", true);
				for (String location = nextLocation(st); location != null; location = nextLocation(st)) {
					// Installing twice just returns the same bundle.
					try {
						Bundle b = context.installBundle(location, null);
						if (b != null) {
							b.start();
						}
					} catch (Exception ex) {
						System.err.println("Auto-properties start: " + ex);
					}
				}
			}
		}

	}

	public void stop(BundleContext context) throws Exception {
	}

	private static String nextLocation(StringTokenizer st) {
		String retVal = null;

		if (st.countTokens() > 0) {
			String tokenList = "\" ";
			StringBuffer tokBuf = new StringBuffer(10);
			String tok = null;
			boolean inQuote = false;
			boolean tokStarted = false;
			boolean exit = false;
			while ((st.hasMoreTokens()) && (!exit)) {
				tok = st.nextToken(tokenList);
				if (tok.equals("\"")) {
					inQuote = !inQuote;
					if (inQuote) {
						tokenList = "\"";
					} else {
						tokenList = "\" ";
					}

				} else if (tok.equals(" ")) {
					if (tokStarted) {
						retVal = tokBuf.toString();
						tokStarted = false;
						tokBuf = new StringBuffer(10);
						exit = true;
					}
				} else {
					tokStarted = true;
					tokBuf.append(tok.trim());
				}
			}

			// Handle case where end of token stream and
			// still got data
			if ((!exit) && (tokStarted)) {
				retVal = tokBuf.toString();
			}
		}
		return retVal;
	}

}