/**
 *  Copyright 2009 The Regents of the University of California
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
package org.opencastproject.capture.impl;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.opencastproject.capture.api.Scheduler;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator {

  /** Logging Facility */
  private static final Logger log = LoggerFactory.getLogger(Activator.class);
  private BundleContext context;
  private Scheduler sched;

  /**
   * {@inheritDoc}
   * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
   */
  public void start(BundleContext context) throws Exception {
    createCoreDirectories(ConfigurationManager.getInstance());
    this.context = context;
    sched = new SchedulerImpl();
    sched.init();
  }

  /**
   * {@inheritDoc}
   * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
   */
  public void stop(BundleContext context) throws Exception {
    this.context = null;
  }

  /**
   * Creates the core Opencast directories  
   */
  private void createCoreDirectories(ConfigurationManager config) {
    createFileObj(CaptureParameters.CAPTURE_FILESYSTEM_CONFIG_URL, config);
    createFileObj(CaptureParameters.CAPTURE_FILESYSTEM_CACHE_URL, config);
    createFileObj(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_URL, config);
    createFileObj(CaptureParameters.CAPTURE_FILESYSTEM_VOLATILE_URL, config);
  }

  /**
   * Creates a file or directory
   * @param key    The key to set in the configuration manager.  Key is set equal to name
   * @param config The configuration manager to store the key-value pair
   */
  private void createFileObj(String key, ConfigurationManager config) {
    try {
      File target = new File (config.getItem(key));
      try {
        FileUtils.forceMkdir(target);
        config.setItem(key, target.toString());
      } catch (IOException e) {
        log.error("Unable to create directory " + target.toString(), e);
      }
    } catch (NullPointerException e) {
      log.error("No value found for key " + key);
    }
  }
}
