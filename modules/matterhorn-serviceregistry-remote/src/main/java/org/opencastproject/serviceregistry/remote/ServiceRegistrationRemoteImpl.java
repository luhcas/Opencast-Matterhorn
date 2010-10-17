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

import org.opencastproject.serviceregistry.api.ServiceRegistration;

import static org.apache.commons.lang.StringUtils.isBlank;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 * A service registration that uses json parsing to marshall and unmarshall {@link ServiceRegistration}s.
 */
public class ServiceRegistrationRemoteImpl implements ServiceRegistration {

  public static final String SERVICE_PROPERTY = "service";
  public static final String SERVICE_TYPE_PROPERTY = "serviceType";
  public static final String HOST_PROPERTY = "host";
  public static final String PATH_PROPERTY = "path";
  public static final String ONLINE_PROPERTY = "online";
  public static final String MAINTENANCE_PROPERTY = "maintenance";
  public static final String JOB_PRODUCER_PROPERTY = "jobProducer";
  
  protected String serviceType  = null;
  protected String host  = null;
  protected String path  = null;
  protected boolean online = false;
  protected boolean maintenance = false;
  protected boolean jobProducer = false;
  
  public ServiceRegistrationRemoteImpl(String jsonString) {
    JSONObject json = (JSONObject)JSONValue.parse(jsonString);
    JSONObject jsonService = (JSONObject)json.get(SERVICE_PROPERTY);
    this.serviceType = (String)jsonService.get(SERVICE_TYPE_PROPERTY);
    this.host = (String)jsonService.get(HOST_PROPERTY);
    this.path = (String)jsonService.get(PATH_PROPERTY);
    if(isBlank(serviceType) || isBlank(host) || isBlank(path)) {
      throw new IllegalArgumentException("JSON must specify non-empty values for service type, host, and path");
    }
    this.online = Boolean.parseBoolean((String)jsonService.get(ONLINE_PROPERTY));
    this.maintenance = Boolean.parseBoolean((String)jsonService.get(MAINTENANCE_PROPERTY));
    this.jobProducer = Boolean.parseBoolean((String)jsonService.get(JOB_PRODUCER_PROPERTY));
  }
  
  @SuppressWarnings("unchecked")
  public String toJson() {
    JSONObject json = new JSONObject();
    json.put(SERVICE_TYPE_PROPERTY, this.serviceType);
    json.put(HOST_PROPERTY, this.host);
    json.put(PATH_PROPERTY, this.path);
    json.put(ONLINE_PROPERTY, this.online);
    json.put(MAINTENANCE_PROPERTY, this.maintenance);
    json.put(JOB_PRODUCER_PROPERTY, this.jobProducer);
    return json.toJSONString();
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.serviceregistry.api.ServiceRegistration#getServiceType()
   */
  @Override
  public String getServiceType() {
    return serviceType;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.serviceregistry.api.ServiceRegistration#getHost()
   */
  @Override
  public String getHost() {
    return host;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.serviceregistry.api.ServiceRegistration#getPath()
   */
  @Override
  public String getPath() {
    return path;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.serviceregistry.api.ServiceRegistration#isJobProducer()
   */
  @Override
  public boolean isJobProducer() {
    return jobProducer;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.serviceregistry.api.ServiceRegistration#isOnline()
   */
  @Override
  public boolean isOnline() {
    return online;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.serviceregistry.api.ServiceRegistration#isInMaintenanceMode()
   */
  @Override
  public boolean isInMaintenanceMode() {
    return maintenance;
  }

}
