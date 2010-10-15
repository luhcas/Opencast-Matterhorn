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
package org.opencastproject.serviceregistry.remote;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.serviceregistry.api.ServiceRegistration;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceStatistics;
import org.opencastproject.util.UrlSupport;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.osgi.framework.ServiceException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * This implementation of the remote service registry is able to provide the functionality specified by the api over
 * <code>HTTP</code> rather than by directly connecting to the database that is backing the service.
 * <p>
 * This means that it is suited to run inside protected environments as long as there is an implementation of the
 * service running somwhere that provides the matching communication endpoint, which is the case with the default
 * implementation at {@link org.opencastproject.serviceregistry.impl.ServiceRegistryJpaImpl}.
 * <p>
 * Other than with the other <code>-remote</code> implementations, this one needs to be configured to find it's
 * counterpart implementation. It may either point to a load balancer hiding a number of running instances or to one
 * specific instance.
 */
public class ServiceRegistryRemoteImpl implements ServiceRegistry {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(ServiceRegistryRemoteImpl.class);
  
  /** Configuration key for the service registry */
  public static final String OPT_SERVICE_REGISTRY_URL = "org.opencastproject.serviceregistry.url";

  /** The http client to use when connecting to remote servers */
  protected TrustedHttpClient client = null;

  /** Url of the actual service implementation */
  protected String serviceURL = null;

  /**
   * Callback for the OSGi environment that is called upon service activation.
   * 
   * @param context
   *          the component context
   */
  protected void activate(ComponentContext context) {
    String serviceURLProperty = StringUtils.trimToNull((String)context.getProperties().get(OPT_SERVICE_REGISTRY_URL));
    if (serviceURLProperty == null)
      throw new ServiceException("Remote service registry can't find " + OPT_SERVICE_REGISTRY_URL);
    try {
      serviceURL = new URL(serviceURLProperty).toExternalForm();
    } catch (MalformedURLException e) {
      throw new ServiceException(OPT_SERVICE_REGISTRY_URL + " is malformed: " + serviceURLProperty);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#registerService(java.lang.String, java.lang.String,
   *      java.lang.String)
   */
  @Override
  public ServiceRegistration registerService(String serviceType, String host, String path) {
    return registerService(serviceType, host, path, false);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#registerService(java.lang.String, java.lang.String,
   *      java.lang.String, boolean)
   */
  @Override
  public ServiceRegistration registerService(String serviceType, String host, String path, boolean jobProducer) {
    String servicePath = "register";
    HttpPost post = new HttpPost(UrlSupport.concat(serviceURL, servicePath));
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("serviceType", serviceType));
      params.add(new BasicNameValuePair("host", host));
      params.add(new BasicNameValuePair("path", path));
      UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);
      post.setEntity(entity);
      HttpResponse response = null;
    } catch(UnsupportedEncodingException e) {
       // TODO: Do something meaningful
    }
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#unRegisterService(java.lang.String, java.lang.String,
   *      java.lang.String)
   */
  @Override
  public void unRegisterService(String serviceType, String host, String path) {
    // TODO Auto-generated method stub

  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#setMaintenanceStatus(java.lang.String,
   *      java.lang.String, boolean)
   */
  @Override
  public void setMaintenanceStatus(String serviceType, String host, boolean maintenance) throws IllegalStateException {
    // TODO Auto-generated method stub

  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#parseJob(java.lang.String)
   */
  @Override
  public Job parseJob(String xml) throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#parseJob(java.io.InputStream)
   */
  @Override
  public Job parseJob(InputStream in) throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#createJob(java.lang.String)
   */
  @Override
  public Job createJob(String type) {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#updateJob(org.opencastproject.job.api.Job)
   */
  @Override
  public void updateJob(Job job) {
    // TODO Auto-generated method stub

  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getJob(java.lang.String)
   */
  @Override
  public Job getJob(String id) {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getServiceRegistrations(java.lang.String)
   */
  @Override
  public List<ServiceRegistration> getServiceRegistrations(String serviceType) {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getServiceRegistration(java.lang.String,
   *      java.lang.String)
   */
  @Override
  public ServiceRegistration getServiceRegistration(String serviceType, String host) {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getServiceRegistrations()
   */
  @Override
  public List<ServiceRegistration> getServiceRegistrations() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getServiceStatistics()
   */
  @Override
  public List<ServiceStatistics> getServiceStatistics() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#count(java.lang.String,
   *      org.opencastproject.job.api.Job.Status)
   */
  @Override
  public long count(String serviceType, Status status) {
    // TODO Auto-generated method stub
    return 0;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#count(java.lang.String,
   *      org.opencastproject.job.api.Job.Status, java.lang.String)
   */
  @Override
  public long count(String serviceType, Status status, String host) {
    // TODO Auto-generated method stub
    return 0;
  }

  /**
   * Sets the trusted http client.
   * 
   * @param client
   *          the trusted http client
   */
  protected void setTrustedHttpClient(TrustedHttpClient client) {
    this.client = client;
  }

}
