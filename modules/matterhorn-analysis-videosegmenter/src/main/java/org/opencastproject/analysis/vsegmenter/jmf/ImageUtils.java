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

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.media.Buffer;

/**
 * A collection of utility methods used to deal with frame buffers and images.
 */
public class ImageUtils {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ImageUtils.class);

  /**
   * Returns <code>true</code> if <code>image</code> differs from <code>currentImage</code>. In order to be treated a
   * different image, the <code>rgb</code> values of at least <code>changesThreshold</code> pixels must have changed.
   * 
   * @param currentImage
   *          the previous image
   * @param image
   *          the new image
   * @param changesThreshold
   *          the number of pixels that need to change
   * 
   * @return <code>true</code> if a new scene has been detected
   */
  public static boolean isDifferent(BufferedImage currentImage, BufferedImage image, float changesThreshold) {
    boolean differsFromCurrentScene = false;

    if (currentImage == null) {
      differsFromCurrentScene = true;
      logger.debug("First segment started");
    } else if (currentImage.getWidth() != image.getWidth() || currentImage.getHeight() != image.getHeight()) {
      differsFromCurrentScene = true;
      String currentResolution = currentImage.getWidth() + "x" + currentImage.getHeight();
      String newResolution = image.getWidth() + "x" + image.getHeight();
      logger.warn("Resolution change detected ({} -> {})", currentResolution, newResolution);
    } else {
      int changes = 0;
      long pixels = image.getWidth() * image.getHeight();
      long changesThresholdPixels = (long)(pixels * changesThreshold);
      imagecomparison: for (int x = 0; x < image.getWidth(); x++) {
        for (int y = 0; y < image.getHeight(); y++) {
          if (image.getRGB(x, y) != currentImage.getRGB(x, y)) {
//            if (image.getRGB(x, y) == -16777216)
//              logger.info("Looks like a no-signal image");
            changes++;
            if (changes > changesThresholdPixels) {
              logger.debug("Found more than {} changes", changesThresholdPixels);
              differsFromCurrentScene = true;
              break imagecomparison;
            }
          }
        }
      }
      float percentage = ((float)changes)/((float)pixels);
      logger.debug("Found {}% changes to the previous frame", percentage);
    }

    return differsFromCurrentScene;
  }

  /**
   * Convers the frame buffer to a <code>BufferedImage</code>. This method returns <code>null</code> if the buffer
   * couldn't be created
   * 
   * @param buf
   *          the buffer
   * @return a <code>BufferedImage</code>
   */
  public static BufferedImage createImage(Buffer buf) throws IOException {
    InputStream is = new ByteArrayInputStream((byte[])buf.getData());
    BufferedImage bufferedImage = ImageIO.read(new MemoryCacheImageInputStream(is));
    return bufferedImage;
  }

  /**
   * Writes the image to disk.
   * 
   * @param image
   *          the image
   */
  public static void saveImage(BufferedImage image, File file) throws IOException {
    file.getParentFile().mkdirs();
    FileUtils.deleteQuietly(file);
    ImageIO.write(image, "jpg", file);
  }

}
