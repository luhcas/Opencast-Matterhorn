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
package org.opencastproject.analysis.vsegmenter;

import org.opencastproject.analysis.api.MediaAnalysisException;
import org.opencastproject.analysis.api.MediaAnalysisService;
import org.opencastproject.media.mediapackage.MediaPackageElement;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.receipt.api.Receipt;

import org.osgi.service.component.ComponentContext;

/**
 * Delegates MediaAnalysis methods to either the local VideoSegmenter service impl, or to a remote service. If a
 * "remote.videosegmenter" property is provided during activation, the composer service at that URL will be used.
 */
public class VideoSegmenterDelegatingImpl implements MediaAnalysisService {

  /**
   * The composer service handling the actual work.
   */
  MediaAnalysisService delegate;

  /**
   * The local composer service implementation
   */
  MediaAnalysisService local;

  /**
   * @param local
   *          the local to set
   */
  public void setLocal(VideoSegmenter local) {
    this.local = local;
  }

  /**
   * The remote composer service implementation
   */
  VideoSegmenterRemoteImpl remote;

  /**
   * @param remote
   *          the remote to set
   */
  public void setRemote(VideoSegmenterRemoteImpl remote) {
    this.remote = remote;
  }

  public void activate(ComponentContext cc) {
    String remoteHost = cc.getBundleContext().getProperty(VideoSegmenterRemoteImpl.REMOTE_VIDEO_SEGMENTER);
    if (remoteHost == null) {
      delegate = local;
    } else {
      delegate = remote;
    }
  }

  @Override
  public Receipt analyze(MediaPackageElement element, boolean block) throws MediaAnalysisException {
    return delegate.analyze(element, block);
  }

  @Override
  public Receipt getReceipt(String id) {
    return delegate.getReceipt(id);
  }

  @Override
  public MediaPackageElementFlavor produces() {
    return delegate.produces();
  }

  @Override
  public MediaPackageElementFlavor[] requires() {
    return delegate.requires();
  }

}
