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
package org.opencastproject.security;

import static org.opencastproject.security.DelegatingAuthenticationEntryPoint.DIGEST_AUTH;
import static org.opencastproject.security.DelegatingAuthenticationEntryPoint.REQUESTED_AUTH_HEADER;

import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.security.api.TrustedHttpClientException;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;

/**
 * An http client that executes secure (though not necessarily encrypted) http requests.
 */
public class TrustedHttpClientImpl implements TrustedHttpClient {
  private static final Logger logger = LoggerFactory.getLogger(TrustedHttpClientImpl.class);
  
  public static final String DIGEST_AUTH_USER_KEY = "org.opencastproject.security.digest.user";
  public static final String DIGEST_AUTH_PASS_KEY = "org.opencastproject.security.digest.pass";

  protected String user = null;
  protected String pass = null;
  
  public void activate(ComponentContext cc) {
    logger.debug("activate");
    user = cc.getBundleContext().getProperty(DIGEST_AUTH_USER_KEY);
    pass = cc.getBundleContext().getProperty(DIGEST_AUTH_PASS_KEY);
    if(user == null || pass == null) throw new IllegalStateException("trusted communication is not properly configured");
  }
  
  public void deactivate() {
    logger.debug("deactivate");
  }
  
  public TrustedHttpClientImpl() {}
  
  public TrustedHttpClientImpl(String user, String pass) {
    this.user = user;
    this.pass = pass;
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.security.api.TrustedHttpClient#execute(org.apache.http.client.methods.HttpUriRequest)
   */
  @Override
  public HttpResponse execute(HttpUriRequest httpUriRequest) {
    DefaultHttpClient httpClient = new DefaultHttpClient();

    // Add the request header to elicit a digest auth response
    httpUriRequest.addHeader(REQUESTED_AUTH_HEADER, DIGEST_AUTH);

    if("GET".equalsIgnoreCase(httpUriRequest.getMethod()) || "HEAD".equalsIgnoreCase(httpUriRequest.getMethod())) {
      // Set the user/pass
      UsernamePasswordCredentials creds = new UsernamePasswordCredentials(user, pass);
      httpClient.getCredentialsProvider().setCredentials(AuthScope.ANY, creds);

      // Run the request (the http client handles the multiple back-and-forth requests)
      try {
        return httpClient.execute(httpUriRequest);
      } catch (IOException e) {
        throw new TrustedHttpClientException(e);
      }
    }

    // HttpClient doesn't handle the request dynamics for other verbs (especially when sending a streamed multipart
    // request), so we need to handle the details of the digest auth back-and-forth manually
    String[] realmAndNonce = getRealmAndNonce(httpClient, httpUriRequest.getURI());
    
    if(realmAndNonce != null) {
      // Set the user/pass
      UsernamePasswordCredentials creds = new UsernamePasswordCredentials(user, pass);

      // Set up the digest authentication with the reqired values
      DigestScheme digestAuth = new DigestScheme();
      digestAuth.overrideParamter("realm", realmAndNonce[0]);
      digestAuth.overrideParamter("nonce", realmAndNonce[1]);        

      // Add the authentication header
      try {
        httpUriRequest.addHeader(digestAuth.authenticate(creds, httpUriRequest));
      } catch (Exception e) {
        throw new TrustedHttpClientException(e);
      }
    }
    try {
      return httpClient.execute(httpUriRequest);
    } catch (Exception e) {
      throw new TrustedHttpClientException(e);
    }

  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.security.api.TrustedHttpClient#execute(org.apache.http.client.methods.HttpUriRequest, org.apache.http.client.ResponseHandler)
   */
  @Override
  public <T> T execute(HttpUriRequest httpUriRequest, ResponseHandler<T> responseHandler) {
    try {
      return responseHandler.handleResponse(execute(httpUriRequest));
    } catch (IOException e) {
      throw new TrustedHttpClientException(e);
    }
  }

  /**
   * Perform a HEAD request to the URI, and extract the realm and nonce values
   * 
   * @param httpClient The client to use in executing the HEAD
   * @param uri The URI for the request
   * @return A String[] containing the {realm, nonce}
   */
  protected String[] getRealmAndNonce(HttpClient httpClient, URI uri) {
    HttpHead head = new HttpHead(uri);
    head.addHeader(REQUESTED_AUTH_HEADER, DIGEST_AUTH);
    HttpResponse headResponse;
    try {
      headResponse = httpClient.execute(head);
    } catch (IOException e) {
      throw new TrustedHttpClientException(e);
    }
    Header[] headers = headResponse.getHeaders("WWW-Authenticate");
    if(headers == null || headers.length == 0) {
      logger.warn("URI {} does not support digest authentication", uri);
      return null;
    }
    Header authRequiredResponseHeader = headers[0];
    String nonce = null;
    String realm = null;
    for(HeaderElement element : authRequiredResponseHeader.getElements()) {
      if("nonce".equals(element.getName())) {
        nonce = element.getValue();
      } else if("Digest realm".equals(element.getName())) {
        realm = element.getValue();
      }
    }
    return new String[] {realm, nonce};
  }
}
