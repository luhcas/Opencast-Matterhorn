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
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.remote.api.Receipt;
import org.opencastproject.remote.api.RemoteServiceManager;
import org.opencastproject.remote.api.Receipt.Status;
import org.opencastproject.security.api.TrustedHttpClient;

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
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

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
  protected RemoteServiceManager remoteServiceManager;

  public void setTrustedHttpClient(TrustedHttpClient trustedHttpClient) {
    this.trustedHttpClient = trustedHttpClient;
  }

  public void setRemoteServiceManager(RemoteServiceManager remoteServiceManager) {
    this.remoteServiceManager = remoteServiceManager;
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
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#countJobs(orgorg.opencastproject.remote.Receipt.Status)
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
    List<String> remoteHosts = remoteServiceManager.getRemoteHosts(JOB_TYPE);
    Map<String, String> hostErrors = new HashMap<String, String>();
    for (String remoteHost : remoteHosts) {
      String url = remoteHost + "/composer/rest/count?" + URLEncodedUtils.format(queryStringParams, "UTF-8");
      HttpGet get = new HttpGet(url);
      try {
        HttpResponse response = trustedHttpClient.execute(get);
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
          return longResponseHandler.handleResponse(response);
        } else {
          hostErrors.put(remoteHost, response.getStatusLine().toString());
        }
      } catch (Exception e) {
        hostErrors.put(remoteHost, e.getMessage());
        continue;
      }
    }
    logger.warn("The following errors were encountered while attempting a {} remote service call: {}", JOB_TYPE,
            hostErrors);
    throw new RuntimeException("Unable to execute method, none of the " + remoteHosts.size()
            + " remote hosts are available");
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#encode(org.opencastproject.mediapackage.Track,
   *      java.lang.String)
   */
  public Receipt encode(Track sourceTrack, String profileId) throws EncoderException {
    return encode(sourceTrack, profileId, false);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#encode(org.opencastproject.mediapackage.Track,
   *      java.lang.String, boolean)
   */
  public Receipt encode(Track sourceTrack, String profileId, boolean block) throws EncoderException {
    Receipt r = null;
    List<String> remoteHosts = remoteServiceManager.getRemoteHosts(JOB_TYPE);
    Map<String, String> hostErrors = new HashMap<String, String>();
    for (String remoteHost : remoteHosts) {
      String url = remoteHost + "/composer/rest/encode";
      HttpPost post = new HttpPost(url);
      try {
        List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
        params.add(new BasicNameValuePair("sourceTrack", getXML(sourceTrack)));
        params.add(new BasicNameValuePair("profileId", profileId));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);
        post.setEntity(entity);
        HttpResponse response = trustedHttpClient.execute(post);
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
          hostErrors.put(remoteHost, response.getStatusLine().toString());
          continue;
        }
        String content = EntityUtils.toString(response.getEntity());
        r = remoteServiceManager.parseReceipt(content);
        break;
      } catch (Exception e) {
        hostErrors.put(remoteHost, e.getMessage());
        continue;
      }
    }

    if (r == null) {
      logger.warn("The following errors were encountered while attempting a {} remote service call: {}", JOB_TYPE,
              hostErrors);
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
   * @see org.opencastproject.composer.api.ComposerService#mux(org.opencastproject.mediapackage.Track,
   *      org.opencastproject.mediapackage.Track, java.lang.String)
   */
  public Receipt mux(Track sourceVideoTrack, Track sourceAudioTrack, String profileId) throws EncoderException {
    return mux(sourceVideoTrack, sourceAudioTrack, profileId, false);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#mux(org.opencastproject.mediapackage.Track,
   *      org.opencastproject.mediapackage.Track, java.lang.String, boolean)
   */
  public Receipt mux(Track sourceVideoTrack, Track sourceAudioTrack, String profileId, boolean block)
          throws EncoderException {
    Receipt r = null;
    List<String> remoteHosts = remoteServiceManager.getRemoteHosts(JOB_TYPE);
    Map<String, String> hostErrors = new HashMap<String, String>();
    for (String remoteHost : remoteHosts) {
      String url = remoteHost + "/composer/rest/mux";
      HttpPost post = new HttpPost(url);
      try {
        List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
        params.add(new BasicNameValuePair("sourceVideoTrack", getXML(sourceVideoTrack)));
        params.add(new BasicNameValuePair("sourceAudioTrack", getXML(sourceAudioTrack)));
        params.add(new BasicNameValuePair("profileId", profileId));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);
        post.setEntity(entity);
        HttpResponse response = trustedHttpClient.execute(post);
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
          hostErrors.put(remoteHost, response.getStatusLine().toString());
          continue;
        }
        String content = EntityUtils.toString(response.getEntity());
        r = remoteServiceManager.parseReceipt(content);
        break;
      } catch (Exception e) {
        hostErrors.put(remoteHost, e.getMessage());
        continue;
      }
    }

    if (r == null) {
      logger.warn("The following errors were encountered while attempting a {} remote service call: {}", JOB_TYPE,
              hostErrors);
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
    List<String> remoteHosts = remoteServiceManager.getRemoteHosts(JOB_TYPE);
    Map<String, String> hostErrors = new HashMap<String, String>();
    for (String remoteHost : remoteHosts) {
      String url = remoteHost + "/composer/rest/profile/" + profileId + ".xml";
      HttpGet get = new HttpGet(url);
      try {
        HttpResponse response = trustedHttpClient.execute(get);
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
          return encodingProfileResponseHandler.handleResponse(response);
        } else {
          hostErrors.put(remoteHost, response.getStatusLine().toString());
        }
      } catch (Exception e) {
        hostErrors.put(remoteHost, e.getMessage());
        continue;
      }
    }
    logger.warn("The following errors were encountered while attempting a {} remote service call: {}", JOB_TYPE,
            hostErrors);
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
    List<String> remoteHosts = remoteServiceManager.getRemoteHosts(JOB_TYPE);
    Map<String, String> hostErrors = new HashMap<String, String>();
    for (String remoteHost : remoteHosts) {
      String url = remoteHost + "/composer/rest/receipt/" + id + ".xml";
      HttpGet get = new HttpGet(url);
      try {
        HttpResponse response = trustedHttpClient.execute(get);
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
          return receiptResponseHandler.handleResponse(response);
        } else {
          hostErrors.put(remoteHost, response.getStatusLine().toString());
          continue;
        }
      } catch (Exception e) {
        hostErrors.put(remoteHost, e.getMessage());
        continue;
      }
    }
    logger.warn("The following errors were encountered while attempting a {} remote service call: {}", JOB_TYPE,
            hostErrors);
    throw new RuntimeException("Unable to execute method, none of the " + remoteHosts.size()
            + " remote hosts are available");
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#image(org.opencastproject.mediapackage.Track,
   *      java.lang.String, long)
   */
  public Receipt image(Track sourceTrack, String profileId, long time) throws EncoderException {
    return image(sourceTrack, profileId, time, false);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#image(org.opencastproject.mediapackage.Track,
   *      java.lang.String, long, boolean)
   */
  public Receipt image(Track sourceTrack, String profileId, long time, boolean block) throws EncoderException {
    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    UrlEncodedFormEntity entity = null;

    try {
      params.add(new BasicNameValuePair("sourceTrack", getXML(sourceTrack)));
      params.add(new BasicNameValuePair("profileId", profileId));
      params.add(new BasicNameValuePair("time", Long.toString(time)));
      entity = new UrlEncodedFormEntity(params);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    Receipt r = null;

    List<String> remoteHosts = remoteServiceManager.getRemoteHosts(JOB_TYPE);
    Map<String, String> hostErrors = new HashMap<String, String>();
    for (String remoteHost : remoteHosts) {
      String url = remoteHost + "/composer/rest/image";
      HttpPost post = new HttpPost(url);
      try {
        post.setEntity(entity);
        HttpResponse response = trustedHttpClient.execute(post);
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
          r = remoteServiceManager.parseReceipt(response.getEntity().getContent());
          break;
        } else {
          hostErrors.put(remoteHost, response.getStatusLine().toString());
        }
      } catch (Exception e) {
        hostErrors.put(remoteHost, e.getMessage());
        continue;
      }
    }

    if (r == null) {
      logger.warn("The following errors were encountered while attempting a {} remote service call: {}", JOB_TYPE,
              hostErrors);
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
    List<String> remoteHosts = remoteServiceManager.getRemoteHosts(JOB_TYPE);
    Map<String, String> hostErrors = new HashMap<String, String>();
    for (String remoteHost : remoteHosts) {
      String url = remoteHost + "/composer/rest/profiles.xml";
      HttpGet get = new HttpGet(url);
      try {
        HttpResponse response = trustedHttpClient.execute(get);
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
          EncodingProfileList profileList = trustedHttpClient.execute(get, encodingProfileListResponseHandler);
          List<EncodingProfileImpl> list = profileList.getProfiles();
          return list.toArray(new EncodingProfile[list.size()]);
        } else {
          hostErrors.put(remoteHost, response.getStatusLine().toString());
        }
      } catch (Exception e) {
        hostErrors.put(remoteHost, e.getMessage());
        continue;
      }
    }
    logger.warn("The following errors were encountered while attempting a {} remote service call: {}", JOB_TYPE,
            hostErrors);
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
        return entity == null ? null : remoteServiceManager.parseReceipt(entity.getContent());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
  
  public String getXML(MediaPackageElement element) throws Exception {
    if (element == null)
      return null;
    DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document doc = docBuilder.newDocument();
    Node node = element.toManifest(doc, null);
    DOMSource domSource = new DOMSource(node);
    StringWriter writer = new StringWriter();
    StreamResult result = new StreamResult(writer);
    Transformer transformer;
    transformer = TransformerFactory.newInstance().newTransformer();
    transformer.transform(domSource, result);
    return writer.toString();
  }

}
