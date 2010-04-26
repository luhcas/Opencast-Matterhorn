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

  /** The number of seconds that need to resemble until a scene is considered "stable" */
  public static final int STABILITY_THRESHOLD = 5;

  /** Number of pixels that may change between two frames without considering them different */
  public static final int CHANGES_THRESHOLD = 57600; // 75% change considering 320*240
  
  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(VideoSegmenter.class);

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

    BufferedImage previousImage = null;
    int currentSceneStabilityCount = 0;
    PlayerListener playerListener = null;

    Mpeg7Catalog mpeg7 = Mpeg7CatalogImpl.newInstance();

    try {
      // Create a player
      DataSource ds = Manager.createDataSource(mediaUrl);
      Player player = null;
      try {
        player = Manager.createRealizedPlayer(ds);
        playerListener = new PlayerListener(player);
        player.addControllerListener(playerListener);
      } catch (Exception e) {
        throw new MediaAnalysisException(e);
      }

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

      // Get the movie duration
      if (duration == Duration.DURATION_UNKNOWN) {
        throw new MediaAnalysisException("Java media framework is unable to detect movie duration");
      }

      logger.info("Track {} loaded, duration is {} s", mediaUrl, duration.getSeconds());

      MediaTime contentTime = new MediaTimeImpl(0, (long) duration.getSeconds() * 1000);
      MediaLocator contentLocator = new MediaLocatorImpl(mediaUrl.toURI());
      Video videoContent = mpeg7.addVideoContent("videosegment", contentTime, contentLocator);
      int segmentCount = 0;

      ContentSegment contentSegment = videoContent.getTemporalDecomposition().createSegment("segment-" + segmentCount);

//      EdgeDetector edgeDetector = new EdgeDetector();
      // TODO: configure edge detector

      int t = 1;
      int lastKnownTimestamp = 0;
      long startOfSegment = 0;
      boolean sceneChangeImminent = false;

      logger.info("Starting video segmentation of {}", mediaUrl);

      while (t < duration.getSeconds()) {

        // Move the positioning control to the correct time
        fpc.seek(t);

        // Take a snap of the current frame
        Buffer buf = fg.grabFrame();
        BufferedImage bufferedImage = ImageUtils.createImage(buf);
        if (bufferedImage == null) {
          throw new MediaAnalysisException("Unable to extract image at time " + t);
        }
        
        logger.trace("Analyzing video at {} s", t);

        // Let the edge detector do it's work (force it to create a new image for the result)
        logger.debug("Running edge detection on image at {} seconds", t);
//        edgeDetector.setSourceImage(bufferedImage);
//        edgeDetector.setEdgesImage(null);
//        BufferedImage edgeImage = edgeDetector.process();

        // Compare the new image with our previous sample
        boolean differsFromPreviousImage = ImageUtils.isDifferent(previousImage, bufferedImage, CHANGES_THRESHOLD);

        // We found an image that is different compared to the previous one. Let's see if this image remains stable
        // for some time (STABILITY_THRESHOLD) so we can declare a new scene
        if (differsFromPreviousImage) {
          logger.debug("Found differing image at {} seconds", t);

          // If this is the result of a lucky punch (looking ahead STABILITY_THRESHOLD seconds), then we should
          // really start over an make sure we get the correct beginning of the new scene
          if (t - lastKnownTimestamp > 1) {
            t = lastKnownTimestamp;
            sceneChangeImminent = true;
          } else {
            lastKnownTimestamp = t - 1;
            currentSceneStabilityCount = 0;
            previousImage = bufferedImage;
            t++;
          }

          logger.debug("Found image in new scene");
        }

        // Seems to be the same image. If we have just recently detected a new scene, let's see if we are able to
        // confirm that this is scene is stable (>= STABILITY_THRESHOLD)
        else {
          
          if (currentSceneStabilityCount < STABILITY_THRESHOLD) {
            currentSceneStabilityCount++;
            
            // Did we find a new scene?
            if (currentSceneStabilityCount == STABILITY_THRESHOLD) {
              segmentCount++;
              lastKnownTimestamp = t;

              long endOfSegment = t - STABILITY_THRESHOLD;
              long durationms = (endOfSegment - startOfSegment)*1000L;
              
              // Create a new segment if this wasn't the first one
              if (endOfSegment > 1) {
                contentSegment.setMediaTime(new MediaTimeImpl(startOfSegment*1000L, durationms));
                contentSegment = videoContent.getTemporalDecomposition().createSegment("segment-" + segmentCount);
                startOfSegment = endOfSegment;
              }
              
              t += STABILITY_THRESHOLD;
              sceneChangeImminent = false;

              logger.info("Found new scene at {} s", startOfSegment);
            } else {
              t++;
            }

          } else if (sceneChangeImminent) {
            // We found a scene change by looking ahead. Now we want to get to the exact position
            lastKnownTimestamp = t;
            t ++;
          } else {
            // If things look stable, then let's look ahead as much as possible whithout loosing information (which is
            // equal to looking ahead STABILITY_THRESHOLD seconds.
            lastKnownTimestamp = t;
            t += STABILITY_THRESHOLD;
          }

          previousImage = bufferedImage;
        }

      }

      // Finish off the last segment
      long startOfSegmentms = startOfSegment*1000L;
      long durationms = ((long) duration.getSeconds() - startOfSegment)*1000;
      contentSegment.setMediaTime(new MediaTimeImpl(startOfSegmentms, durationms));

      logger.info("Finished video segmentation of {}", mediaUrl);

    } catch (Exception e) {
      throw new MediaAnalysisException(e);
    }

    return mpeg7;
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
