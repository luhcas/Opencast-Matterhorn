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
import org.opencastproject.security.api.TrustedHttpClient;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.osgi.service.component.ComponentContext;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * A remote distribution service invoker.
 */
public class DistributionServiceRemoteImpl implements DistributionService {
  public static final String REMOTE_SERVICE_KEY = "remote.distribution";
  public static final String REMOTE_SERVICE_CHANNEL = "distribution.channel";
  protected String distributionChannel;
  protected String remoteHost;
  protected TrustedHttpClient trustedHttpClient;
  
  /** Default, no-arg constructor needed by OSGI declarative services */
  public DistributionServiceRemoteImpl() {}

  /** Constructs a DistributionServiceRemoteImpl with a specific remote host and dist channel */
  public DistributionServiceRemoteImpl(String remoteHost, String distributionChannel) {
    this.remoteHost = remoteHost;
    this.distributionChannel = distributionChannel;
  }
  
  public void setRemoteHost(String remoteHost) {
    this.remoteHost = remoteHost;
  }

  public void setTrustedHttpClient(TrustedHttpClient trustedHttpClient) {
    this.trustedHttpClient = trustedHttpClient;
  }
 
  public void activate(ComponentContext cc) {
    this.remoteHost = cc.getBundleContext().getProperty(REMOTE_SERVICE_KEY);
    this.distributionChannel = (String)cc.getProperties().get(REMOTE_SERVICE_CHANNEL);
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.distribution.api.DistributionService#distribute(org.opencastproject.media.mediapackage.MediaPackage, java.lang.String[])
   */
  @Override
  public MediaPackage distribute(MediaPackage mediaPackage, String... elementIds) throws DistributionException {
    String url = remoteHost + "/distribution/rest/" + distributionChannel;
    HttpPost post = new HttpPost(url);
    InputStream in = null;
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("mediapackage", mediaPackage.toXml()));
      if(elementIds != null && elementIds.length > 0) {
        for(String elementId : elementIds) {
          params.add(new BasicNameValuePair("elementId", elementId));
        }
      }
      UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);
      post.setEntity(entity);
      HttpResponse response = trustedHttpClient.execute(post);
      in = response.getEntity().getContent();
      return MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().loadFromXml(in);
    } catch(Exception e) {
      throw new DistributionException(e);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.distribution.api.DistributionService#retract(org.opencastproject.media.mediapackage.MediaPackage)
   */
  @Override
  public void retract(MediaPackage mediaPackage) throws DistributionException {
    String url = remoteHost + "/distribution/rest/retract/" + distributionChannel;
    HttpPost post = new HttpPost(url);
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("mediapackage", mediaPackage.toXml()));
      UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);
      post.setEntity(entity);
      HttpResponse response = trustedHttpClient.execute(post);
      if(HttpStatus.SC_NO_CONTENT != response.getStatusLine().getStatusCode()) {
        throw new DistributionException("Remote retract failed with status code " + response.getStatusLine().getStatusCode());
      }
    } catch(Exception e) {
      throw new DistributionException(e);
    }
  }
  
}
