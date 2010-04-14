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

import org.opencastproject.security.api.TrustedHttpClientException;
import org.opencastproject.security.api.TrustedHttpClientFactory;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 
 */
public class TrustedHttpClientFactoryImpl implements TrustedHttpClientFactory {
  private static final Logger logger = LoggerFactory.getLogger(TrustedHttpClientFactoryImpl.class);
  
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
  
  public TrustedHttpClientFactoryImpl() {}
  
  public TrustedHttpClientFactoryImpl(String user, String pass) {
    this.user = user;
    this.pass = pass;
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.security.api.TrustedHttpClientFactory#getTrustedHttpClientAndContext()
   */
  @Override
  public HttpClientAndContext getTrustedHttpClientAndContext(String url) throws TrustedHttpClientException {
    try {
      DefaultHttpClient httpClient = new DefaultHttpClient();
      
      // Perform a HEAD, and extract the realm and nonce
      HttpHead head = new HttpHead(url);
      head.addHeader(REQUESTED_AUTH_HEADER, DIGEST_AUTH);
      HttpResponse headResponse = httpClient.execute(head);
      Header[] headers = headResponse.getHeaders("WWW-Authenticate");
      if(headers == null || headers.length == 0) {
        throw new IllegalStateException(url + " does not support digest authentication");
      }
      Header authHeader = headers[0];
      String nonce = null;
      String realm = null;
      for(HeaderElement element : authHeader.getElements()) {
        if("nonce".equals(element.getName())) {
          nonce = element.getValue();
        } else if("Digest realm".equals(element.getName())) {
          realm = element.getValue();
        }
      }

      // Configure the context with the realm and nonce we discovered in the previous head request
      HttpContext localContext = new BasicHttpContext();
      DigestScheme digestAuth = new DigestScheme();
      digestAuth.overrideParamter("realm", realm);
      digestAuth.overrideParamter("nonce", nonce);        
      localContext.setAttribute("preemptive-auth", digestAuth);

      return new HttpClientAndContext(getTrustedHttpClient(url), localContext);
    } catch(IOException e) {
      throw new TrustedHttpClientException(e);
    }
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.security.api.TrustedHttpClientFactory#getTrustedHttpClient()
   */
  @Override
  public HttpClient getTrustedHttpClient(String url) {
    UsernamePasswordCredentials creds = new UsernamePasswordCredentials(user, pass);
    DefaultHttpClient httpClient = new DefaultHttpClient();
    httpClient.getCredentialsProvider().setCredentials(AuthScope.ANY, creds);
    return httpClient;
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.security.api.TrustedHttpClientFactory#getTrustedHttpGet(java.lang.String)
   */
  @Override
  public HttpGet getTrustedHttpGet(String url) {
    HttpGet get = new HttpGet(url);
    get.addHeader("X-Requested-Auth", "Digest");
    return get;
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.security.api.TrustedHttpClientFactory#getTrustedHttpHead(java.lang.String)
   */
  @Override
  public HttpHead getTrustedHttpHead(String url) {
    HttpHead head = new HttpHead(url);
    head.addHeader("X-Requested-Auth", "Digest");
    return head;
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.security.api.TrustedHttpClientFactory#getTrustedHttpPost(java.lang.String)
   */
  @Override
  public HttpPost getTrustedHttpPost(String url) {
    HttpPost post = new HttpPost(url);
    post.addHeader("X-Requested-Auth", "Digest");
    return post;
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.security.api.TrustedHttpClientFactory#getTrustedHttpDelete(java.lang.String)
   */
  @Override
  public HttpDelete getTrustedHttpDelete(String url) {
    HttpDelete delete = new HttpDelete(url);
    delete.addHeader("X-Requested-Auth", "Digest");
    return delete;
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.security.api.TrustedHttpClientFactory#getTrustedHttpPut(java.lang.String)
   */
  @Override
  public HttpPut getTrustedHttpPut(String url) {
    HttpPut put = new HttpPut(url);
    put.addHeader("X-Requested-Auth", "Digest");
    return put;
  }
}
