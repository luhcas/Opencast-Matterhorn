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
package org.opencastproject.composer.impl;

import org.opencastproject.composer.api.ComposerService;
import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.composer.api.Receipt;
import org.opencastproject.composer.api.Receipt.Status;
import org.opencastproject.composer.impl.endpoint.EncodingProfileList;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.opencastproject.security.api.TrustedHttpClientFactory;
import org.opencastproject.security.api.TrustedHttpClientFactory.HttpClientAndContext;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Proxies a remote composer service for use as a JVM-local service.
 */
public class ComposerServiceRemoteImpl implements ComposerService {
  private static final Logger logger = LoggerFactory.getLogger(ComposerServiceRemoteImpl.class);

  public static final String REMOTE_COMPOSER = "remote.composer";
  public static final ResponseHandler<Long> longResponseHandler = new LongResponseHandler();
  public static final ResponseHandler<EncodingProfile> encodingProfileResponseHandler = new EncodingProfileResponseHandler();
  public static final ResponseHandler<EncodingProfileList> encodingProfileListResponseHandler = new EncodingProfileListResponseHandler();
  public static final ResponseHandler<Receipt> receiptResponseHandler = new ReceiptResponseHandler();

  protected String remoteHost;
  protected TrustedHttpClientFactory trustedClientFactory;

  public void setRemoteHost(String remoteHost) {
    this.remoteHost = remoteHost;
  }

  public void setTrustedClientFactory(TrustedHttpClientFactory trustedClientFactory) {
    this.trustedClientFactory = trustedClientFactory;
  }

  public ComposerServiceRemoteImpl() {
  }

  public ComposerServiceRemoteImpl(String remoteHost) {
    this.remoteHost = remoteHost;
  }

  public void activate(ComponentContext cc) {
    this.remoteHost = cc.getBundleContext().getProperty(REMOTE_COMPOSER);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#countJobs(org.opencastproject.composer.api.Receipt.Status)
   */
  @Override
  public long countJobs(Status status) {
    if (status == null) throw new IllegalArgumentException("status must not be null");
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
    if (status == null) throw new IllegalArgumentException("status must not be null");
    List<NameValuePair> queryStringParams = new ArrayList<NameValuePair>();
    queryStringParams.add(new BasicNameValuePair("status", status.toString().toLowerCase()));
    if(host != null) {
      queryStringParams.add(new BasicNameValuePair("host", host));
    }
    String url = remoteHost + "/composer/rest/count?" + URLEncodedUtils.format(queryStringParams, "UTF-8");
    HttpClient client = trustedClientFactory.getTrustedHttpClient(url);
    HttpGet get = trustedClientFactory.getTrustedHttpGet(url);
    try {
      return client.execute(get, longResponseHandler);
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      client.getConnectionManager().shutdown();
    }
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
    String url = remoteHost + "/composer/rest/encode";
    HttpClientAndContext clientAndContext = trustedClientFactory.getTrustedHttpClientAndContext(url);
    HttpPost post = trustedClientFactory.getTrustedHttpPost(url);
    Receipt r;
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("mediapackage", mediaPackage.toXml()));
      params.add(new BasicNameValuePair("videoSourceTrackId", sourceVideoTrackId));
      params.add(new BasicNameValuePair("audioSourceTrackId", sourceAudioTrackId));
      params.add(new BasicNameValuePair("profileId", profileId));
      UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);
      post.setEntity(entity);
      HttpResponse response = clientAndContext.getHttpClient().execute(post, clientAndContext.getHttpContext());
      
      String content = EntityUtils.toString(response.getEntity());
      r = ReceiptBuilder.getInstance().parseReceipt(content);
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      clientAndContext.getHttpClient().getConnectionManager().shutdown();
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
    String url = remoteHost + "/composer/rest/profile/" + profileId + ".xml";
    HttpClient client = trustedClientFactory.getTrustedHttpClient(url);
    HttpGet get = trustedClientFactory.getTrustedHttpGet(url);
    try {
      return client.execute(get, encodingProfileResponseHandler);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#getReceipt(java.lang.String)
   */
  @Override
  public Receipt getReceipt(String id) {
    String url = remoteHost + "/composer/rest/receipt/" + id + ".xml";
    HttpClient client = trustedClientFactory.getTrustedHttpClient(url);
    HttpGet get = trustedClientFactory.getTrustedHttpGet(url);
    try {
      return client.execute(get, receiptResponseHandler);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
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
    String url = remoteHost + "/composer/rest/image";
    HttpClientAndContext clientAndContext = trustedClientFactory.getTrustedHttpClientAndContext(url);
    HttpPost post = trustedClientFactory.getTrustedHttpPost(url);
    Receipt r;
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("mediapackage", mediaPackage.toXml()));
      params.add(new BasicNameValuePair("sourceTrackId", sourceVideoTrackId));
      params.add(new BasicNameValuePair("profileId", profileId));
      params.add(new BasicNameValuePair("time", Long.toString(time)));
      UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);
      post.setEntity(entity);
      HttpResponse response = clientAndContext.getHttpClient().execute(post, clientAndContext.getHttpContext());
      r = ReceiptBuilder.getInstance().parseReceipt(response.getEntity().getContent());
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      clientAndContext.getHttpClient().getConnectionManager().shutdown();
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
    String url = remoteHost + "/composer/rest/profiles.xml";
    HttpClient client = trustedClientFactory.getTrustedHttpClient(url);
    HttpGet get = trustedClientFactory.getTrustedHttpGet(url);
    try {
      EncodingProfileList profileList = client.execute(get, encodingProfileListResponseHandler);
      List<EncodingProfileImpl> list = profileList.getProfiles();
      return list.toArray(new EncodingProfile[list.size()]);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
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

  static class LongResponseHandler implements ResponseHandler<Long> {
    public Long handleResponse(final HttpResponse response) throws HttpResponseException, IOException {
      StatusLine statusLine = response.getStatusLine();
      if(statusLine.getStatusCode() == 404) {
        return null;
      } else if (statusLine.getStatusCode() >= 300) {
        throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
      }
      HttpEntity entity = response.getEntity();
      return entity == null ? null : Long.valueOf(EntityUtils.toString(entity));
    }
  }

  static class EncodingProfileResponseHandler implements ResponseHandler<EncodingProfile> {
    public EncodingProfile handleResponse(final HttpResponse response) throws HttpResponseException, IOException {
      StatusLine statusLine = response.getStatusLine();
      if(statusLine.getStatusCode() == 404) {
        return null;
      } else if (statusLine.getStatusCode() >= 300) {
        throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
      }
      HttpEntity entity = response.getEntity();
      if(entity == null) {
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

  static class EncodingProfileListResponseHandler implements ResponseHandler<EncodingProfileList> {
    public EncodingProfileList handleResponse(final HttpResponse response) throws HttpResponseException, IOException {
      StatusLine statusLine = response.getStatusLine();
      if(statusLine.getStatusCode() == 404) {
        return null;
      } else if (statusLine.getStatusCode() >= 300) {
        throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
      }
      HttpEntity entity = response.getEntity();
      if(entity == null) {
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

  static class ReceiptResponseHandler implements ResponseHandler<Receipt> {
    public Receipt handleResponse(final HttpResponse response) throws HttpResponseException, IOException {
      StatusLine statusLine = response.getStatusLine();
      if(statusLine.getStatusCode() == 404) {
        return null;
      } else if (statusLine.getStatusCode() >= 300) {
        throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
      }
      HttpEntity entity = response.getEntity();
      try {
        return entity == null ? null : ReceiptBuilder.getInstance().parseReceipt(entity.getContent());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
}
