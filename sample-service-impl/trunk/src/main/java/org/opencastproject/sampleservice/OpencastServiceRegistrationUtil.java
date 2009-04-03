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
package org.opencastproject.sampleservice;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.util.Dictionary;
import java.util.Hashtable;

public class OpencastServiceRegistrationUtil {
  /**
   * Registers an OSGI service, simultaneously exposing it as a webservice
   * endpoint. Use this to avoid duplicating boilerplate code.
   * 
   * TODO Move this to a utilities bundle ???
   * 
   * @param context
   *          The bundle context of this service
   * @param serviceImpl
   *          The implementation of the service
   * @param serviceInterface
   *          The interface for which this service is to be registered
   * @param webServicePath
   *          The path to the service's endpoint
   * @return The {@link ServiceRegistration}
   */
  public static ServiceRegistration register(BundleContext context,
      Object serviceImpl, Class<?> serviceInterface, String webServicePath) {
    Dictionary<String, String> props = new Hashtable<String, String>();
    props.put("osgi.remote.interfaces", "*");
    props.put("osgi.remote.requires.intents", "SOAP.1_2");
    props.put("osgi.remote.configuration.type", "pojo");
    props.put("osgi.remote.configuration.pojo.httpservice.context",
        webServicePath);
    return context.registerService(serviceInterface.getName(), serviceImpl,
        props);
  }
}
