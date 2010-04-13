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
package org.opencastproject.analysis.vsegmenter.jmf;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.media.Buffer;
import javax.media.Format;
import javax.media.format.RGBFormat;
import javax.media.format.VideoFormat;
import javax.media.util.BufferToImage;

/**
 * A collection of utility methods used to deal with frame buffers and images.
 */
public class ImageUtils {

  /**
   * Convers the frame buffer to a <code>BufferedImage</code>. This method returns <code>null</code> if the buffer
   * couldn't be created
   * 
   * @param buf
   *          the buffer
   * @return a <code>BufferedImage</code>
   */
  public static BufferedImage createImage(Buffer buf) {
    VideoFormat vf = (VideoFormat) buf.getFormat();
    Dimension resolution = vf.getSize();

    int width = (int) resolution.getWidth();
    int height = (int) resolution.getHeight();
    int maxDataLength = vf.getMaxDataLength();

    RGBFormat rgbFormat = new RGBFormat(
      resolution,
      maxDataLength,
      Format.byteArray,
      0,
      Format.NOT_SPECIFIED, // TODO: Figure out data size
      0xFF0000,
      0xFF00,
      0xFF,
      1,
      width,
      VideoFormat.FALSE,
      Format.NOT_SPECIFIED
    );

    buf.setFormat(rgbFormat);
    buf.setSequenceNumber(0);
    buf.setTimeStamp(0);

    // Convert the buffer to an image
    BufferToImage bufferToImage = new BufferToImage(rgbFormat);
    Image im = bufferToImage.createImage(buf);

    // We need a BufferedImage. Chances are that we already have one
    BufferedImage bufferedImage = null;
    if (im instanceof BufferedImage) {
      bufferedImage = (BufferedImage) im;
    } else {
      bufferedImage = new BufferedImage((int) width, height, BufferedImage.TYPE_3BYTE_BGR);
      bufferedImage.getGraphics().drawImage(im, 0, 0, null);
    }

    return bufferedImage;
  }

}
