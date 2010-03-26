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
import org.opencastproject.analysis.api.MediaAnalysisFlavor;
import org.opencastproject.analysis.api.MediaAnalysisServiceSupport;
import org.opencastproject.metadata.mpeg7.Mpeg7Catalog;
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogImpl;
import org.opencastproject.workspace.api.Workspace;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;

import javax.media.Buffer;
import javax.media.Manager;
import javax.media.Player;
import javax.media.control.FrameGrabbingControl;
import javax.media.control.FramePositioningControl;
import javax.media.format.VideoFormat;
import javax.media.util.BufferToImage;

/**
 * Media analysis plugin that takes a video stream and extracts video segments by trying to detect slide and/or scene
 * changes.
 */
public class VideoSegmenter extends MediaAnalysisServiceSupport  {

  /** The local workspace */
  private Workspace workspace = null;

  /**
   * Creates a new video segmenter.
   */
  protected VideoSegmenter() {
    super(MediaAnalysisFlavor.SEGMENTS_FLAVOR);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.analysis.api.MediaAnalysisService#analyze(java.net.URL)
   */
  @Override
  public Mpeg7Catalog analyze(URL mediaUrl) throws MediaAnalysisException {
    Mpeg7Catalog mpeg7 = Mpeg7CatalogImpl.newInstance();
    try {
      File trackFile = workspace.get(mediaUrl.toURI());

      // Create a player
      Player p = Manager.createRealizedPlayer(trackFile.toURI().toURL());

      // Create a frame positioner
      FramePositioningControl fpc = (FramePositioningControl) p.getControl("javax.media.control.FramePositioningControl");

      // Create a frame grabber
      FrameGrabbingControl fg = (FrameGrabbingControl) p.getControl("javax.media.control.FrameGrabbingControl");

      // Request that the player changes to a 'prefetched' state
      p.prefetch();

      // Wait until the player is in that state...
      fpc.seek(0);

      // Take a snap of the current frame
      Buffer buf = fg.grabFrame();

      // Get its video format details
      VideoFormat vf = (VideoFormat) buf.getFormat();

      // Initialize BufferToImage with video format
      BufferToImage bufferToImage = new BufferToImage(vf);

      // Convert the buffer to an image
      Image im = bufferToImage.createImage(buf);
      
      // We need a BufferedImage. Chances are that we already have one
      BufferedImage bufferedImage = null;
      if (im instanceof BufferedImage) {
        bufferedImage = (BufferedImage)im;
      } else {
        Dimension d = vf.getSize();
        bufferedImage = new BufferedImage((int)d.getWidth(), (int)d.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        bufferedImage.getGraphics().drawImage(im, 0, 0, null);
      }
      
      // TODO: detect differences to previous frame
      //EdgeDetector edgeDetector = new EdgeDetector();
      //edgeDetector.setSourceImage(bufferedImage);
      
      // TODO: configure edge detector
      //edgeDetector.process();
      
      //BufferedImage edgeImage = edgeDetector.getEdgesImage();

    } catch (Exception e) {
      throw new MediaAnalysisException(e);
    }
 
    return mpeg7;
  }

  /**
   * Callback for declarative services configuration that will introduce us to the local workspace service.
   * Implementation assumes that the reference is configured as being static.
   * 
   * @param workspace
   *          an instance of the workspace
   */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

}
