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
package org.opencastproject.search.remote;

import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.search.api.SearchException;
import org.opencastproject.search.api.SearchQuery;
import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.api.SearchResultImpl;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.security.api.TrustedHttpClient;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.osgi.service.component.ComponentContext;

import java.util.ArrayList;
import java.util.List;

/**
 * A proxy to a remote search service.
 */
public class SearchServiceRemoteImpl implements SearchService {
  public static final String REMOTE_SEARCH = "remote.search";
  
  protected String remoteHost;
  protected TrustedHttpClient trustedHttpClient;

  public void setRemoteHost(String remoteHost) {
    this.remoteHost = remoteHost;
  }

  public void setTrustedHttpClient(TrustedHttpClient trustedHttpClient) {
    this.trustedHttpClient = trustedHttpClient;
  }
  
  public void activate(ComponentContext cc) {
    this.remoteHost = cc.getBundleContext().getProperty(REMOTE_SEARCH);
  }


  /**
   * {@inheritDoc}
   * @see org.opencastproject.search.api.SearchService#add(org.opencastproject.media.mediapackage.MediaPackage)
   */
  @Override
  public void add(MediaPackage mediaPackage) throws SearchException {
    try {
      HttpPost post = new HttpPost(remoteHost + "/search/rest/add");
      List<NameValuePair> params = new ArrayList<NameValuePair>();
      params.add(new BasicNameValuePair("mediapackage", mediaPackage.toXml()));
      UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);
      post.setEntity(entity);
      HttpResponse response = trustedHttpClient.execute(post);
      int status = response.getStatusLine().getStatusCode();
      if (status != HttpStatus.SC_NO_CONTENT) throw new RuntimeException("/add repsonse should have been HTTP 204 but was " + status);
    } catch (Exception e) {
      throw new SearchException(e);
    }
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.search.api.SearchService#clear()
   */
  @Override
  public void clear() throws SearchException {
    HttpPost post = new HttpPost(remoteHost + "/search/rest/clear");
    try {
      HttpResponse response = trustedHttpClient.execute(post);
      int status = response.getStatusLine().getStatusCode();
      if (status != HttpStatus.SC_NO_CONTENT) throw new RuntimeException("/clear repsonse should have been HTTP 204 but was " + status);
    } catch (Exception e) {
      throw new SearchException(e);
    }
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.search.api.SearchService#delete(java.lang.String)
   */
  @Override
  public void delete(String mediaPackageId) throws SearchException {
    String url = remoteHost + "/search/rest/" + mediaPackageId;
    HttpDelete del = new HttpDelete(url);
    try {
      HttpResponse response = trustedHttpClient.execute(del);
      int status = response.getStatusLine().getStatusCode();
      if (status != HttpStatus.SC_NO_CONTENT) throw new RuntimeException("/delete repsonse should have been HTTP 204 but was " + status);
    } catch (Exception e) {
      throw new SearchException(e);
    }
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.search.api.SearchService#getByQuery(org.opencastproject.search.api.SearchQuery)
   */
  @Override
  public SearchResult getByQuery(SearchQuery q) throws SearchException {
    StringBuilder url = new StringBuilder(remoteHost);
    List<NameValuePair> queryStringParams = new ArrayList<NameValuePair>();
    if(q.getText() != null) {
      queryStringParams.add(new BasicNameValuePair("q", q.getText()));
    }
    queryStringParams.add(new BasicNameValuePair("limit", Integer.toString(q.getLimit())));
    queryStringParams.add(new BasicNameValuePair("offset", Integer.toString(q.getOffset())));
    if( ! q.isIncludeEpisodes() && q.isIncludeSeries()) {
      url.append("/search/rest/series");
    } else if(q.isIncludeEpisodes() && ! q.isIncludeSeries()) {
      url.append("/search/rest/episode");
    } else {
      url.append("/search/rest/");
    }
    url.append(URLEncodedUtils.format(queryStringParams, "UTF-8"));
    HttpGet get = new HttpGet(url.toString());
    try {
      return SearchResultImpl.valueOf(trustedHttpClient.execute(get).getEntity().getContent());
    } catch (Exception e) {
      throw new SearchException(e);
    }
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.search.api.SearchService#getByQuery(java.lang.String, int, int)
   */
  @Override
  public SearchResult getByQuery(String query, int limit, int offset) throws SearchException {
    String url = remoteHost + "/search/rest/lucene";
    List<NameValuePair> queryStringParams = new ArrayList<NameValuePair>();
    queryStringParams.add(new BasicNameValuePair("q", query));
    queryStringParams.add(new BasicNameValuePair("limit", Integer.toString(limit)));
    queryStringParams.add(new BasicNameValuePair("offset", Integer.toString(offset)));
    url += URLEncodedUtils.format(queryStringParams, "UTF-8");
    HttpGet get = new HttpGet(url.toString());
    try {
      return SearchResultImpl.valueOf(trustedHttpClient.execute(get).getEntity().getContent());
    } catch (Exception e) {
      throw new SearchException(e);
    }
  }
}
