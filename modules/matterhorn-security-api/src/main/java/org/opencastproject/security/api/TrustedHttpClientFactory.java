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
package org.opencastproject.security.api;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.protocol.HttpContext;

/**
 * Provides secured http client components to access to protected resources.
 */
public interface TrustedHttpClientFactory {

  /**
   * Gets an {@link HttpClient} pre-configured for trusted communication.
   * @return
   */
  HttpClient getTrustedHttpClient(String url) throws TrustedHttpClientException;
  
  /**
   * Gets an {@link HttpClientAndContext} with an {@link HttpClient} and {@link HttpContext} pre-configured for
   * trusted communication.  When doing HttpPosts, HttpClient requires the use of an HttpContext containing the
   * nonce and realm
   * @return
   */
  HttpClientAndContext getTrustedHttpClientAndContext(String url) throws TrustedHttpClientException;

  /**
   * Returns an {@link HttpGet} configured for trusted communication
   * @param url The URL
   * @return
   */
  HttpGet getTrustedHttpGet(String url);

  /**
   * Returns an {@link HttpPost} configured for trusted communication
   * @param url The URL
   * @return
   */
  HttpPost getTrustedHttpPost(String url);

  /**
   * Returns an {@link HttpPut} configured for trusted communication
   * @param url The URL
   * @return
   */
  HttpPut getTrustedHttpPut(String url);

  /**
   * Returns an {@link HttpDelete} configured for trusted communication
   * @param url The URL
   * @return
   */
  HttpDelete getTrustedHttpDelete(String url);

  /**
   * Returns an {@link HttpHead} configured for trusted communication
   * @param url The URL
   * @return
   */
  HttpHead getTrustedHttpHead(String url);

  /**
   * A tuple of an HttpClient and an HttpContext.  Be sure to use the HttpContext in calls to
   * {@link HttpClient#execute(org.apache.http.client.methods.HttpUriRequest, HttpContext)}
   */
  class HttpClientAndContext {
    private HttpClient httpClient;
    private HttpContext httpContext;
    public HttpClientAndContext(HttpClient httpClient, HttpContext httpContext) {
      this.httpClient = httpClient;
      this.httpContext = httpContext;
    }
    /**
     * @return the httpClient
     */
    public HttpClient getHttpClient() {
      return httpClient;
    }
    /**
     * @return the httpContext
     */
    public HttpContext getHttpContext() {
      return httpContext;
    }
    
  }
}
