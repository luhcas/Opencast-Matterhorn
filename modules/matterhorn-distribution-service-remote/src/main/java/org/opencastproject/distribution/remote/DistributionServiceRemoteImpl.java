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
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.remote.api.RemoteBase;

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
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * A remote distribution service invoker.
 */
public class DistributionServiceRemoteImpl extends RemoteBase implements DistributionService {
  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(DistributionServiceRemoteImpl.class);

  /** The service type prefix */
  public static final String REMOTE_SERVICE_TYPE_PREFIX = "org.opencastproject.distribution.";

  /** The property to look up and append to REMOTE_SERVICE_TYPE_PREFIX */
  public static final String REMOTE_SERVICE_CHANNEL = "distribution.channel";

  /** The distribution channel identifier */
  protected String distributionChannel;

  public DistributionServiceRemoteImpl() {
    // the service type is not available at construction time. we need to wait for activation to set this value
    super("waiting for activation");
  }

  /** activates the component */
  protected void activate(ComponentContext cc) {
    this.distributionChannel = (String) cc.getProperties().get(REMOTE_SERVICE_CHANNEL);
    super.serviceType = REMOTE_SERVICE_TYPE_PREFIX + this.distributionChannel;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.distribution.api.DistributionService#distribute(org.opencastproject.mediapackage.MediaPackage,
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
    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    params.add(new BasicNameValuePair("mediapackage", xml));
    if (elementIds != null && elementIds.length > 0) {
      for (String elementId : elementIds) {
        params.add(new BasicNameValuePair("elementId", elementId));
      }
    }
    String url = "/distribution/rest/" + distributionChannel;
    HttpPost post = new HttpPost(url);
    InputStream in = null;
    HttpResponse response = null;
    try {
      UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);
      post.setEntity(entity);
      response = getResponse(post);
      if (response != null) {
        in = response.getEntity().getContent();
        MediaPackage result = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().loadFromXml(in);
        logger.info("distributed {} from {}", mediaPackage, distributionChannel);
        return result;
      }
    } catch (Exception e) {
      throw new DistributionException("Unable to distribute mediapackage " + mediaPackage
              + " using a remote distribution service proxy.", e);
    } finally {
      IOUtils.closeQuietly(in);
      closeConnection(response);
    }
    throw new DistributionException("Unable to distribute mediapackage " + mediaPackage
            + " using a remote distribution service proxy.");
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.distribution.api.DistributionService#retract(java.lang.String)
   */
  @Override
  public void retract(String mediaPackageId) throws DistributionException {
    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    params.add(new BasicNameValuePair("mediapackageId", mediaPackageId));
    String url = "/distribution/rest/retract/" + distributionChannel;
    HttpPost post = new HttpPost(url);
    HttpResponse response = null;
    UrlEncodedFormEntity entity = null;
    try {
      entity = new UrlEncodedFormEntity(params);
    } catch (UnsupportedEncodingException e) {
      throw new DistributionException("Unable to encode mediapackage " + mediaPackageId + " for http post", e);
    }
    post.setEntity(entity);
    try {
      response = getResponse(post, HttpStatus.SC_NO_CONTENT);
      if (response != null) {
        logger.info("retracted {} from {}", mediaPackageId, distributionChannel);
        return;
      }
    } finally {
      closeConnection(response);
    }
    throw new DistributionException("Unable to retract mediapackage " + mediaPackageId
            + " using a remote distribution service proxy");
  }
}
