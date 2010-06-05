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
package org.opencastproject.inspection.remote;

import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.media.mediapackage.AbstractMediaPackageElement;
import org.opencastproject.media.mediapackage.MediaPackageElement;
import org.opencastproject.remote.api.Receipt;
import org.opencastproject.remote.api.RemoteServiceManager;
import org.opencastproject.remote.api.Receipt.Status;
import org.opencastproject.security.api.TrustedHttpClient;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Proxies a remote media inspection service for use as a JVM-local service.
 */
public class MediaInspectionServiceRemoteImpl implements MediaInspectionService {
  private static final Logger logger = LoggerFactory.getLogger(MediaInspectionServiceRemoteImpl.class);

  protected ResponseHandler<Receipt> receiptResponseHandler = new ReceiptResponseHandler();
  protected RemoteServiceManager remoteServiceManager;
  protected TrustedHttpClient trustedHttpClient;

  public void setTrustedHttpClient(TrustedHttpClient trustedHttpClient) {
    this.trustedHttpClient = trustedHttpClient;
  }

  public void setRemoteServiceManager(RemoteServiceManager remoteServiceManager) {
    this.remoteServiceManager = remoteServiceManager;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.inspection.api.MediaInspectionService#inspect(java.net.URI, boolean)
   */
  @Override
  public Receipt inspect(URI uri, boolean block) {
    List<String> remoteHosts = remoteServiceManager.getRemoteHosts(JOB_TYPE);
    List<NameValuePair> queryStringParams = new ArrayList<NameValuePair>();
    queryStringParams.add(new BasicNameValuePair("uri", uri.toString()));

    for(String remoteHost : remoteHosts) {
      String url = remoteHost + "/inspection/rest/inspect?" + URLEncodedUtils.format(queryStringParams, "UTF-8");
      logger.info("Inspecting a Track(" + uri + ") on a remote server: " + remoteHost);
      HttpGet get = new HttpGet(url);
      try {
        HttpResponse response = trustedHttpClient.execute(get);
        StatusLine statusLine = response.getStatusLine();
        if(statusLine.getStatusCode() != HttpStatus.SC_OK) {
          logger.warn("{} returned http status '{}'", url, statusLine.getStatusCode());
          continue;
        }
        Receipt receipt = receiptResponseHandler.handleResponse(response);
        if(receipt == null) {
          logger.warn("{} returned no entity", url);
          continue;
        }
        if(block) {
          receipt = poll(receipt.getId());
        }
        return receipt;
      } catch (Exception e) {
        logger.warn("POST to {} returned exception '{}'", url, e);
        continue;
      }
    }
    throw new RuntimeException("Unable to inspect " + uri + " on any of " + remoteHosts.size() + " remote services");
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.inspection.api.MediaInspectionService#enrich(org.opencastproject.media.mediapackage.AbstractMediaPackageElement,
   *      boolean, boolean)
   */
  @Override
  public Receipt enrich(MediaPackageElement original, boolean override, boolean block) {
    List<String> remoteHosts = remoteServiceManager.getRemoteHosts(JOB_TYPE);
    List<NameValuePair> params = new ArrayList<NameValuePair>();
    try {
      params.add(new BasicNameValuePair("mediaPackageElement", ((AbstractMediaPackageElement)original).getAsXml()));
    } catch (Exception e1) {
      throw new RuntimeException(e1);
    }
    params.add(new BasicNameValuePair("override", new Boolean(override).toString()));
    for(String remoteHost : remoteHosts) {
      logger.info("Enriching a Track(" + original.getIdentifier() + ") on a remote server: " + remoteHost);
      String url = remoteHost + "/inspection/rest/enrich";
      try {
        HttpPost post = new HttpPost(url);
        HttpEntity entity = new UrlEncodedFormEntity(params);
        post.setEntity(entity);
        HttpResponse response = trustedHttpClient.execute(post);
        StatusLine statusLine = response.getStatusLine();
        if(statusLine.getStatusCode() != HttpStatus.SC_OK) {
          logger.warn("{} returned http status '{}'", url, statusLine.getStatusCode());
          continue;
        }
        Receipt receipt = receiptResponseHandler.handleResponse(response);
        if(receipt == null) {
          logger.warn("{} returned no entity", url);
          continue;
        }
        if(block) {
          receipt = poll(receipt.getId());
        }
        return receipt;
      } catch (Exception e) {
        logger.warn("POST to {} returned exception '{}'", url, e);
        continue;
      }
    }
    throw new RuntimeException("Unable to enrich " + original + " on any of " + remoteHosts.size() + " remote services");
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
  
  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.inspection.api.MediaInspectionService#getReceipt(java.lang.String)
   */
  @Override
  public Receipt getReceipt(String id) {
    List<String> remoteHosts = remoteServiceManager.getRemoteHosts(JOB_TYPE);
    for(String remoteHost : remoteHosts) {
      logger.debug("Returning a Receipt(" + id + ") from a remote server: " + remoteHost);
      String url = remoteHost + "/inspection/rest/receipt/" + id + ".xml";
      HttpGet get = new HttpGet(url);
      try {
        HttpResponse response = trustedHttpClient.execute(get);
        StatusLine statusLine = response.getStatusLine();
        if(statusLine.getStatusCode() != HttpStatus.SC_OK) {
          logger.warn("{} returned http status '{}'", url, statusLine.getStatusCode());
          continue;
        }
        Receipt receipt = receiptResponseHandler.handleResponse(response);
        if(receipt == null) {
          logger.warn("{} returned no entity", url);
          continue;
        }
        return receipt;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    return null;
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

}
