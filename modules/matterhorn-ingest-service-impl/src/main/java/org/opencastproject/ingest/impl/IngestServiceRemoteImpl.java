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
package org.opencastproject.ingest.impl;

import org.opencastproject.ingest.api.IngestException;
import org.opencastproject.ingest.api.IngestService;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.UnsupportedElementException;
import org.opencastproject.mediapackage.identifier.HandleException;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.security.api.TrustedHttpClientException;
import org.opencastproject.util.ConfigurationException;
import org.opencastproject.workflow.api.WorkflowBuilder;
import org.opencastproject.workflow.api.WorkflowInstance;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.message.BasicNameValuePair;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Proxies a remote ingest service for use as a JVM-local service.
 */
public class IngestServiceRemoteImpl implements IngestService {
  private static final Logger logger = LoggerFactory.getLogger(IngestServiceRemoteImpl.class);

  public static final String REMOTE_INGEST = "remote.ingest";
  protected ResponseHandler<MediaPackage> mediaPackageResponseHandler = new MediaPackageResponseHandler();
  protected ResponseHandler<WorkflowInstance> workflowInstanceResponseHandler = new WorkflowInstanceResponseHandler();
  protected String remoteHost;
  protected TrustedHttpClient trustedHttpClient;
  protected MediaPackageBuilder mpBuilder;
  protected WorkflowBuilder wfBuilder;

  public void setTrustedHttpClient(TrustedHttpClient trustedHttpClient) {
    this.trustedHttpClient = trustedHttpClient;
  }

  public void setRemoteHost(String remoteHost) {
    this.remoteHost = remoteHost;
  }

  public IngestServiceRemoteImpl() {
    mpBuilder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
    wfBuilder = WorkflowBuilder.getInstance();
  }

  public IngestServiceRemoteImpl(String remoteHost) {
    this.remoteHost = remoteHost;
  }

  public void activate(ComponentContext cc) {
    this.remoteHost = cc.getBundleContext().getProperty(REMOTE_INGEST);
  }

  @Override
  public MediaPackage addAttachment(URI uri, MediaPackageElementFlavor flavor, MediaPackage mediaPackage)
          throws MediaPackageException, UnsupportedElementException, IOException {
    logger.info("Adding an attachment (" + uri + ") on a remote server: " + remoteHost);
    String remoteHostMethod = remoteHost + "/ingest/rest/addAttachment";
    return addMediaPackageElement(remoteHostMethod, uri, mediaPackage, flavor);
  }

  @Override
  public MediaPackage addAttachment(InputStream file, String fileName, MediaPackageElementFlavor flavor,
          MediaPackage mediaPackage) throws MediaPackageException, UnsupportedElementException, MalformedURLException,
          IOException {
    logger.info("Adding an attachment from a local file to a remote server: " + remoteHost);
    String remoteHostMethod = remoteHost + "/ingest/rest/addAttachment";
    return addMediaPackageElement(remoteHostMethod, file, fileName, mediaPackage, flavor);
  }

  @Override
  public MediaPackage addCatalog(URI uri, MediaPackageElementFlavor flavor, MediaPackage mediaPackage)
          throws MediaPackageException, UnsupportedElementException, IOException {
    logger.info("Adding a catalog (" + uri + ") on a remote server: " + remoteHost);
    String remoteHostMethod = remoteHost + "/ingest/rest/addCatalog";
    return addMediaPackageElement(remoteHostMethod, uri, mediaPackage, flavor);
  }

  @Override
  public MediaPackage addCatalog(InputStream catalog, String fileName, MediaPackageElementFlavor flavor,
          MediaPackage mediaPackage) throws MediaPackageException, UnsupportedElementException, MalformedURLException,
          IOException {
    logger.info("Adding a catalog from a local file to a remote server: " + remoteHost);
    String remoteHostMethod = remoteHost + "/ingest/rest/addCatalog";
    return addMediaPackageElement(remoteHostMethod, catalog, fileName, mediaPackage, flavor);
  }

  @Override
  public MediaPackage addTrack(URI uri, MediaPackageElementFlavor flavor, MediaPackage mediaPackage)
          throws MediaPackageException, UnsupportedElementException, IOException {
    logger.info("Adding a track (" + uri + ") on a remote server: " + remoteHost);
    String remoteHostMethod = remoteHost + "/ingest/rest/addTrack";
    return addMediaPackageElement(remoteHostMethod, uri, mediaPackage, flavor);
  }

  @Override
  public MediaPackage addTrack(InputStream mediaFile, String fileName, MediaPackageElementFlavor flavor,
          MediaPackage mediaPackage) throws MediaPackageException, UnsupportedElementException, MalformedURLException,
          IOException {
    logger.info("Adding a track from a local file to a remote server: " + remoteHost);
    String remoteHostMethod = remoteHost + "/ingest/rest/addTrack";
    return addMediaPackageElement(remoteHostMethod, mediaFile, fileName, mediaPackage, flavor);
  }

  @Override
  public WorkflowInstance addZippedMediaPackage(InputStream ZippedMediaPackage) throws FileNotFoundException,
          IOException, IngestException {
    logger.info("Adding and ingesting a zipped media package on a remote server: " + remoteHost);
    String remoteHostMethod = remoteHost + "/ingest/rest/addZippedMediaPackage";
    MultipartEntity mpEntity = new MultipartEntity();
    mpEntity.addPart("mp", new InputStreamBody(ZippedMediaPackage, "ZippedMediaPackage.zip"));
    return ingestMediaPackage(remoteHostMethod, mpEntity);
  }

  @Override
  public WorkflowInstance addZippedMediaPackage(InputStream ZippedMediaPackage, String workflowDefinitionID)
          throws FileNotFoundException, IOException, IngestException {
    logger.info("Adding and ingesting a zipped media package on a remote server: " + remoteHost);
    String remoteHostMethod = remoteHost + "/ingest/rest/addZippedMediaPackage/" + workflowDefinitionID;
    MultipartEntity mpEntity = new MultipartEntity();
    mpEntity.addPart("mp", new InputStreamBody(ZippedMediaPackage, "ZippedMediaPackage.zip"));
    return ingestMediaPackage(remoteHostMethod, mpEntity);
  }

  @Override
  public WorkflowInstance addZippedMediaPackage(InputStream ZippedMediaPackage, String workflowDefinitionID,
          Map<String, String> wfConfig) throws FileNotFoundException, IOException, IngestException {
    logger.info("Adding and ingesting a zipped media package on a remote server: " + remoteHost);
    MultipartEntity mpEntity = new MultipartEntity();
    String remoteHostMethod = remoteHost + "/ingest/rest/addZippedMediaPackage/" + workflowDefinitionID;
    for (String key : wfConfig.keySet()) {
      mpEntity.addPart(key, new StringBody(wfConfig.get(key)));
    }
    mpEntity.addPart("mp", new InputStreamBody(ZippedMediaPackage, "ZippedMediaPackage.zip"));
    return ingestMediaPackage(remoteHostMethod, mpEntity);
  }

  @Override
  public MediaPackage createMediaPackage() throws MediaPackageException, ConfigurationException, HandleException {
    logger.info("Creating a new media package on a remote server: " + remoteHost);
    String remoteHostMethod = remoteHost + "/ingest/rest/createMediaPackage";
    HttpGet get = new HttpGet(remoteHostMethod);
    try {
      return trustedHttpClient.execute(get, mediaPackageResponseHandler);
    } catch (TrustedHttpClientException e) {
      throw new MediaPackageException("Unable to create a media package", e);
    }
  }

  @Override
  public void discardMediaPackage(MediaPackage mediaPackage) throws IOException {
    String id = mediaPackage.getIdentifier().compact();
    logger.info("Discarding a media package (" + id + ") on a remote server: " + remoteHost);

    String remoteHostMethod = remoteHost + "/ingest/rest/discardMediaPackage";
    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    UrlEncodedFormEntity entity;
    params.add(new BasicNameValuePair("mediaPackage", mediaPackage.toXml()));
    entity = new UrlEncodedFormEntity(params);
    HttpPost post = new HttpPost(remoteHostMethod);
    post.setEntity(entity);
    HttpResponse response = null;
    try {
      response = trustedHttpClient.execute(post);
    } finally {
      trustedHttpClient.close(response);
    }
  }

  @Override
  public WorkflowInstance ingest(MediaPackage mediaPackage) throws IngestException {
    String id = mediaPackage.getIdentifier().compact();
    logger.info("Ingesting a media package (" + id + ") on a remote server: " + remoteHost);

    String remoteHostMethod = remoteHost + "/ingest/rest/ingest";
    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    params.add(new BasicNameValuePair("mediaPackage", mediaPackage.toXml()));
    return ingestMediaPackage(remoteHostMethod, params);
  }

  @Override
  public WorkflowInstance ingest(MediaPackage mediaPackage, String workflowDefinitionID) throws IllegalStateException,
          IngestException {
    String id = mediaPackage.getIdentifier().compact();
    logger.info("Ingesting a media package (" + id + ") with set workflow (" + workflowDefinitionID
            + ") on a remote server: " + remoteHost);

    // remoteHostMethod
    String remoteHostMethod = remoteHost + "/ingest/rest/ingest/" + workflowDefinitionID;
    // params
    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    params.add(new BasicNameValuePair("mediaPackage", mediaPackage.toXml()));
    // call
    return ingestMediaPackage(remoteHostMethod, params);
  }

  @Override
  public WorkflowInstance ingest(MediaPackage mediaPackage, String workflowDefinitionID, Map<String, String> properties)
          throws IngestException {
    String id = mediaPackage.getIdentifier().compact();
    logger.info("Ingesting a media package (" + id + ") with set workflow (" + workflowDefinitionID
            + ") on a remote server: " + remoteHost);
    // remoteHostMethod
    String remoteHostMethod = remoteHost + "/ingest/rest/ingest/" + workflowDefinitionID;
    // params
    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    params.add(new BasicNameValuePair("mediaPackage", mediaPackage.toXml()));
    for (String key : properties.keySet()) {
      params.add(new BasicNameValuePair(key, properties.get(key)));
    }
    // call
    return ingestMediaPackage(remoteHostMethod, params);
  }

  // ------------------- helper methods ------------------
  protected MediaPackage addMediaPackageElement(String remoteHostMethod, URI uri, MediaPackage mediaPackage,
          MediaPackageElementFlavor flavor) throws MediaPackageException, IOException {
    HttpPost post = new HttpPost(remoteHostMethod);
    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    params.add(new BasicNameValuePair("url", uri.toString()));
    params.add(new BasicNameValuePair("mediaPackage", mediaPackage.toXml()));
    params.add(new BasicNameValuePair("flavor", flavor.toString()));
    UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);
    post.setEntity(entity);
    return trustedHttpClient.execute(post, mediaPackageResponseHandler);
  }

  protected MediaPackage addMediaPackageElement(String remoteHostMethod, InputStream file, String filename,
          MediaPackage mediaPackage, MediaPackageElementFlavor flavor) throws MediaPackageException,
          UnsupportedEncodingException, IOException {
    // build http entity
    MultipartEntity mpEntity = new MultipartEntity();
    mpEntity.addPart("mediaPackage", new StringBody(mediaPackage.toXml()));
    mpEntity.addPart("flavor", new StringBody(flavor.toString()));
    mpEntity.addPart("userfile", new InputStreamBody(file, filename));
    // build HttpPost
    HttpPost httppost = new HttpPost(remoteHostMethod);
    httppost.setEntity(mpEntity);
    return trustedHttpClient.execute(httppost, mediaPackageResponseHandler);
  }

  protected WorkflowInstance ingestMediaPackage(String remoteHostMethod, List<BasicNameValuePair> params)
          throws IngestException {
    UrlEncodedFormEntity entity;
    try {
      entity = new UrlEncodedFormEntity(params);
      HttpPost post = new HttpPost(remoteHostMethod);
      post.setEntity(entity);
      return trustedHttpClient.execute(post, workflowInstanceResponseHandler);
    } catch (UnsupportedEncodingException e) {
      throw new IngestException(e);
    } catch (TrustedHttpClientException e) {
      throw new IngestException(e);
    }
  }

  protected WorkflowInstance ingestMediaPackage(String remoteHostMethod, MultipartEntity mpEntity)
          throws TrustedHttpClientException {
    HttpPost httppost = new HttpPost(remoteHostMethod);
    httppost.setEntity(mpEntity);
    return trustedHttpClient.execute(httppost, workflowInstanceResponseHandler);
  }

  // ------------------- response handlers ------------------
  class MediaPackageResponseHandler implements ResponseHandler<MediaPackage> {
    public MediaPackage handleResponse(final HttpResponse response) throws HttpResponseException, IOException {
      StatusLine statusLine = response.getStatusLine();
      if (statusLine.getStatusCode() == 404) {
        return null;
      } else if (statusLine.getStatusCode() >= 300) {
        throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
      }
      HttpEntity entity = response.getEntity();
      try {
        return entity == null ? null : mpBuilder.loadFromXml(entity.getContent());
      } catch (MediaPackageException e) {
        throw new IOException(e);
      } finally {
        trustedHttpClient.close(response);
      }
    }
  }

  class WorkflowInstanceResponseHandler implements ResponseHandler<WorkflowInstance> {
    public WorkflowInstance handleResponse(final HttpResponse response) throws HttpResponseException, IOException {
      StatusLine statusLine = response.getStatusLine();
      if (statusLine.getStatusCode() == 404) {
        return null;
      } else if (statusLine.getStatusCode() >= 300) {
        throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
      }
      HttpEntity entity = response.getEntity();
      try {
        return entity == null ? null : wfBuilder.parseWorkflowInstance(entity.getContent());
      } catch (Exception e) {
        throw new IOException(e);
      } finally {
        trustedHttpClient.close(response);
      }
    }
  }

}
