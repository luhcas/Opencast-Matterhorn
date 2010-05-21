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
package org.opencastproject.composer.remote;

import org.opencastproject.composer.api.ComposerService;
import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.composer.api.EncodingProfileBuilder;
import org.opencastproject.composer.api.EncodingProfileImpl;
import org.opencastproject.composer.api.EncodingProfileList;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.opencastproject.receipt.api.Receipt;
import org.opencastproject.receipt.api.ReceiptService;
import org.opencastproject.receipt.api.Receipt.Status;
import org.opencastproject.security.api.TrustedHttpClient;

import org.apache.commons.collections.MapIterator;
import org.apache.commons.collections.bidimap.TreeBidiMap;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Proxies a set of remote composer services for use as a JVM-local service. Remote services are selected at random.
 */
public class ComposerServiceRemoteImpl implements ComposerService {
  private static final Logger logger = LoggerFactory.getLogger(ComposerServiceRemoteImpl.class);

  public static final String REMOTE_COMPOSER = "remote.composer";
  protected ResponseHandler<Long> longResponseHandler = new LongResponseHandler();
  protected ResponseHandler<EncodingProfile> encodingProfileResponseHandler = new EncodingProfileResponseHandler();
  protected ResponseHandler<EncodingProfileList> encodingProfileListResponseHandler = new EncodingProfileListResponseHandler();
  protected ResponseHandler<Receipt> receiptResponseHandler = new ReceiptResponseHandler();

  protected TrustedHttpClient trustedHttpClient;
  protected ReceiptService receiptService;

  public void setTrustedHttpClient(TrustedHttpClient trustedHttpClient) {
    this.trustedHttpClient = trustedHttpClient;
  }

  public void setReceiptService(ReceiptService receiptService) {
    this.receiptService = receiptService;
  }

  public void activate(ComponentContext cc) {
  }

  class HostAndLoad implements Comparable<HostAndLoad> {
    String host;
    Long load;

    public int compareTo(HostAndLoad o) {
      return (int) (load - o.load);
    }
  }

  /**
   * Finds the remote composer services, ordered by their load (lightest to heaviest).
   */
  protected List<String> getRemoteHosts() {
    Map<String, Long> runningComposerJobs = receiptService.getHostsCount(RECEIPT_TYPE, new Status[] { Status.QUEUED,
            Status.RUNNING });
    List<String> hosts = receiptService.getHosts(RECEIPT_TYPE);
    TreeBidiMap bidiMap = new TreeBidiMap(runningComposerJobs);

    LinkedList<String> sortedRemoteHosts = new LinkedList<String>();
    MapIterator iter = bidiMap.inverseOrderedBidiMap().orderedMapIterator();
    while (iter.hasNext()) {
      iter.next();
      sortedRemoteHosts.add((String) iter.getValue());
    }
    // If some of the hosts have no jobs, they are not in the list yet. Add them at the front of the list.
    for (String host : hosts) {
      if (!sortedRemoteHosts.contains(host)) {
        sortedRemoteHosts.add(0, host);
      }
    }
    return sortedRemoteHosts;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#countJobs(org.opencastproject.receipt.api.Receipt.Status)
   */
  @Override
  public long countJobs(Status status) {
    if (status == null)
      throw new IllegalArgumentException("status must not be null");
    return countJobs(status, null);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#countJobs(org.opencastproject.composer.api.Receipt.Status,
   *      java.lang.String)
   */
  @Override
  public long countJobs(Status status, String host) {
    if (status == null)
      throw new IllegalArgumentException("status must not be null");
    List<NameValuePair> queryStringParams = new ArrayList<NameValuePair>();
    queryStringParams.add(new BasicNameValuePair("status", status.toString().toLowerCase()));
    if (host != null) {
      queryStringParams.add(new BasicNameValuePair("host", host));
    }
    List<String> remoteHosts = getRemoteHosts();
    for (String remoteHost : remoteHosts) {
      String url = remoteHost + "/composer/rest/count?" + URLEncodedUtils.format(queryStringParams, "UTF-8");
      HttpGet get = new HttpGet(url);
      try {
        HttpResponse response = trustedHttpClient.execute(get);
        if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
          return longResponseHandler.handleResponse(response);
        }
      } catch (Exception e) {
        logger.info(e.getMessage(), e);
        continue;
      }
    }
    throw new RuntimeException("Unable to execute method, none of the " + remoteHosts.size()
            + " remote hosts are available");
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#encode(java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public Receipt encode(MediaPackage mediaPackage, String sourceTrackId, String profileId) throws EncoderException,
          MediaPackageException {
    return encode(mediaPackage, sourceTrackId, sourceTrackId, profileId, false);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#encode(java.lang.String, java.lang.String, java.lang.String,
   *      boolean)
   */
  @Override
  public Receipt encode(MediaPackage mediaPackage, String sourceTrackId, String profileId, boolean block)
          throws EncoderException, MediaPackageException {
    return encode(mediaPackage, sourceTrackId, sourceTrackId, profileId, block);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#encode(java.lang.String, java.lang.String, java.lang.String,
   *      java.lang.String)
   */
  @Override
  public Receipt encode(MediaPackage mediaPackage, String sourceVideoTrackId, String sourceAudioTrackId,
          String profileId) throws EncoderException, MediaPackageException {
    return encode(mediaPackage, sourceVideoTrackId, sourceAudioTrackId, profileId, false);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#encode(java.lang.String, java.lang.String, java.lang.String,
   *      java.lang.String, boolean)
   */
  @Override
  public Receipt encode(MediaPackage mediaPackage, String sourceVideoTrackId, String sourceAudioTrackId,
          String profileId, boolean block) throws EncoderException, MediaPackageException {
    Receipt r = null;
    List<String> remoteHosts = getRemoteHosts();
    for (String remoteHost : remoteHosts) {
      String url = remoteHost + "/composer/rest/encode";
      HttpPost post = new HttpPost(url);
      try {
        List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
        params.add(new BasicNameValuePair("mediapackage", mediaPackage.toXml()));
        params.add(new BasicNameValuePair("videoSourceTrackId", sourceVideoTrackId));
        params.add(new BasicNameValuePair("audioSourceTrackId", sourceAudioTrackId));
        params.add(new BasicNameValuePair("profileId", profileId));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);
        post.setEntity(entity);
        HttpResponse response = trustedHttpClient.execute(post);
        if(response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
          continue;
        }
        String content = EntityUtils.toString(response.getEntity());
        r = receiptService.parseReceipt(content);
        break;
      } catch (Exception e) {
        logger.info(e.getMessage(), e);
        continue;
      }
    }

    if(r == null) {
      throw new RuntimeException("Unable to execute method, none of the " + remoteHosts.size()
              + " remote hosts are available");
    }

    if (block) {
      r = poll(r.getId());
    }
    return r;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#getProfile(java.lang.String)
   */
  @Override
  public EncodingProfile getProfile(String profileId) {
    List<String> remoteHosts = getRemoteHosts();
    for (String remoteHost : remoteHosts) {
      String url = remoteHost + "/composer/rest/profile/" + profileId + ".xml";
      HttpGet get = new HttpGet(url);
      try {
        HttpResponse response = trustedHttpClient.execute(get);
        if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
          return encodingProfileResponseHandler.handleResponse(response);
        }
      } catch (Exception e) {
        logger.info(e.getMessage(), e);
        continue;
      }
    }
    throw new RuntimeException("Unable to execute method, none of the " + remoteHosts.size()
            + " remote hosts are available");
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#getReceipt(java.lang.String)
   */
  @Override
  public Receipt getReceipt(String id) {
    List<String> remoteHosts = getRemoteHosts();
    for (String remoteHost : remoteHosts) {
      String url = remoteHost + "/composer/rest/receipt/" + id + ".xml";
      HttpGet get = new HttpGet(url);
      try {
        HttpResponse response = trustedHttpClient.execute(get);
        if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
          return receiptResponseHandler.handleResponse(response);
        }
        return receiptResponseHandler.handleResponse(response);
      } catch (Exception e) {
        logger.info(e.getMessage(), e);
        continue;
      }
    }
    throw new RuntimeException("Unable to execute method, none of the " + remoteHosts.size()
            + " remote hosts are available");
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#image(org.opencastproject.media.mediapackage.MediaPackage,
   *      java.lang.String, java.lang.String, long)
   */
  @Override
  public Receipt image(MediaPackage mediaPackage, String sourceVideoTrackId, String profileId, long time)
          throws EncoderException, MediaPackageException {
    return image(mediaPackage, sourceVideoTrackId, profileId, time, false);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#image(org.opencastproject.media.mediapackage.MediaPackage,
   *      java.lang.String, java.lang.String, long, boolean)
   */
  @Override
  public Receipt image(MediaPackage mediaPackage, String sourceVideoTrackId, String profileId, long time, boolean block)
          throws EncoderException, MediaPackageException {
    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    params.add(new BasicNameValuePair("mediapackage", mediaPackage.toXml()));
    params.add(new BasicNameValuePair("sourceTrackId", sourceVideoTrackId));
    params.add(new BasicNameValuePair("profileId", profileId));
    params.add(new BasicNameValuePair("time", Long.toString(time)));
    UrlEncodedFormEntity entity;
    try {
      entity = new UrlEncodedFormEntity(params);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }

    Receipt r = null;

    List<String> remoteHosts = getRemoteHosts();
    for (String remoteHost : remoteHosts) {
      String url = remoteHost + "/composer/rest/image";
      HttpPost post = new HttpPost(url);
      try {
        post.setEntity(entity);
        HttpResponse response = trustedHttpClient.execute(post);
        if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
          r = receiptService.parseReceipt(response.getEntity().getContent());
          break;
        }
      } catch (Exception e) {
        logger.info(e.getMessage(), e);
        continue;
      }
    }

    if(r == null) {
      throw new RuntimeException("Unable to execute method, none of the " + remoteHosts.size()
              + " remote hosts are available");
    }

    if (block) {
      r = poll(r.getId());
    }
    return r;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#listProfiles()
   */
  @Override
  public EncodingProfile[] listProfiles() {
    List<String> remoteHosts = getRemoteHosts();
    for (String remoteHost : remoteHosts) {
      
      String url = remoteHost + "/composer/rest/profiles.xml";
      HttpGet get = new HttpGet(url);
      try {
        HttpResponse response = trustedHttpClient.execute(get);
        if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
          EncodingProfileList profileList = trustedHttpClient.execute(get, encodingProfileListResponseHandler);
          List<EncodingProfileImpl> list = profileList.getProfiles();
          return list.toArray(new EncodingProfile[list.size()]);
        }
      } catch(Exception e) {
        logger.info(e.getMessage(), e);
        continue;
      }
    }
    throw new RuntimeException("Unable to execute method, none of the " + remoteHosts.size()
            + " remote hosts are available");
  }

  /**
   * Polls for receipts until they return a status of {@link Status#FINISHED} or {@link Status#FAILED}
   * 
   * @param r
   *          The receipt id
   * @return The receipt
   */
  private Receipt poll(String id) {
    Receipt r = getReceipt(id);
    while (r.getStatus() != Status.FAILED && r.getStatus() != Status.FINISHED) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        logger.warn("polling interrupted");
      }
      r = getReceipt(id);
    }
    return r;
  }

  class LongResponseHandler implements ResponseHandler<Long> {
    public Long handleResponse(final HttpResponse response) throws HttpResponseException, IOException {
      HttpEntity entity = response.getEntity();
      return entity == null ? null : Long.valueOf(EntityUtils.toString(entity));
    }
  }

  class EncodingProfileResponseHandler implements ResponseHandler<EncodingProfile> {
    public EncodingProfile handleResponse(final HttpResponse response) throws HttpResponseException, IOException {
      HttpEntity entity = response.getEntity();
      if (entity == null) {
        return null;
      } else {
        try {
          return EncodingProfileBuilder.getInstance().parseProfile(entity.getContent());
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  class EncodingProfileListResponseHandler implements ResponseHandler<EncodingProfileList> {
    public EncodingProfileList handleResponse(final HttpResponse response) throws HttpResponseException, IOException {
      HttpEntity entity = response.getEntity();
      if (entity == null) {
        return null;
      } else {
        try {
          return EncodingProfileBuilder.getInstance().parseProfileList(entity.getContent());
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  class ReceiptResponseHandler implements ResponseHandler<Receipt> {
    public Receipt handleResponse(final HttpResponse response) throws HttpResponseException, IOException {
      HttpEntity entity = response.getEntity();
      try {
        return entity == null ? null : receiptService.parseReceipt(entity.getContent());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
}
