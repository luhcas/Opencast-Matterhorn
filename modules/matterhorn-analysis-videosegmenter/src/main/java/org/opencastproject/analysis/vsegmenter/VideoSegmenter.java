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
import org.opencastproject.analysis.api.MediaAnalysisServiceSupport;
import org.opencastproject.analysis.vsegmenter.jmf.FrameGrabber;
import org.opencastproject.analysis.vsegmenter.jmf.ImageComparator;
import org.opencastproject.analysis.vsegmenter.jmf.ImageUtils;
import org.opencastproject.analysis.vsegmenter.jmf.PlayerListener;
import org.opencastproject.composer.api.ComposerService;
import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageElement;
import org.opencastproject.media.mediapackage.MediaPackageElements;
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.opencastproject.media.mediapackage.MediaPackageReference;
import org.opencastproject.media.mediapackage.MediaPackageReferenceImpl;
import org.opencastproject.media.mediapackage.Track;
import org.opencastproject.metadata.mpeg7.ContentSegment;
import org.opencastproject.metadata.mpeg7.MediaLocator;
import org.opencastproject.metadata.mpeg7.MediaLocatorImpl;
import org.opencastproject.metadata.mpeg7.MediaRelTimeImpl;
import org.opencastproject.metadata.mpeg7.MediaTime;
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogImpl;
import org.opencastproject.metadata.mpeg7.Video;
import org.opencastproject.receipt.api.Receipt;
import org.opencastproject.receipt.api.ReceiptService;
import org.opencastproject.receipt.api.Receipt.Status;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.workingfilerepository.api.WorkingFileRepository;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.media.Buffer;
import javax.media.Controller;
import javax.media.Duration;
import javax.media.IncompatibleSourceException;
import javax.media.Manager;
import javax.media.Processor;
import javax.media.Time;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
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

  /** Receipt type */
  public static final String RECEIPT_TYPE = "org.opencastproject.analysis.vsegmenter";

  /** Resulting collection in the working file repository */
  public static final String COLLECTION_ID = "vsegmenter";

  /** Constant used to retreive the frame positioning control */
  public static final String FRAME_POSITIONING = "javax.media.control.FramePositioningControl";

  /** Constant used to retreive the frame grabbing control */
  public static final String FRAME_GRABBING = "javax.media.control.FrameGrabbingControl";

  /** Name of the encoding profile that transcodes video tracks into a segmentable format */
  public static final String MJPEG_ENCODING_PROFILE = "mjpeg.http";

  /** The number of seconds that need to resemble until a scene is considered "stable" */
  public static final int STABILITY_THRESHOLD = 5;

  /** Number of pixels that may change between two frames without considering them different */
  public static final float CHANGES_THRESHOLD = 0.05f; // 5% change

  /** The expected mimetype of the resulting preview encoding */
  public static final MimeType MJPEG_MIMETYPE = MimeTypes.MJPEG;

  /** The configuration key for setting the number of worker threads */
  public static final String CONFIG_THREADS = "videosegmenter.threads";
  
  /** The default worker thread pool size to use if no configuration is specified  */
  public static final int DEFAULT_THREADS = 2;

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(VideoSegmenter.class);

  /** Reference to the receipt service */
  private ReceiptService receiptService;

  /** The repository to store the mpeg7 catalogs */
  private WorkingFileRepository repository;

  /** The workspace to ue when retrieving remote media files */
  private Workspace workspace;

  /** The composer service */
  private ComposerService composer;

  /** The http client to use for retrieving protected mpeg7 files */
  protected TrustedHttpClient trustedHttpClient;

  /** The executor service used to queue and run jobs */
  private ExecutorService executor;

  /**
   * Creates a new video segmenter.
   */
  public VideoSegmenter() {
    super(MediaPackageElements.SEGMENTS_FLAVOR);
    super.requireVideo(true);
  }

  protected void activate(ComponentContext cc) {
    // set up threading
    int threads = -1;
    String configredThreads = (String) cc.getBundleContext().getProperty(CONFIG_THREADS);
    // try to parse the value as a number.  If it fails to parse, there is a config problem so we throw an exception.
    if (configredThreads == null) {
      threads = DEFAULT_THREADS;
    } else {
      threads = Integer.parseInt(configredThreads);
    }
    if(threads < 1) {
      throw new IllegalStateException("The composer needs one or more threads to function.");
    }
    setExecutorThreads(threads);
  }

  /** Separating this from the activate method so it's easier to test */
  void setExecutorThreads(int threads) {
    executor = Executors.newFixedThreadPool(threads);
    logger.info("Thread pool size = {}", threads);
  }
  
  /**
   * Sets the composer service.
   * 
   * @param composerService
   */
  public void setComposerService(ComposerService composerService) {
    this.composer = composerService;
  }

  /**
   * Sets the file repository
   * 
   * @param repository
   *          an instance of the working file repository
   */
  public void setFileRepository(WorkingFileRepository repository) {
    this.repository = repository;
  }

  /**
   * Sets the workspace
   * 
   * @param workspace
   *          an instance of the workspace
   */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * Sets the receipt service
   * 
   * @param receiptService
   *          the receipt service
   */
  public void setReceiptService(ReceiptService receiptService) {
    this.receiptService = receiptService;
  }

  /**
   * Sets the trusted http client which is used for authenticated service distribution.
   * 
   * @param trustedHttpClient
   *          the trusted http client
   */
  public void setTrustedHttpClient(TrustedHttpClient trustedHttpClient) {
    this.trustedHttpClient = trustedHttpClient;
  }

  /**
   * Starts segmentation on the video track identified by <code>mediapackageId</code> and <code>elementId</code> and
   * returns a receipt containing the final result in the form of a {@link import
   * org.opencastproject.metadata.mpeg7.Mpeg7Catalog}.
   * 
   * @param element
   *          the element to analyze
   * @param mediapackageId
   *          the media package identifier
   * @param elementId
   *          element identifier
   * @param block
   *          <code>true</code> to make this operation synchronous
   * @return a receipt containing the resulting mpeg-7 catalog
   * @throws MediaAnalysisException
   */
  public Receipt analyze(final MediaPackageElement element, boolean block) throws MediaAnalysisException {
    final ReceiptService rs = receiptService;
    final Receipt receipt = rs.createReceipt(RECEIPT_TYPE);

    // Make sure the element can be analyzed using this analysis implementation
    if (!super.isSupported(element)) {
      receipt.setStatus(Status.FAILED);
      rs.updateReceipt(receipt);
      return receipt;
    }

    final Track track = (Track) element;

    Runnable command = new Runnable() {
      public void run() {
        receipt.setStatus(Status.RUNNING);
        rs.updateReceipt(receipt);

        PlayerListener processorListener = null;

        Mpeg7CatalogImpl mpeg7 = Mpeg7CatalogImpl.newInstance();

        try {

          logger.info("Encoding {} to {}", track, MJPEG_MIMETYPE);
          Track mjpegTrack = prepare(track);

          // Create a player
          File mediaFile = workspace.get(mjpegTrack.getURI());
          URL mediaUrl = mediaFile.toURI().toURL();
          DataSource ds = Manager.createDataSource(mediaUrl);
          Processor processor = null;
          try {
            processor = Manager.createProcessor(ds);
            processorListener = new PlayerListener(processor);
            processor.addControllerListener(processorListener);
          } catch (Exception e) {
            receipt.setStatus(Status.FAILED);
            rs.updateReceipt(receipt);
            throw new MediaAnalysisException(e);
          }

          // Configure the processor
          processor.configure();
          if (!processorListener.waitForState(Processor.Configured)) {
            receipt.setStatus(Status.FAILED);
            rs.updateReceipt(receipt);
            throw new MediaAnalysisException("Unable to configure processor");
          }

          // Set the processor to RAW content
          processor.setContentDescriptor(new ContentDescriptor(ContentDescriptor.RAW));

          // Realize the processor
          processor.realize();
          if (!processorListener.waitForState(Processor.Realized)) {
            receipt.setStatus(Status.FAILED);
            rs.updateReceipt(receipt);
            throw new MediaAnalysisException("Unable to realize the processor");
          }

          // Get the output DataSource from the processor and
          // hook it up to the DataSourceHandler.
          DataSource outputDataSource = processor.getDataOutput();
          FrameGrabber dsh = new FrameGrabber();

          try {
            dsh.setSource(outputDataSource);
          } catch (IncompatibleSourceException e) {
            receipt.setStatus(Status.FAILED);
            rs.updateReceipt(receipt);
            throw new MediaAnalysisException("Cannot handle the output data source from the processor: "
                    + outputDataSource);
          }

          // Load the movie and change the processor to prefetched state
          processor.prefetch();
          if (!processorListener.waitForState(Controller.Prefetched)) {
            receipt.setStatus(Status.FAILED);
            rs.updateReceipt(receipt);
            throw new MediaAnalysisException("Unable to switch player into 'prefetch' state");
          }

          // Get the movie duration
          Time duration = processor.getDuration();
          if (duration == Duration.DURATION_UNKNOWN) {
            receipt.setStatus(Status.FAILED);
            rs.updateReceipt(receipt);
            throw new MediaAnalysisException("Java media framework is unable to detect movie duration");
          }

          logger.info("Track {} loaded, duration is {} s", mediaUrl, duration.getSeconds());

          MediaTime contentTime = new MediaRelTimeImpl(0, (long) duration.getSeconds() * 1000);
          MediaLocator contentLocator = new MediaLocatorImpl(mediaUrl.toURI());
          Video videoContent = mpeg7.addVideoContent("videosegment", contentTime, contentLocator);

          logger.info("Starting video segmentation of {}", mediaUrl);

          processor.setRate(1.0f);
          processor.start();
          dsh.start();
          List<ContentSegment> segments = segment(videoContent, dsh);

          logger.info("Segmentation of {} yields {} segments", mediaUrl, segments.size());

          URI uri = uploadMpeg7(mpeg7);
          mpeg7.setURI(uri);
          mpeg7.setFlavor(MediaPackageElements.SEGMENTS_FLAVOR);
          mpeg7.setReference(new MediaPackageReferenceImpl(element));
          mpeg7.setTrustedHttpClient(trustedHttpClient);

          receipt.setElement(mpeg7);
          receipt.setStatus(Status.FINISHED);
          rs.updateReceipt(receipt);

          logger.info("Finished video segmentation of {}", mediaUrl);

        } catch (MediaAnalysisException e) {
          receipt.setStatus(Status.FAILED);
          rs.updateReceipt(receipt);
          throw e;
        } catch (Exception e) {
          receipt.setStatus(Status.FAILED);
          rs.updateReceipt(receipt);
          throw new MediaAnalysisException(e);
        }
      }
    };

    Future<?> future = executor.submit(command);
    if (block) {
      try {
        future.get();
      } catch (Exception e) {
        receipt.setStatus(Status.FAILED);
        receiptService.updateReceipt(receipt);
        throw new MediaAnalysisException(e);
      }
    }
    return receipt;
  }

  /**
   * Stores the mpeg-7 catalog in the working file repository.
   * 
   * @param catalog
   *          the catalog
   * @return the catalog's URI in the working file repository
   * @throws TransformerFactoryConfigurationError
   *           if serializing the catalog to xml fails
   * @throws IOException
   *           if writing the catalog to the working file repository fails
   * @throws ParserConfigurationException
   *           if the xml parser is not set up correctly
   * @throws TransformerException
   *           if creating the xml representation from the dom tree fails
   * @throws URISyntaxException
   *           if the working file repository created an invalid uri
   */
  protected URI uploadMpeg7(Mpeg7CatalogImpl catalog) throws TransformerFactoryConfigurationError,
          TransformerException, ParserConfigurationException, IOException, URISyntaxException {
    // Store the mpeg7 in the file repository, and store the mpeg7 catalog in the receipt
    // Write the catalog to a byte[]
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Transformer transformer = TransformerFactory.newInstance().newTransformer();
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
    transformer.transform(new DOMSource(catalog.toXml()), new StreamResult(out));

    // Store the bytes in the file repository
    ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
    return repository.putInCollection(COLLECTION_ID, UUID.randomUUID().toString(), in);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.analysis.api.MediaAnalysisService#getReceipt(java.lang.String)
   */
  @Override
  public Receipt getReceipt(String id) {
    return receiptService.getReceipt(id);
  }

  /**
   * Returns the segments for the movie accessible through the frame grabbing control.
   * 
   * @param video
   *          the mpeg-7 video representation
   * @param fpc
   *          the frame positioning control
   * @param dsh
   *          the data source handler
   * @return the list of segments
   * @throws IOException
   *           if accessing a frame fails
   * @throws MediaAnalysisException
   *           if segmentation of the video fails
   */
  protected List<ContentSegment> segment(Video video, FrameGrabber dsh) throws IOException, MediaAnalysisException {
    List<ContentSegment> segments = new ArrayList<ContentSegment>();

    int t = 1;
    int lastStableImageTime = 0;
    long startOfSegment = 0;
    int currentSceneStabilityCount = 1;
    boolean sceneChangeImminent = true;
    boolean luckyPunchRecovery = false;
    int segmentCount = 1;
    BufferedImage previousImage = null;
    BufferedImage lastStableImage = null;
    BlockingQueue<Buffer> bufferQueue = new ArrayBlockingQueue<Buffer>(STABILITY_THRESHOLD + 1);
    long durationInSeconds = video.getMediaTime().getMediaDuration().getDurationInMilliseconds() / 1000;
    ContentSegment contentSegment = video.getTemporalDecomposition().createSegment("segment-" + segmentCount);
    ImageComparator icomp = new ImageComparator(CHANGES_THRESHOLD);
    
//    icomp.setStatistics(true);
//    String imagesPath = PathSupport.concat(new String[] {
//      System.getProperty("java.io.tmpdir"),
//      "videosegments",
//      video.getMediaLocator().getMediaURI().toString().replaceAll("\\W", "-")
//    });
//    icomp.saveImagesTo(new File(imagesPath));

    Buffer buf = dsh.getBuffer();
    while (t < durationInSeconds && buf != null && !buf.isEOM()) {
      BufferedImage bufferedImage = ImageUtils.createImage(buf);
      if (bufferedImage == null)
        throw new MediaAnalysisException("Unable to extract image at time " + t);

      logger.trace("Analyzing video at {} s", t);

      // Compare the new image with our previous sample
      boolean differsFromPreviousImage = icomp.isDifferent(previousImage, bufferedImage, t);

      // We found an image that is different compared to the previous one. Let's see if this image remains stable
      // for some time (STABILITY_THRESHOLD) so we can declare a new scene
      if (differsFromPreviousImage) {
        logger.debug("Found differing image at {} seconds", t);

        // If this is the result of a lucky punch (looking ahead STABILITY_THRESHOLD seconds), then we should
        // really start over an make sure we get the correct beginning of the new scene
        if (!sceneChangeImminent && t - lastStableImageTime > 1) {
          luckyPunchRecovery = true;
          previousImage = lastStableImage;
          bufferQueue.add(buf);
          t = lastStableImageTime;
        } else {
          lastStableImageTime = t - 1;
          lastStableImage = previousImage;
          previousImage = bufferedImage;
          currentSceneStabilityCount = 1;
          t++;
        }
        sceneChangeImminent = true;
      }
      
      // We are looking ahead and everyhting seems to be fine.
      else if (!sceneChangeImminent) {
        fillLookAheadBuffer(bufferQueue, buf, dsh);
        lastStableImageTime = t;
        t += STABILITY_THRESHOLD;
        previousImage = bufferedImage;
        lastStableImage = bufferedImage;
      }

      // Seems to be the same image. If we have just recently detected a new scene, let's see if we are able to
      // confirm that this is scene is stable (>= STABILITY_THRESHOLD)
      else if (currentSceneStabilityCount < STABILITY_THRESHOLD) {
        currentSceneStabilityCount++;
        previousImage = bufferedImage;
        t++;
      } 
      
      // Did we find a new scene?
      else if (currentSceneStabilityCount == STABILITY_THRESHOLD) {
        lastStableImageTime = t;

        long endOfSegment = t - STABILITY_THRESHOLD;
        long durationms = (endOfSegment - startOfSegment) * 1000L;

        // Create a new segment if this wasn't the first one
        if (endOfSegment > STABILITY_THRESHOLD) {
          contentSegment.setMediaTime(new MediaRelTimeImpl(startOfSegment * 1000L, durationms));
          contentSegment = video.getTemporalDecomposition().createSegment("segment-" + ++segmentCount);
          segments.add(contentSegment);
          startOfSegment = endOfSegment;
        }

        // After finding a new segment, likelihood of a stable image is good, let's take a look ahead. Since
        // a processor can't seek, we need to store the buffers in between, in case we need to come back.
        fillLookAheadBuffer(bufferQueue, buf, dsh);
        t += STABILITY_THRESHOLD;
        previousImage = bufferedImage;
        lastStableImage = bufferedImage;
        currentSceneStabilityCount++;
        sceneChangeImminent = false;
        logger.info("Found new scene at {} s", startOfSegment);
      } 
  
      // Did we find a new scene by looking ahead?
      else if (sceneChangeImminent) {
        // We found a scene change by looking ahead. Now we want to get to the exact position
        lastStableImageTime = t;
        previousImage = bufferedImage;
        lastStableImage = bufferedImage;
        currentSceneStabilityCount++;
        t++;
      } 
      
      // Nothing special, business as usual
      else {
        // If things look stable, then let's look ahead as much as possible without loosing information (which is
        // equal to looking ahead STABILITY_THRESHOLD seconds.
        lastStableImageTime = t;
        fillLookAheadBuffer(bufferQueue, buf, dsh);
        t += STABILITY_THRESHOLD;
        lastStableImage = bufferedImage;
        previousImage = bufferedImage;
      }

      if (luckyPunchRecovery) {
        buf = bufferQueue.poll();
        luckyPunchRecovery = !bufferQueue.isEmpty();
      } else
        buf = dsh.getBuffer();
    }

    // Finish off the last segment
    long startOfSegmentms = startOfSegment * 1000L;
    long durationms = ((long) durationInSeconds - startOfSegment) * 1000;
    contentSegment.setMediaTime(new MediaRelTimeImpl(startOfSegmentms, durationms));
    segments.add(contentSegment);
    
    // Print summary
    if (icomp.hasStatistics()) {
      NumberFormat nf = NumberFormat.getNumberInstance();
      nf.setMaximumFractionDigits(2);
      logger.info("Image comparison finished with an average change of {}% in {} comparisons", nf.format(icomp.getAvgChange()), icomp.getComparisons());
    }

    // Cleanup
    if (icomp.getSavedImagesDirectory() != null) {
      FileUtils.deleteQuietly(icomp.getSavedImagesDirectory());
    }

    return segments;
  }

  /**
   * Fills the look ahead buffer with the next <code>STABILITY_THRESHOLD</code> images.
   * 
   * @param queue
   *          the buffer
   * @param currentBuffer
   *          the current buffer
   * @param dsh
   *          the data source handler
   * @throws IOException
   *           if reading from the data source fails
   */
  private void fillLookAheadBuffer(BlockingQueue<Buffer> queue, Buffer currentBuffer, FrameGrabber dsh)
          throws IOException {
    queue.clear();
    queue.add(currentBuffer);
    for (int i = 0; i < STABILITY_THRESHOLD - 1; i++) {
      Buffer b = dsh.getBuffer();
      if (b != null && !b.isEOM())
        queue.add(b);
      else
        return;
    }
  }

  /**
   * Makes sure that there is version of the track that is compatible with this service implementation. Currently, with
   * the usage of the <code>JMF 2.1.1e</code> framework, the list is rather limited, see Sun's supported list of formats at
   * {@link http://java.sun.com/javase/technologies/desktop/media/jmf/2.1.1/formats.html}.
   * 
   * @param track
   *          the track identifier
   * @return the encoded track
   * @throws MediaPackageException
   *           if adding the encoded track to the media package fails
   * @throws EncoderException
   *           if encoding fails
   * @throws IllegalStateException
   *           if the track is not connected to a media package and is not in the correct format
   */
  protected Track prepare(Track track) throws EncoderException, MediaPackageException {
    MediaPackage mediaPackage = track.getMediaPackage();

    if (mediaPackage == null) {
      if (MJPEG_MIMETYPE.equals(track.getMimeType()))
        return track;
      throw new IllegalStateException();
    }

    MediaPackageReference original = new MediaPackageReferenceImpl(track);

    // See if encoding has already taken place
    List<Track> derivedTracks = new ArrayList<Track>();
    derivedTracks.add(track);
    derivedTracks.addAll(Arrays.asList(track.getMediaPackage().getTracks(original)));
    for (Track t : derivedTracks) {
      if (MJPEG_MIMETYPE.equals(t.getMimeType())) {
        logger.info("Using existing mjpeg track {}", t);
        return t;
      }
    }

    // Looks like we need to do the work ourselves
    logger.info("Requesting {} version of track {}", MJPEG_MIMETYPE, track);
    final Receipt receipt = composer.encode(mediaPackage, track.getIdentifier(), null, MJPEG_ENCODING_PROFILE, true);
    Track composedTrack = (Track) receipt.getElement();
    composedTrack.setReference(original);
    composedTrack.setMimeType(MJPEG_MIMETYPE);
    composedTrack.addTag("segmentation");

    return composedTrack;
  }

}
