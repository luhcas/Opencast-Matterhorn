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
package org.opencastproject.remote.api;

import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.util.UrlSupport;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpRequestBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class serving as a convenience implementation for remote services.
 */
public class RemoteBase {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(RemoteBase.class);

  /** The service type, used to look up remote implementations */
  protected String serviceType = null;

  /** The http client to use when connecting to remote servers */
  protected TrustedHttpClient client = null;

  /** the http client */
  protected RemoteServiceManager remoteServiceManager = null;

  /**
   * Creates a remote implementation for the given type of service.
   * 
   * @param type
   *          the service type
   */
  protected RemoteBase(String type) {
    if (type == null)
      throw new IllegalArgumentException("Service type must not be null");
    this.serviceType = type;
  }

  /**
   * Sets the trusted http client
   * 
   * @param client
   */
  public void setTrustedHttpClient(TrustedHttpClient client) {
    this.client = client;
  }

  /**
   * Sets the remote service manager.
   * 
   * @param remoteServiceManager
   */
  public void setRemoteServiceManager(RemoteServiceManager remoteServiceManager) {
    this.remoteServiceManager = remoteServiceManager;
  }

  /**
   * Makes a request to all available remote services and returns the response as soon as the first of them returns the
   * {@link HttpStatus.SC_OK} as the status code.
   * 
   * @param httpRequest
   *          the http request
   * @return the respomse object
   */
  protected HttpResponse getResponse(HttpRequestBase httpRequest) {
    return getResponse(httpRequest, HttpStatus.SC_OK);
  }

  /**
   * Makes a request to all available remote services and returns the response as soon as the first of them returns the
   * excepcted http status code.
   * 
   * @param httpRequest
   *          the http request
   * @param expectedHttpStatus
   *          any expected status codes to include in the return.
   * @return the respomse object
   */
  protected HttpResponse getResponse(HttpRequestBase httpRequest, Integer... expectedHttpStatus) {
    List<String> remoteHosts = remoteServiceManager.getRemoteHosts(serviceType);
    Map<String, String> hostErrors = new HashMap<String, String>();
    for (String remoteHost : remoteHosts) {
      try {
        URI uri = new URI(UrlSupport.concat(remoteHost, httpRequest.getURI().toString()));
        httpRequest.setURI(uri);
        HttpResponse response = client.execute(httpRequest);
        StatusLine status = response.getStatusLine();
        if (Arrays.asList(expectedHttpStatus).contains(status.getStatusCode())) {
          return response;
        } else {
          hostErrors.put(remoteHost, status.toString());
        }
      } catch (Exception e) {
        hostErrors.put(remoteHost, e.getMessage());
      }
    }
    logger.warn(hostErrors.toString());
    return null;
  }

}
