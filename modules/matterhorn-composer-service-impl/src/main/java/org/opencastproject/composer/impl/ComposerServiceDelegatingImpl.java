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
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageException;

import org.osgi.service.component.ComponentContext;

/**
 * Delegates composer methods to either the local composer service impl, or to a remote service.  If a "composer.remote"
 * property is provided during activation, the composer service at that URL will be used.
 */
public class ComposerServiceDelegatingImpl implements ComposerService {

  /**
   * The composer service handling the actual work.
   */
  ComposerService delegate;
  
  /**
   * The local composer service implementation
   */
  ComposerServiceImpl local;
  
  /**
   * @param local the local to set
   */
  public void setLocal(ComposerServiceImpl local) {
    this.local = local;
  }

  /**
   * The remote composer service implementation
   */
  ComposerServiceRemoteImpl remote;
  
  /**
   * @param remote the remote to set
   */
  public void setRemote(ComposerServiceRemoteImpl remote) {
    this.remote = remote;
  }

  public void activate(ComponentContext cc) {
    String remoteHost = cc.getBundleContext().getProperty(ComposerServiceRemoteImpl.REMOTE_COMPOSER);
    if(remoteHost == null) {
      delegate = local;
    } else {
      delegate = remote;
    }
  }

  /**
   * @param status
   * @param host
   * @return
   * @see org.opencastproject.composer.api.ComposerService#countJobs(org.opencastproject.composer.api.Receipt.Status, java.lang.String)
   */
  public long countJobs(Status status, String host) {
    return delegate.countJobs(status, host);
  }

  /**
   * @param status
   * @return
   * @see org.opencastproject.composer.api.ComposerService#countJobs(org.opencastproject.composer.api.Receipt.Status)
   */
  public long countJobs(Status status) {
    return delegate.countJobs(status);
  }

  /**
   * @param mediaPackage
   * @param sourceTrackId
   * @param profileId
   * @param block
   * @return
   * @throws EncoderException
   * @throws MediaPackageException
   * @see org.opencastproject.composer.api.ComposerService#encode(org.opencastproject.media.mediapackage.MediaPackage, java.lang.String, java.lang.String, boolean)
   */
  public Receipt encode(MediaPackage mediaPackage, String sourceTrackId, String profileId, boolean block)
          throws EncoderException, MediaPackageException {
    return delegate.encode(mediaPackage, sourceTrackId, profileId, block);
  }

  /**
   * @param mediaPackage
   * @param sourceVideoTrackId
   * @param sourceAudioTrackId
   * @param profileId
   * @param block
   * @return
   * @throws EncoderException
   * @throws MediaPackageException
   * @see org.opencastproject.composer.api.ComposerService#encode(org.opencastproject.media.mediapackage.MediaPackage, java.lang.String, java.lang.String, java.lang.String, boolean)
   */
  public Receipt encode(MediaPackage mediaPackage, String sourceVideoTrackId, String sourceAudioTrackId,
          String profileId, boolean block) throws EncoderException, MediaPackageException {
    return delegate.encode(mediaPackage, sourceVideoTrackId, sourceAudioTrackId, profileId, block);
  }

  /**
   * @param mediaPackage
   * @param sourceVideoTrackId
   * @param sourceAudioTrackId
   * @param profileId
   * @return
   * @throws EncoderException
   * @throws MediaPackageException
   * @see org.opencastproject.composer.api.ComposerService#encode(org.opencastproject.media.mediapackage.MediaPackage, java.lang.String, java.lang.String, java.lang.String)
   */
  public Receipt encode(MediaPackage mediaPackage, String sourceVideoTrackId, String sourceAudioTrackId,
          String profileId) throws EncoderException, MediaPackageException {
    return delegate.encode(mediaPackage, sourceVideoTrackId, sourceAudioTrackId, profileId);
  }

  /**
   * @param mediaPackage
   * @param sourceTrackId
   * @param profileId
   * @return
   * @throws EncoderException
   * @throws MediaPackageException
   * @see org.opencastproject.composer.api.ComposerService#encode(org.opencastproject.media.mediapackage.MediaPackage, java.lang.String, java.lang.String)
   */
  public Receipt encode(MediaPackage mediaPackage, String sourceTrackId, String profileId) throws EncoderException,
          MediaPackageException {
    return delegate.encode(mediaPackage, sourceTrackId, profileId);
  }

  /**
   * @param profileId
   * @return
   * @see org.opencastproject.composer.api.ComposerService#getProfile(java.lang.String)
   */
  public EncodingProfile getProfile(String profileId) {
    return delegate.getProfile(profileId);
  }

  /**
   * @param id
   * @return
   * @see org.opencastproject.composer.api.ComposerService#getReceipt(java.lang.String)
   */
  public Receipt getReceipt(String id) {
    return delegate.getReceipt(id);
  }

  /**
   * @param mediaPackage
   * @param sourceVideoTrackId
   * @param profileId
   * @param time
   * @param block
   * @return
   * @throws EncoderException
   * @throws MediaPackageException
   * @see org.opencastproject.composer.api.ComposerService#image(org.opencastproject.media.mediapackage.MediaPackage, java.lang.String, java.lang.String, long, boolean)
   */
  public Receipt image(MediaPackage mediaPackage, String sourceVideoTrackId, String profileId, long time, boolean block)
          throws EncoderException, MediaPackageException {
    return delegate.image(mediaPackage, sourceVideoTrackId, profileId, time, block);
  }

  /**
   * @param mediaPackage
   * @param sourceVideoTrackId
   * @param profileId
   * @param time
   * @return
   * @throws EncoderException
   * @throws MediaPackageException
   * @see org.opencastproject.composer.api.ComposerService#image(org.opencastproject.media.mediapackage.MediaPackage, java.lang.String, java.lang.String, long)
   */
  public Receipt image(MediaPackage mediaPackage, String sourceVideoTrackId, String profileId, long time)
          throws EncoderException, MediaPackageException {
    return delegate.image(mediaPackage, sourceVideoTrackId, profileId, time);
  }

  /**
   * @return
   * @see org.opencastproject.composer.api.ComposerService#listProfiles()
   */
  public EncodingProfile[] listProfiles() {
    return delegate.listProfiles();
  }
}
