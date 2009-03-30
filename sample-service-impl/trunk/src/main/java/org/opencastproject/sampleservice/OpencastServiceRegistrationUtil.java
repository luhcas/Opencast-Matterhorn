package org.opencastproject.sampleservice;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class OpencastServiceRegistrationUtil {
  /**
   * Registers an OSGI service, simultaneously exposing it as a webservice
   * endpoint. Use this to avoid duplicating boilerplate code.
   * 
   * TODO Move this to a utilities bundle ???
   * 
   * @param context
   *            The bundle context of this service
   * @param serviceImpl
   *            The implementation of the service
   * @param serviceInterface
   *            The interface for which this service is to be registered
   * @param webServicePath
   *            The path to the service's endpoint
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
