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
import org.opencastproject.analysis.vsegmenter.jmf.ImageUtils;
import org.opencastproject.analysis.vsegmenter.jmf.PlayerListener;
import org.opencastproject.metadata.mpeg7.ContentSegment;
import org.opencastproject.metadata.mpeg7.MediaLocator;
import org.opencastproject.metadata.mpeg7.MediaLocatorImpl;
import org.opencastproject.metadata.mpeg7.MediaTime;
import org.opencastproject.metadata.mpeg7.MediaTimeImpl;
import org.opencastproject.metadata.mpeg7.Mpeg7Catalog;
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogImpl;
import org.opencastproject.metadata.mpeg7.Video;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.media.Buffer;
import javax.media.Controller;
import javax.media.Duration;
import javax.media.Manager;
import javax.media.Player;
import javax.media.Time;
import javax.media.control.FrameGrabbingControl;
import javax.media.control.FramePositioningControl;
import javax.media.protocol.DataSource;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Media analysis plugin that takes a video stream and extracts video segments by trying to detect slide and/or scene
 * changes.
 * 
 * Videos that can be used by this segmenter need to be created using this commandline:
 * 
 * <pre>
 * ffmpeg -i &lt;inputfile&gt; -deinterlace -r 1 -vcodec mjpeg -qscale 1 -an &lt;outputfile&gt;
 * </pre>
 */
public class VideoSegmenter extends MediaAnalysisServiceSupport {

  /** Constant used to retreive the frame positioning control */
  public static final String FRAME_POSITIONING = "javax.media.control.FramePositioningControl";

  /** Constant used to retreive the frame grabbing control */
  public static final String FRAME_GRABBING = "javax.media.control.FrameGrabbingControl";

  /** The number of frames that need to resemble until a scene is considered "stable" */
  public static final int STABILITY_THRESHOLD = 10;

  /** Number of pixels that may change between two frames without considering them different */
  public static final int CHANGES_THRESHOLD = 1000;

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(VideoSegmenter.class);

  /** The buffered edge image of the current scene */
  private BufferedImage currentImage = null;

  /** The number of stable images for the current scene */
  private int currentSceneStabilityCount = 0;

  /** Listener used to control the player state */
  private PlayerListener playerListener = null;

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
      // Create a player
      DataSource ds = Manager.createDataSource(mediaUrl);
      Player player = Manager.createRealizedPlayer(ds);
      playerListener = new PlayerListener(player);

      // Create a frame positioner
      FramePositioningControl fpc = (FramePositioningControl) player.getControl(FRAME_POSITIONING);
      if (fpc == null)
        throw new MediaAnalysisException("Unable to create a frame positioning control for " + mediaUrl);

      // Create a frame grabber
      FrameGrabbingControl fg = (FrameGrabbingControl) player.getControl(FRAME_GRABBING);
      if (fg == null)
        throw new MediaAnalysisException("Unable to create a frame grabber for " + mediaUrl);

      // Load the movie and change the player to prefetched state
      player.prefetch();
      if (!playerListener.waitForState(Controller.Prefetched)) {
        throw new MediaAnalysisException("Unable to switch player into 'prefetch' state");
      }

      Time duration = player.getDuration();
      int totalFrames = -1;

      // Get the movie duration
      if (duration != Duration.DURATION_UNKNOWN) {
        totalFrames = fpc.mapTimeToFrame(duration);
        if (totalFrames != FramePositioningControl.FRAME_UNKNOWN)
          logger.debug("Total # of video frames in the movies: " + totalFrames);
        else
          logger.warn("The FramePositiongControl does not support mapTimeToFrame.");
      } else {
        throw new MediaAnalysisException("Java media framework is unable to detect movie duration");
      }

      logger.info("Track {} with {} frames loaded", mediaUrl, totalFrames);

      MediaTime contentTime = new MediaTimeImpl(0, (long) duration.getSeconds() * 1000);
      MediaLocator contentLocator = new MediaLocatorImpl(mediaUrl.toURI());
      Video videoContent = mpeg7.addVideoContent("videosegment", contentTime, contentLocator);
      int segmentCount = 0;

      ContentSegment contentSegment = videoContent.getTemporalDecomposition().createSegment("segment-" + segmentCount);

      EdgeDetector edgeDetector = new EdgeDetector();
      // TODO: configure edge detector

      int t = 1;
      int lastKnownTimestamp = -1;
      long startOfSegment = 0;
      boolean approachingSceneChange = false;

      while (t < duration.getSeconds()) {

        // Move the positioning control to the correct time
        fpc.seek(t);

        // Take a snap of the current frame
        Buffer buf = fg.grabFrame();
        buf.setTimeStamp(t);

        BufferedImage bufferedImage = ImageUtils.createImage(buf);
        if (bufferedImage == null)
          throw new MediaAnalysisException("Unable to extract image at time " + t);

        // Let the edge detector do it's work (force it to create a new image for the result)
        edgeDetector.setSourceImage(bufferedImage);
        edgeDetector.setEdgesImage(null);
        logger.debug("Running edge detection on image at {} seconds", t);
        // edgeDetector.process();

        // // Get the edge image and store it.
        // BufferedImage edgeImage = edgeDetector.getEdgesImage();

        logger.debug("Inspecting movie at {} seconds", t);

        // If things look stable, then let's look ahead as much as possible whithout loosing information (which is equal
        // to looking ahead STABILITY_THRESHOLD seconds.

        if (isNewScene(bufferedImage, STABILITY_THRESHOLD)) {
          // Did we try a lucky punch and look ahead?
          if (t - lastKnownTimestamp > 1) {
            t = lastKnownTimestamp + 1;
            approachingSceneChange = true;
          } else {
            segmentCount++;
            approachingSceneChange = false;
            lastKnownTimestamp = t;

            contentSegment.setMediaTime(new MediaTimeImpl(startOfSegment, t - startOfSegment));
            logger.info("Found new scene at frame {}", startOfSegment);
            t++;

            // Create a new segment if this wasn't the first one
            if (startOfSegment > 0) {
              contentSegment = videoContent.getTemporalDecomposition().createSegment("segment-" + segmentCount);
              startOfSegment = t;
            }

          }
        } else if (approachingSceneChange || t < STABILITY_THRESHOLD) {
          lastKnownTimestamp = t;
          t++;
        } else {
          t += STABILITY_THRESHOLD;
        }

      }

      // Finish off the last segment
      contentSegment.setMediaTime(new MediaTimeImpl(startOfSegment, (long) duration.getSeconds() - startOfSegment));

      logger.info("Finished video segmentation of {}", mediaUrl);

    } catch (Exception e) {
      throw new MediaAnalysisException(e);
    }

    return mpeg7;
  }

  /**
   * Returns <code>true</code> if <code>image</code> is the same in a row of <code>stabilityThreshold</code> images.
   * 
   * @param image
   *          the image
   * @param stabilityThreshold
   *          the number of images that need to resemble in order to call it stable
   * 
   * @return <code>true</code> if a new scene has been detected
   */
  private boolean isNewScene(BufferedImage image, int stabilityThreshold) {
    boolean differsFromCurrentScene = false;

    if (currentImage == null) {
      differsFromCurrentScene = true;
      logger.debug("First segment started");
    } else if (currentImage.getWidth() != image.getWidth() || currentImage.getHeight() != image.getHeight()) {
      differsFromCurrentScene = true;
      logger.warn("Resolution change detected ({},{}) -> ({},{})", new Object[] { currentImage.getWidth(),
              currentImage.getHeight(), image.getWidth(), image.getHeight() });
    } else {
      int changes = 0;
      imagecomparison: for (int x = 0; x < image.getWidth(); x++) {
        for (int y = 0; y < image.getHeight(); y++) {
          if (image.getRGB(x, y) != currentImage.getRGB(x, y)) {
            differsFromCurrentScene = true;
            changes++;
            if (changes > CHANGES_THRESHOLD) {
              logger.debug("Found more than {} changes between the last frames", CHANGES_THRESHOLD);
              break imagecomparison;
            }
          }
        }
        logger.debug("Found {} changes to the previous frame", changes);
      }
    }

    if (!differsFromCurrentScene) {
      if (currentSceneStabilityCount == stabilityThreshold) {
        return false;
      } else {
        currentSceneStabilityCount++;
        if (currentSceneStabilityCount == stabilityThreshold) {
          return true;
        }
      }
    } else {
      currentSceneStabilityCount = 1;
      logger.debug("Found image in new scene");
    }

    currentImage = image;
    return false;
  }

  /**
   * Main method for testing purposes.
   * 
   * @param args
   *          the arguments
   */
  public static void main(String[] args) {
    if (args.length < 1) {
      System.err.println("Please provide a media url");
      System.exit(1);
    }

    String mediaFile = args[0];
    URL mediaUrl = null;
    try {
      mediaUrl = new URL(mediaFile);
    } catch (MalformedURLException e) {
      System.err.println("Malformed media url: " + mediaFile);
      System.exit(1);
    }

    Mpeg7Catalog catalog = null;
    try {
      VideoSegmenter vsegmenter = new VideoSegmenter();
      catalog = vsegmenter.analyze(mediaUrl);
    } catch (MediaAnalysisException e) {
      System.err.println("Error on videosegmentation: " + e.getMessage());
      e.printStackTrace(System.err);
      System.exit(1);
    }

    // Dump the resulting catalog to stdout
    if (catalog != null) {
      try {
        Transformer t = TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        t.transform(new DOMSource(catalog.toXml()), new StreamResult(System.out));
      } catch (TransformerException e) {
        e.printStackTrace();
      } catch (ParserConfigurationException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

  }

}
