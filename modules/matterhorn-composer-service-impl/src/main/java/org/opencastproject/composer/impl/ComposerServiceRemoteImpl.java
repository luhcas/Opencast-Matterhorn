/**
 *  Copyright 2009 The Regents of the University of California
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

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.form.Form;
import org.apache.cxf.jaxrs.ext.xml.XMLSource;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Proxies a remote composer service for use as a JVM-local service.
 */
public class ComposerServiceRemoteImpl implements ComposerService {
  private static final Logger logger = LoggerFactory.getLogger(ComposerServiceRemoteImpl.class);

  public static final String REMOTE_COMPOSER = "remote.composer";

  protected String remoteHost;

  public void setRemoteHost(String remoteHost) {
    this.remoteHost = remoteHost;
  }

  public ComposerServiceRemoteImpl() {}

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
    WebClient client = WebClient.create(remoteHost);
    Long l = client.path("/composer/rest/count").query("status", status).accept("text/plain").get(Long.class);
    return l;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#countJobs(org.opencastproject.composer.api.Receipt.Status,
   *      java.lang.String)
   */
  @Override
  public long countJobs(Status status, String host) {
    WebClient client = WebClient.create(remoteHost);
    Long l = client.path("/composer/rest/count").query("status", status).query("host", host).accept("text/plain").get(
            Long.class);
    return l;
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
    WebClient client = WebClient.create(remoteHost);
    Form form = new Form().set("mediapackage", mediaPackage.toXml()).set("videoSourceTrackId", sourceVideoTrackId).set(
            "audioSourceTrackId", sourceAudioTrackId).set("profileId", profileId);
    Response response = client.path("/composer/rest/encode").accept(MediaType.TEXT_XML).form(form);
    XMLSource xs = new XMLSource((InputStream) response.getEntity());
    Receipt r = xs.getNode("/*", ReceiptImpl.class);

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
    try {
      return WebClient.create(remoteHost).path("/composer/rest/profile/" + profileId + ".xml").accept(
              MediaType.TEXT_XML).get(EncodingProfileImpl.class);
    } catch (WebApplicationException e) {
      if (e.getResponse().getStatus() == 404) {
        return null;
      } else {
        throw e;
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.ComposerService#getReceipt(java.lang.String)
   */
  @Override
  public Receipt getReceipt(String id) {
    try {
      return WebClient.create(remoteHost).path("/composer/rest/receipt/" + id + ".xml").accept(MediaType.TEXT_XML).get(
              ReceiptImpl.class);
    } catch (WebApplicationException e) {
      if (e.getResponse().getStatus() == 404) {
        return null;
      } else {
        throw e;
      }
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
    WebClient client = WebClient.create(remoteHost);
    Form form = new Form().set("mediapackage", mediaPackage.toXml()).set("sourceTrackId", sourceVideoTrackId).set(
            "profileId", profileId).set("time", time);
    Response response = client.path("/encode").accept(MediaType.TEXT_XML).form(form);
    XMLSource xs = new XMLSource((InputStream) response.getEntity());
    Receipt r = xs.getNode("/", ReceiptImpl.class);

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
    EncodingProfileList profileList = WebClient.create(remoteHost).path("/composer/rest/profiles.xml").accept(
            MediaType.TEXT_XML).get(EncodingProfileList.class);
    List<EncodingProfileImpl> list = profileList.getProfile();
    return list.toArray(new EncodingProfile[list.size()]);
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
}
