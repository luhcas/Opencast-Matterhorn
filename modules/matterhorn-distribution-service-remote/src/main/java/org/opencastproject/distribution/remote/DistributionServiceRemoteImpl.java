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
package org.opencastproject.distribution.remote;

import org.opencastproject.distribution.api.DistributionException;
import org.opencastproject.distribution.api.DistributionService;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.opencastproject.remote.api.RemoteServiceManager;
import org.opencastproject.security.api.TrustedHttpClient;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * A remote distribution service invoker.
 */
public class DistributionServiceRemoteImpl implements DistributionService {
  private static final Logger logger = LoggerFactory.getLogger(DistributionServiceRemoteImpl.class);
  public static final String REMOTE_SERVICE_TYPE_PREFIX = "org.opencastproject.distribution.";
  public static final String REMOTE_SERVICE_CHANNEL = "distribution.channel";
  protected String distributionChannel;
  protected TrustedHttpClient trustedHttpClient;
  protected RemoteServiceManager remoteServiceManager;

  public void setTrustedHttpClient(TrustedHttpClient trustedHttpClient) {
    this.trustedHttpClient = trustedHttpClient;
  }

  public void setRemoteServiceManager(RemoteServiceManager remoteServiceManager) {
    this.remoteServiceManager = remoteServiceManager;
  }

  protected void activate(ComponentContext cc) {
    this.distributionChannel = (String) cc.getProperties().get(REMOTE_SERVICE_CHANNEL);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.distribution.api.DistributionService#distribute(org.opencastproject.media.mediapackage.MediaPackage,
   *      java.lang.String[])
   */
  @Override
  public MediaPackage distribute(MediaPackage mediaPackage, String... elementIds) throws DistributionException {
    String xml = null;
    try {
      xml = mediaPackage.toXml();
    } catch (MediaPackageException e) {
      throw new DistributionException("Unable to marshall mediapackage to xml: " + e.getMessage());
    }
    List<String> remoteHosts = remoteServiceManager.getRemoteHosts(REMOTE_SERVICE_TYPE_PREFIX + distributionChannel);
    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    params.add(new BasicNameValuePair("mediapackage", xml));
    if (elementIds != null && elementIds.length > 0) {
      for (String elementId : elementIds) {
        params.add(new BasicNameValuePair("elementId", elementId));
      }
    }
    for (String remoteHost : remoteHosts) {
      String url = remoteHost + "/distribution/rest/" + distributionChannel;
      HttpPost post = new HttpPost(url);
      InputStream in = null;
      try {
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);
        post.setEntity(entity);
        HttpResponse response = trustedHttpClient.execute(post);
        in = response.getEntity().getContent();
        return MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().loadFromXml(in);
      } catch (Exception e) {
        continue;
      } finally {
        IOUtils.closeQuietly(in);
      }
    }
    throw new DistributionException("Unable to distribute mediapackage " + mediaPackage + " to any of "
            + remoteHosts.size() + " remote services.");
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.distribution.api.DistributionService#retract(org.opencastproject.media.mediapackage.MediaPackage)
   */
  @Override
  public void retract(MediaPackage mediaPackage) throws DistributionException {
    String xml = null;
    try {
      xml = mediaPackage.toXml();
    } catch (MediaPackageException e) {
      throw new DistributionException("Unable to marshall mediapackage to xml: " + e.getMessage());
    }
    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    params.add(new BasicNameValuePair("mediapackage", xml));
    List<String> remoteHosts = remoteServiceManager.getRemoteHosts(REMOTE_SERVICE_TYPE_PREFIX + distributionChannel);
    for(String remoteHost : remoteHosts) {
      String url = remoteHost + "/distribution/rest/retract/" + distributionChannel;
      HttpPost post = new HttpPost(url);
      try {
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);
        post.setEntity(entity);
        HttpResponse response = trustedHttpClient.execute(post);
        int httpStatusCode = response.getStatusLine().getStatusCode();
        if (HttpStatus.SC_NO_CONTENT != httpStatusCode) {
          logger.debug("Unable to retract using distribution service at {}. Status code = {}", url, httpStatusCode);
          continue;
        }
      } catch (Exception e) {
        logger.debug("Unable to retract using distribution service at {}. {}", url, e);
      }
    }
    throw new DistributionException("Unable to retract mediapackage " + mediaPackage + " using any of " + remoteHosts.size() + " remote services");
  }
}
