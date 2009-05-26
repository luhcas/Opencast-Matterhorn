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
package org.opencastproject.authentication;

import org.jasig.cas.client.authentication.AuthenticationFilter;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import javax.servlet.Filter;

public class OsgiActivator implements BundleActivator {
  private static final Logger logger = LoggerFactory.getLogger(OsgiActivator.class);
  public static final String ENDPOINT_CONTEXT_KEY =
    "osgi.remote.configuration.pojo.httpservice.context";
  public static final String ENDPOINT_CONTEXT_FILTER = "(" + ENDPOINT_CONTEXT_KEY + "=*)";

  private String casServerLoginUrl;
  private String casService;
  private ServiceRegistration casFilterRegistration;
  private Set<String> protectedUrls = new HashSet<String>();
  
  public void start(BundleContext context) throws Exception {
    // Configure the CAS settings
    casServerLoginUrl = context.getProperty("casServerLoginUrl");
    casService = context.getProperty("casService");
    
    // For each matching service, add the service endpoint
    ServiceReference[] refs = context.getServiceReferences(null, ENDPOINT_CONTEXT_FILTER);
    if(refs != null) {
      for(int i=0; i<refs.length; i++) {
        addEndpoint(refs[i]);
      }
    }
    casFilterRegistration = registerFilter(context);

    
    // Watch for web service endpoints being published or removed, and update the authn filter 
    ServiceTracker webServiceEndpointTracker = new ServiceTracker(context,
        context.createFilter(ENDPOINT_CONTEXT_FILTER), null) {
      @Override
      public Object addingService(ServiceReference reference) {
        addEndpoint(reference);
        casFilterRegistration.unregister();
        casFilterRegistration = registerFilter(context);
        return super.addingService(reference);
      }
      @Override
      public void removedService(ServiceReference reference, Object service) {
        removeEndpoint(reference);
        casFilterRegistration.unregister();
        casFilterRegistration = registerFilter(context);
        super.removedService(reference, service);
      }
    };
    webServiceEndpointTracker.open();
  }
  public void stop(BundleContext arg0) throws Exception {
    casFilterRegistration.unregister();
  }
  private void addEndpoint(ServiceReference ref) {
    Object endPoint = ref.getProperty(ENDPOINT_CONTEXT_KEY);
    if(endPoint.getClass().isArray()) {
      protectedUrls.add(((String[])endPoint)[0]);
    } else {
      protectedUrls.add((String)endPoint);
    }
  }
  private void removeEndpoint(ServiceReference ref) {
    Object endPoint = ref.getProperty(ENDPOINT_CONTEXT_KEY);
    if(endPoint.getClass().isArray()) {
      protectedUrls.remove(((String[])endPoint)[0]);
    } else {
      protectedUrls.remove((String)endPoint);
    }
  }
  @SuppressWarnings("unchecked")
  private Dictionary getAuthenticationFilterProperties() {
    String[] urlArray = protectedUrls.toArray(new String[0]);
    Dictionary props = new Hashtable();
    props.put("filter-name", "Authentication Filter");
    props.put("urlPatterns", urlArray);
    props.put("service", casService);
    return props;
  }

  private ServiceRegistration registerFilter(BundleContext context) {
    AuthenticationFilter authenticationFilter = new AuthenticationFilter();
    authenticationFilter.setIgnoreInitConfiguration(true);
    authenticationFilter.setCasServerLoginUrl(casServerLoginUrl);
    authenticationFilter.setRenew(false);
    authenticationFilter.setGateway(false);
    authenticationFilter.setService(casService);
    authenticationFilter.setServerName(casService);
    
    if(logger.isInfoEnabled()) {
      StringBuilder sb = new StringBuilder("Protected URLs : ");
      for(String endpoint : protectedUrls) {
        sb.append(endpoint);
        sb.append(" ");
      }
      logger.info(sb.toString());
    }
    return context.registerService(Filter.class.getName(), authenticationFilter,
        getAuthenticationFilterProperties());
  }
}
