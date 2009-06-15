/**
 *  Copyright 2009 Opencast Project (http://www.opencastproject.org)
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
package org.opencastproject.encoder;

import org.opencastproject.encoder.api.EncoderService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.util.Dictionary;
import java.util.Hashtable;

public class OsgiActivator implements BundleActivator {
  protected ServiceRegistration encoderServiceRegistration;

  public void start(BundleContext context) throws Exception {
    EncoderServiceImpl encoderServiceImpl = new EncoderServiceImpl();
    
    Dictionary<String, String> props = new Hashtable<String, String>();
    props.put("osgi.remote.interfaces", "*");
    props.put("osgi.remote.requires.intents", "SOAP.1_2");
    props.put("osgi.remote.configuration.type", "pojo");
    props.put("osgi.remote.configuration.pojo.httpservice.context",
        "/encoder");
    props.put("org.apache.cxf.dosgi.databinding", "jaxb");
    props.put("org.apache.cxf.dosgi.frontend", "jaxws");
    encoderServiceRegistration = context.registerService(EncoderService.class.getName(),
        encoderServiceImpl, props);
  }

  public void stop(BundleContext context) throws Exception {
    encoderServiceRegistration.unregister();
  }
}
