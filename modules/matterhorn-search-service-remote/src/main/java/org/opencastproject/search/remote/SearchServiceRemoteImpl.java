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
import org.opencastproject.remote.api.RemoteServiceManager;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * A proxy to a remote search service.
 */
public class SearchServiceRemoteImpl implements SearchService {
  private static final Logger logger = LoggerFactory.getLogger(SearchServiceRemoteImpl.class);
  
  protected TrustedHttpClient trustedHttpClient;

  public void setTrustedHttpClient(TrustedHttpClient trustedHttpClient) {
    this.trustedHttpClient = trustedHttpClient;
  }
  
  protected RemoteServiceManager remoteServiceManager;
  public void setRemoteServiceManager(RemoteServiceManager remoteServiceManager) {
    this.remoteServiceManager = remoteServiceManager;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.search.api.SearchService#add(org.opencastproject.media.mediapackage.MediaPackage)
   */
  @Override
  public void add(MediaPackage mediaPackage) throws SearchException {
    List<String> remoteHosts = remoteServiceManager.getRemoteHosts(JOB_TYPE);
    for(String remoteHost : remoteHosts) {
      try {
        HttpPost post = new HttpPost(remoteHost + "/search/rest/add");
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("mediapackage", mediaPackage.toXml()));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);
        post.setEntity(entity);
        HttpResponse response = trustedHttpClient.execute(post);
        int status = response.getStatusLine().getStatusCode();
        if (status == HttpStatus.SC_NO_CONTENT) {
          logger.debug("Successfully added mediapackage {} to search service at {}", mediaPackage, remoteHost);
          return;
        } else {
          logger.info("/add repsonse should have been HTTP 204 but was " + status);
          continue;
        }
      } catch (Exception e) {
        logger.debug(e.getMessage(), e);
        continue;
      }
    }
    throw new SearchException("Unable to add mediapackage " + mediaPackage + " to any of " + remoteHosts.size() + " remote search services");
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.search.api.SearchService#clear()
   */
  @Override
  public void clear() throws SearchException {
    List<String> remoteHosts = remoteServiceManager.getRemoteHosts(JOB_TYPE);
    for(String remoteHost : remoteHosts) {
      try {
        HttpPost post = new HttpPost(remoteHost + "/search/rest/clear");
        HttpResponse response = trustedHttpClient.execute(post);
        int status = response.getStatusLine().getStatusCode();
        if (status == HttpStatus.SC_NO_CONTENT) {
          logger.debug("Successfully cleared search index at {}", remoteHost);
          return;
        } else {
          logger.info("/clear repsonse should have been HTTP 204 but was " + status);
          continue;
        }
      } catch (Exception e) {
        logger.debug(e.getMessage(), e);
        continue;
      }
    }
    throw new SearchException("Unable to clear search index at any of " + remoteHosts.size() + " remote search services");
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.search.api.SearchService#delete(java.lang.String)
   */
  @Override
  public void delete(String mediaPackageId) throws SearchException {
    List<String> remoteHosts = remoteServiceManager.getRemoteHosts(JOB_TYPE);
    for(String remoteHost : remoteHosts) {
      try {
        String url = remoteHost + "/search/rest/" + mediaPackageId;
        HttpDelete del = new HttpDelete(url);
        HttpResponse response = trustedHttpClient.execute(del);
        int status = response.getStatusLine().getStatusCode();
        if (status == HttpStatus.SC_NO_CONTENT) {
          logger.debug("Successfully deleted {} from the search index at {}", mediaPackageId, remoteHost);
          return;
        } else {
          logger.info("/delete repsonse should have been HTTP 204 but was " + status);
          continue;
        }
      } catch (Exception e) {
        logger.debug(e.getMessage(), e);
        continue;
      }
    }
    throw new SearchException("Unable to remove " + mediaPackageId + " from search index at any of " + remoteHosts.size() + " remote search services");
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.search.api.SearchService#getByQuery(org.opencastproject.search.api.SearchQuery)
   */
  @Override
  public SearchResult getByQuery(SearchQuery q) throws SearchException {
    List<String> remoteHosts = remoteServiceManager.getRemoteHosts(JOB_TYPE);
    for(String remoteHost : remoteHosts) {
      StringBuilder url = new StringBuilder(remoteHost);
      List<NameValuePair> queryStringParams = new ArrayList<NameValuePair>();
      if(q.getText() != null) {
        queryStringParams.add(new BasicNameValuePair("q", q.getText()));
      }
      queryStringParams.add(new BasicNameValuePair("limit", Integer.toString(q.getLimit())));
      queryStringParams.add(new BasicNameValuePair("offset", Integer.toString(q.getOffset())));
      if( ! q.isIncludeEpisodes() && q.isIncludeSeries()) {
        url.append("/search/rest/series?");
      } else if(q.isIncludeEpisodes() && ! q.isIncludeSeries()) {
        url.append("/search/rest/episode?");
      } else {
        url.append("/search/rest/?");
      }
      url.append(URLEncodedUtils.format(queryStringParams, "UTF-8"));
      HttpGet get = new HttpGet(url.toString());
      try {
        HttpResponse response = trustedHttpClient.execute(get);
        int status = response.getStatusLine().getStatusCode();
        if (status != HttpStatus.SC_OK) {
          logger.info("Query repsonse from {} should have been HTTP 200 but was ", url, status);
          continue;
        }
        return SearchResultImpl.valueOf(response.getEntity().getContent());        
      } catch (Exception e) {
        logger.debug(e.getMessage(), e);
        continue;
      }
    }
    throw new SearchException("Unable to perform getByQuery from search index at any of " + remoteHosts.size() + " remote search services");
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.search.api.SearchService#getByQuery(java.lang.String, int, int)
   */
  @Override
  public SearchResult getByQuery(String query, int limit, int offset) throws SearchException {
    List<String> remoteHosts = remoteServiceManager.getRemoteHosts(JOB_TYPE);
    for(String remoteHost : remoteHosts) {
      List<NameValuePair> queryStringParams = new ArrayList<NameValuePair>();
      queryStringParams.add(new BasicNameValuePair("q", query));
      queryStringParams.add(new BasicNameValuePair("limit", Integer.toString(limit)));
      queryStringParams.add(new BasicNameValuePair("offset", Integer.toString(offset)));

      StringBuilder url = new StringBuilder(remoteHost);
      url.append("/search/rest/lucene?");
      url.append(URLEncodedUtils.format(queryStringParams, "UTF-8"));
      
      HttpGet get = new HttpGet(url.toString());
      logger.debug("Sending remote query '{}'", get.getRequestLine().toString());
      try {
        HttpResponse response = trustedHttpClient.execute(get);
        int status = response.getStatusLine().getStatusCode();
        if (status != HttpStatus.SC_OK) {
          logger.info("Query repsonse from {} should have been HTTP 200 but was ", url, status);
          continue;
        }
        return SearchResultImpl.valueOf(response.getEntity().getContent());        
      } catch (Exception e) {
        logger.debug(e.getMessage(), e);
        continue;
      }
    }
    throw new SearchException("Unable to perform getByQuery from search index at any of " + remoteHosts.size() + " remote search services");
  }
}
