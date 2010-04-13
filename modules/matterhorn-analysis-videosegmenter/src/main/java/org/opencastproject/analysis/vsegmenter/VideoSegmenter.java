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
import org.opencastproject.metadata.mpeg7.Mpeg7Catalog;
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogImpl;
import org.opencastproject.workspace.api.NotFoundException;
import org.opencastproject.workspace.api.Workspace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import javax.media.Buffer;
import javax.media.Controller;
import javax.media.Duration;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.Player;
import javax.media.Time;
import javax.media.control.FrameGrabbingControl;
import javax.media.control.FramePositioningControl;
import javax.xml.parsers.ParserConfigurationException;
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
 * <pre>
 * ffmpeg -i &lt;inputfile&gt; -deinterlace -r 1 -vcodec mjpeg -qscale 1 &lt;outputfile&gt;
 * </pre>
 */
public class VideoSegmenter extends MediaAnalysisServiceSupport {

  /** Constant used to retreive the frame positioning control */
  public static final String FRAME_POSITIONING = "javax.media.control.FramePositioningControl";

  /** Constant used to retreive the frame grabbing control */
  public static final String FRAME_GRABBING = "javax.media.control.FrameGrabbingControl";

  /** The number of frames that need to resemble until a scene is considered "stable" */
  public static final int STABILITY_THRESHOLD = 10;

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(VideoSegmenter.class);

  /** The local workspace */
  private Workspace workspace = null;

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
      File trackFile = workspace.get(mediaUrl.toURI());
      trackFile = new File("/Users/wunden/Desktop/slides-vga-mjpeg.mov");

      // Create a player
      Player player = Manager.createRealizedPlayer(new MediaLocator(trackFile.toURI().toURL()));
      playerListener = new PlayerListener(player);

      // Create a frame positioner
      FramePositioningControl fpc = (FramePositioningControl) player.getControl(FRAME_POSITIONING);

      // Create a frame grabber
      FrameGrabbingControl fg = (FrameGrabbingControl) player.getControl(FRAME_GRABBING);

      // Load the movie and change the player to prefetched state
      player.prefetch();
      if (!playerListener.waitForState(Controller.Prefetched)) {
        throw new MediaAnalysisException("Unable to switch player into 'prefetch' state");
      }

      logger.info("MJPEG track {} loaded", trackFile);

      Time duration = player.getDuration();
      int totalFrames = -1;

      // Get the movie duration
      if (duration != Duration.DURATION_UNKNOWN) {
        System.out.println("Movie duration: " + duration.getSeconds());
        totalFrames = fpc.mapTimeToFrame(duration);
        if (totalFrames != FramePositioningControl.FRAME_UNKNOWN)
          logger.debug("Total # of video frames in the movies: " + totalFrames);
        else
          logger.warn("The FramePositiongControl does not support mapTimeToFrame.");
      } else {
        throw new MediaAnalysisException("Java media framework is unable to detect movie duration");
      }

      EdgeDetector edgeDetector = new EdgeDetector();
      // TODO: configure edge detector

      int t = 1;
      int lastKnownTimestamp = -1;
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
        edgeDetector.process();

        // // Get the edge image and store it.
        BufferedImage edgeImage = edgeDetector.getEdgesImage();

        logger.debug("Inspecting movie at {} seconds", t);

        // If things look stable, then let's look ahead as much as possible whithout loosing information (which is equal
        // to looking ahead STABILITY_THRESHOLD seconds.

        if (isNewScene(edgeImage, STABILITY_THRESHOLD)) {
          // Did we try a lucky punch and look ahead?
          if (t - lastKnownTimestamp > 1) {
            t = lastKnownTimestamp + 1;
            approachingSceneChange = true;
          } else {
            approachingSceneChange = false;
            lastKnownTimestamp = t;
            t++;
            logger.info("Found new scene at {} seconds", t);
          }
        } else if (approachingSceneChange || t < STABILITY_THRESHOLD) {
          lastKnownTimestamp = t;
          t++;
        } else {
          t += STABILITY_THRESHOLD;
        }

      }

      logger.info("Finished video segmentation");

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
      logger.info("Found first scene");
    } else if (currentImage.getWidth() != image.getWidth() || currentImage.getHeight() != image.getHeight()) {
      differsFromCurrentScene = true;
      logger.warn("Resolution change detected ({},{}) -> ({},{})", new Object[] { currentImage.getWidth(),
              currentImage.getHeight(), image.getWidth(), image.getHeight() });
    } else {
      // TODO: Improve this. One pixel difference should not lead to a new scene
      imagecomparison: for (int x = 0; x < image.getWidth(); x++) {
        for (int y = 0; y < image.getHeight(); y++) {
          if (image.getRGB(x, y) != currentImage.getRGB(x, y)) {
            differsFromCurrentScene = true;
            break imagecomparison;
          }
        }
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
      logger.info("Found image in new scene");
    }

    currentImage = image;
    return false;
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
      vsegmenter.workspace = new Workspace() {
        public void delete(String mediaPackageID, String mediaPackageElementID) throws NotFoundException {
        }
        public File get(URI uri) throws NotFoundException {
          return new File(uri);
        }
        public URI getURI(String mediaPackageID, String mediaPackageElementID) throws NotFoundException {
          return null;
        }
        public URI put(String mediaPackageID, String mediaPackageElementID, String fileName, InputStream in) {
          return null;
        }
      };
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
