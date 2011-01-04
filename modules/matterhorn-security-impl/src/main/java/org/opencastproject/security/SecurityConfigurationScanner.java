/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.security;

import org.opencastproject.rest.RestConstants;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.osgi.context.ConfigurableOsgiBundleApplicationContext;
import org.springframework.osgi.context.support.OsgiBundleXmlApplicationContext;

import java.io.File;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.Filter;

/**
 * Scans for the security.xml file in the configuration directory.
 */
public class SecurityConfigurationScanner {
  public static final String SECURITY_CONFIG_FILE = "org.opencastproject.security.config";
  private static final Logger logger = LoggerFactory.getLogger(SecurityConfigurationScanner.class);
  protected ConfigurableOsgiBundleApplicationContext springContext;
  protected BundleContext bundleContext;
  protected ServiceRegistration reg;
  protected Timer timer;
  protected File securityConfig;
  protected long lastModified = -1;
  protected boolean warned = false;

  public void activate(ComponentContext cc) {
    this.bundleContext = cc.getBundleContext();
    this.securityConfig = getSecurityConfig();
    if (!securityConfig.exists() || !securityConfig.canRead()) {
      // TODO: This should be an illegal state, but allow it to pass for now
      // throw new IllegalStateException("can not read security configuraiton at " +
      // this.securityConfig.getAbsolutePath());
    }
    this.timer = new Timer(SECURITY_CONFIG_FILE, true);
    timer.schedule(new ScannerTask(), new Date(), 5000); // poll for configuration changes, starting now
  }

  public void deactivate() {
    timer.cancel();
    if (reg != null) {
      reg.unregister();
      reg = null;
    }
    if (springContext != null && springContext.isRunning()) {
      springContext.close();
    }
  }

  protected File getSecurityConfig() {
    return new File(bundleContext.getProperty(SECURITY_CONFIG_FILE));
  }

  class ScannerTask extends TimerTask {
    /**
     * {@inheritDoc}
     * 
     * @see java.util.TimerTask#run()
     */
    @SuppressWarnings("unchecked")
    @Override
    public void run() {
      // If the file modification date has been set but it's last modified hasn't changed, there is nothing to do
      securityConfig = getSecurityConfig();
      if (!securityConfig.exists() || !securityConfig.canRead()) {
        if (!warned) {
          logger.error("Unable to read security configuration file at " + securityConfig.getAbsolutePath());
          // also print a stack trace so it's very obvious that there's something wrong
          try {
            throw new IllegalStateException("Unable to read security configuration file at "
                    + securityConfig.getAbsolutePath());
          } catch (IllegalStateException e) {
            e.printStackTrace();
          }
          warned = true;
        }
        return;
      }

      if (lastModified != -1 && lastModified == securityConfig.lastModified()) {
        logger.debug("security configuration is up to date");
        return;
      }
      logger.info("Updating the security configuration");

      // This is the first access to the file, or the file has changed. Update the lastModified time and (re)load.
      lastModified = securityConfig.lastModified();

      try {
        if (springContext == null) {
          springContext = new OsgiBundleXmlApplicationContext(
                  new String[] { "file:" + securityConfig.getAbsolutePath() });
          springContext.setBundleContext(bundleContext);
          logger.info("registered {}", springContext);
        }
        // Refresh the spring application context
        springContext.refresh();

        // Register the filter as an osgi service, unregistering the previous version if it has already been registered
        @SuppressWarnings("rawtypes")
        Dictionary props = new Hashtable<String, Boolean>();
        props.put("contextId", RestConstants.HTTP_CONTEXT_ID);
        props.put("pattern", ".*");
        props.put("service.ranking", "1");
        if (reg != null) {
          reg.unregister();
          reg = null;
        }
        reg = bundleContext.registerService(Filter.class.getName(), springContext.getBean("springSecurityFilterChain"),
                props);

        // Reset the warning, in case we remove this successfully configured spring filter chain
        warned = false;
      } catch (Exception e) {
        // If we throw an exception, the scanner thread will stop. Instead, just log the problem.
        logger.warn("Unable to update the spring security configuration", e);
      }
    }
  }
}
