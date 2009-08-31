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
package org.opencastproject.media.mediapackage;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

/**
 * Registers services related to {@link MediaPackage}s, including a JAX-RS {@link MessageBodyReader} and
 * {@link MessageBodyWriter}.
 */
public class Activator implements BundleActivator{
  private static final Logger logger = LoggerFactory.getLogger(Activator.class);
  
  protected List<ServiceRegistration> readersAndWriters = new ArrayList<ServiceRegistration>();
  
  /**
   * {@inheritDoc}
   * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
   */
  public void start(BundleContext context) throws Exception {
    logger.info("starting");

    // Register a Media Package Reader / Writer
    MediaPackageJaxRsReaderWriter mediaPackageReaderWriter = new MediaPackageJaxRsReaderWriter();
    readersAndWriters.add(context.registerService(MessageBodyReader.class.getName(), mediaPackageReaderWriter, null));
    readersAndWriters.add(context.registerService(MessageBodyWriter.class.getName(), mediaPackageReaderWriter, null));

    // Register a Media Package Element Reader / Writer
    MediaPackageElementJaxRsReaderWriter elementReaderWriter = new MediaPackageElementJaxRsReaderWriter();
    readersAndWriters.add(context.registerService(MessageBodyReader.class.getName(), elementReaderWriter, null));
    readersAndWriters.add(context.registerService(MessageBodyWriter.class.getName(), elementReaderWriter, null));
  }

  /**
   * {@inheritDoc}
   * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
   */
  public void stop(BundleContext context) throws Exception {
    logger.info("stopping");
    for(ServiceRegistration reg: readersAndWriters) {
      reg.unregister();
    }
  }
}
