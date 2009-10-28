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

import org.apache.commons.io.FileUtils;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

  private BundleContext context;
  private SchedulerImpl sched;

  /**
   * {@inheritDoc}
   * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
   */
  public void start(BundleContext context) throws Exception {
    createTmpDirectory();
    ConfigurationManager.getInstance();
    this.context = context;
    //TODO:  Get default URI from properties file?
    //TODO:  Get the default polling time from properties file?
    sched = new SchedulerImpl();
    sched.init(getClass().getClassLoader().getResource("Matterhorn-Example.ics"), 5);
  }

  /**
   * {@inheritDoc}
   * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
   */
  public void stop(BundleContext context) throws Exception {
    this.context = null;
  }
  
  /**
   * Create the tmp folder to store the recording.
   */
  private void createTmpDirectory() {
    String tmpPath = System.getProperty("java.io.tmpdir") + File.separator + 
                      "opencast" + File.separator + "capture";
    File f = new File(tmpPath);
    if (!f.exists()) {
      try {
        FileUtils.forceMkdir(f);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

}
