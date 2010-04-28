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
package org.opencastproject.inspection.impl;

import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.inspection.impl.api.AudioStreamMetadata;
import org.opencastproject.inspection.impl.api.MediaAnalyzer;
import org.opencastproject.inspection.impl.api.MediaContainerMetadata;
import org.opencastproject.inspection.impl.api.VideoStreamMetadata;
import org.opencastproject.media.mediapackage.MediaPackageElement;
import org.opencastproject.media.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.media.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.media.mediapackage.Stream;
import org.opencastproject.media.mediapackage.Track;
import org.opencastproject.media.mediapackage.UnsupportedElementException;
import org.opencastproject.media.mediapackage.MediaPackageElement.Type;
import org.opencastproject.media.mediapackage.track.AudioStreamImpl;
import org.opencastproject.media.mediapackage.track.TrackImpl;
import org.opencastproject.media.mediapackage.track.VideoStreamImpl;
import org.opencastproject.receipt.api.Receipt;
import org.opencastproject.receipt.api.ReceiptService;
import org.opencastproject.receipt.api.Receipt.Status;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.ChecksumType;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.UnknownFileTypeException;
import org.opencastproject.workspace.api.NotFoundException;
import org.opencastproject.workspace.api.Workspace;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Inspects media via the 3rd party MediaInfo tool by default, and can be configured to use other media analyzers.
 */
public class MediaInspectionServiceImpl implements MediaInspectionService, ManagedService {
  // FIXME move this to media info analyzer service
  public static final String CONFIG_ANALYZER_MEDIAINFOPATH = "inspection.analyzer.mediainfopath";
  private static final Logger logger = LoggerFactory.getLogger(MediaInspectionServiceImpl.class);

  Workspace workspace;
  ReceiptService receiptService;
  ExecutorService executor = null;
  Map<String, Object> analyzerConfig = new ConcurrentHashMap<String, Object>();

  public void setWorkspace(Workspace workspace) {
    logger.debug("setting " + workspace);
    this.workspace = workspace;
  }

  public void setReceiptService(ReceiptService receiptService) {
    this.receiptService = receiptService;
  }

  @SuppressWarnings("unchecked")
  public void updated(Dictionary properties) throws ConfigurationException {
    logger.info("TODO Updating configuration on {}", this.getClass().getName());
    // FIXME this is doing nothing
  }

  public void activate(ComponentContext cc) {
    if (cc != null) {
      if (cc.getBundleContext().getProperty(CONFIG_ANALYZER_MEDIAINFOPATH) != null) {
        // use binary path from CONFIG
        String path = cc.getBundleContext().getProperty(CONFIG_ANALYZER_MEDIAINFOPATH);
        analyzerConfig.put(MediaInfoAnalyzer.CONFIG_MEDIAINFO_BINARY, path);
        logger.info("CONFIG " + CONFIG_ANALYZER_MEDIAINFOPATH + ": " + path);
      }
    }
    activate();
  }

  public void activate() {
    executor = Executors.newFixedThreadPool(4);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.inspection.api.MediaInspectionService#getReceipt(java.lang.String)
   */
  @Override
  public Receipt getReceipt(String id) {
    return receiptService.getReceipt(id);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.inspection.api.MediaInspectionService#inspect(java.net.URI, boolean)
   */
  public Receipt inspect(final URI uri, final boolean block) {
    logger.debug("inspect(" + uri + ") called, using workspace " + workspace);

    // Construct a receipt for this operation
    final Receipt receipt = receiptService.createReceipt(RECEIPT_TYPE);
    final ReceiptService rs = receiptService;
    Callable<Track> command = new Callable<Track>() {
      public Track call() throws Exception {
        // Update the receipt status
        receipt.setStatus(Status.RUNNING);
        rs.updateReceipt(receipt);

        // Get the file from the URL (runtime exception if invalid)
        File file = null;
        try {
          file = workspace.get(uri);
        } catch (NotFoundException e) {
          receipt.setStatus(Status.FAILED);
          rs.updateReceipt(receipt);
          throw new RuntimeException(e);
        }
        MediaContainerMetadata metadata = getFileMetadata(file);
        if (metadata == null) {
          logger.warn("Unable to acquire media metadata for " + uri);
          receipt.setStatus(Status.FAILED);
          rs.updateReceipt(receipt);
          return null; // TODO: does the contract for this service define what to do if the file isn't valid media?
        } else {
          MediaPackageElementBuilder elementBuilder = MediaPackageElementBuilderFactory.newInstance()
                  .newElementBuilder();
          TrackImpl track;
          MediaPackageElement element;
          try {
            element = elementBuilder.elementFromURI(uri, Type.Track, null);
          } catch (UnsupportedElementException e) {
            receipt.setStatus(Status.FAILED);
            rs.updateReceipt(receipt);
            throw new RuntimeException(e);
          }
          track = (TrackImpl) element;
          if (metadata.getDuration() != null)
            track.setDuration(metadata.getDuration());
          try {
            track.setChecksum(Checksum.create(ChecksumType.DEFAULT_TYPE, file));
          } catch (NoSuchAlgorithmException e) {
            receipt.setStatus(Status.FAILED);
            rs.updateReceipt(receipt);
            throw new RuntimeException(e);
          } catch (IOException e) {
            receipt.setStatus(Status.FAILED);
            rs.updateReceipt(receipt);
            throw new RuntimeException(e);
          }
          try {
            track.setMimeType(MimeTypes.fromURL(file.toURI().toURL()));
          } catch (Exception e) {
            logger.info("unable to find mimetype for {}", file.getAbsolutePath());
          }
          List<AudioStreamMetadata> audioList = metadata.getAudioStreamMetadata();
          if (audioList != null && !audioList.isEmpty()) {
            for (int i = 0; i < audioList.size(); i++) {
              AudioStreamImpl audio = new AudioStreamImpl("audio-" + (i + 1));
              AudioStreamMetadata a = audioList.get(i);
              audio.setBitRate(a.getBitRate());
              audio.setChannels(a.getChannels());
              audio.setFormat(a.getFormat());
              audio.setFormatVersion(a.getFormatVersion());
              audio.setBitDepth(a.getResolution());
              audio.setSamplingRate(a.getSamplingRate());
              track.addStream(audio);
            }
          }
          List<VideoStreamMetadata> videoList = metadata.getVideoStreamMetadata();
          if (videoList != null && !videoList.isEmpty()) {
            for (int i = 0; i < videoList.size(); i++) {
              VideoStreamImpl video = new VideoStreamImpl("video-" + (i + 1));
              VideoStreamMetadata v = videoList.get(i);
              video.setBitRate(v.getBitRate());
              video.setFormat(v.getFormat());
              video.setFormatVersion(v.getFormatVersion());
              video.setFrameHeight(v.getFrameHeight());
              video.setFrameRate(v.getFrameRate());
              video.setFrameWidth(v.getFrameWidth());
              video.setScanOrder(v.getScanOrder());
              video.setScanType(v.getScanType());
              track.addStream(video);
            }
          }
          receipt.setElement(track);
          receipt.setStatus(Status.FINISHED);
          rs.updateReceipt(receipt);
          return track;
        }
      }
    };

    Future<Track> future = executor.submit(command);

    if (block) {
      try {
        future.get();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    return receipt;
  }

  protected Callable<MediaPackageElement> getEnrichTrackCommand(final Track originalTrack, final boolean override,
          final Receipt receipt) {
    final ReceiptService rs = receiptService;
    return new Callable<MediaPackageElement>() {
      public MediaPackageElement call() throws Exception {
        // Set the receipt state to running
        receipt.setStatus(Status.RUNNING);
        rs.updateReceipt(receipt);

        URI originalTrackUrl = originalTrack.getURI();
        MediaPackageElementFlavor flavor = originalTrack.getFlavor();
        logger.debug("enrich(" + originalTrackUrl + ") called");
        // Get the file from the URL
        File file = null;
        try {
          file = workspace.get(originalTrackUrl);
        } catch (NotFoundException e) {
          receipt.setStatus(Status.FAILED);
          rs.updateReceipt(receipt);
          throw new RuntimeException(e);
        }
        MediaContainerMetadata metadata = getFileMetadata(file);
        if (metadata == null) {
          logger.warn("Unable to acquire media metadata for " + originalTrackUrl);
          receipt.setStatus(Status.FAILED);
          rs.updateReceipt(receipt);
          return null;
        } else if (metadata.getAudioStreamMetadata().size() == 0 && metadata.getVideoStreamMetadata().size() == 0) {
          logger.warn("File at {} does not seem like a a/v media", originalTrackUrl);
          receipt.setStatus(Status.FAILED);
          rs.updateReceipt(receipt);
          return null;
        } else {
          TrackImpl track = null;
          try {
            track = (TrackImpl) MediaPackageElementBuilderFactory.newInstance().newElementBuilder().elementFromURI(
                    originalTrackUrl, Type.Track, flavor);
          } catch (UnsupportedElementException e) {
            receipt.setStatus(Status.FAILED);
            rs.updateReceipt(receipt);
            throw new RuntimeException(e);
          }
          // init the new track with old
          track.setChecksum(originalTrack.getChecksum());
          track.setDuration(originalTrack.getDuration());
          track.setElementDescription(originalTrack.getElementDescription());
          track.setFlavor(flavor);
          track.setIdentifier(originalTrack.getIdentifier());
          track.setMimeType(originalTrack.getMimeType());
          track.setReference(originalTrack.getReference());
          track.setSize(originalTrack.getSize());
          track.setURI(originalTrackUrl);
          for (String tag : originalTrack.getTags())
            track.addTag(tag);

          // enrich the new track with basic info
          if (track.getDuration() == -1L || override)
            track.setDuration(metadata.getDuration());
          if (track.getChecksum() == null || override)
            try {
              track.setChecksum(Checksum.create(ChecksumType.DEFAULT_TYPE, file));
            } catch (NoSuchAlgorithmException e) {
              receipt.setStatus(Status.FAILED);
              rs.updateReceipt(receipt);
              throw new RuntimeException(e);
            } catch (IOException e) {
              receipt.setStatus(Status.FAILED);
              rs.updateReceipt(receipt);
              throw new RuntimeException(e);
            }

          // Add the mime type if it's not already present
          if (track.getMimeType() == null || override) {
            try {
              track.setMimeType(MimeTypes.fromURL(track.getURI().toURL()));
            } catch (MalformedURLException e) {
              logger.warn("Track {} has a malformed URL, {}", track.getIdentifier(), track.getURI());
            } catch (UnknownFileTypeException e) {
              logger.debug("Unable to detect the mimetype for track {} at {}", track.getIdentifier(), track.getURI());
            }
          }
          // find all streams
          Dictionary<String, Stream> streamsId2Stream = new Hashtable<String, Stream>();
          for (Stream stream : originalTrack.getStreams()) {
            streamsId2Stream.put(stream.getIdentifier(), stream);
          }

          // audio list
          List<AudioStreamMetadata> audioList = metadata.getAudioStreamMetadata();
          if (audioList != null && !audioList.isEmpty()) {
            for (int i = 0; i < audioList.size(); i++) {
              AudioStreamImpl audio = new AudioStreamImpl("audio-" + (i + 1));
              AudioStreamMetadata a = audioList.get(i);
              audio.setBitRate(a.getBitRate());
              audio.setChannels(a.getChannels());
              audio.setFormat(a.getFormat());
              audio.setFormatVersion(a.getFormatVersion());
              audio.setBitDepth(a.getResolution());
              audio.setSamplingRate(a.getSamplingRate());
              // TODO: retain the original audio metadata
              track.addStream(audio);
            }
          }
          // video list
          List<VideoStreamMetadata> videoList = metadata.getVideoStreamMetadata();
          if (videoList != null && !videoList.isEmpty()) {
            for (int i = 0; i < videoList.size(); i++) {
              VideoStreamImpl video = new VideoStreamImpl("video-" + (i + 1));
              VideoStreamMetadata v = videoList.get(i);
              video.setBitRate(v.getBitRate());
              video.setFormat(v.getFormat());
              video.setFormatVersion(v.getFormatVersion());
              video.setFrameHeight(v.getFrameHeight());
              video.setFrameRate(v.getFrameRate());
              video.setFrameWidth(v.getFrameWidth());
              video.setScanOrder(v.getScanOrder());
              video.setScanType(v.getScanType());
              // TODO: retain the original video metadata
              track.addStream(video);
            }
          }
          receipt.setElement(track);
          receipt.setStatus(Status.FINISHED);
          rs.updateReceipt(receipt);
          return track;
        }
      }
    };
  }

  protected Callable<MediaPackageElement> getEnrichElementCommand(final MediaPackageElement element,
          final boolean override, final Receipt receipt) {
    final ReceiptService rs = receiptService;
    return new Callable<MediaPackageElement>() {
      public MediaPackageElement call() throws Exception {
        receipt.setStatus(Status.RUNNING);
        rs.updateReceipt(receipt);
        File file;
        try {
          file = workspace.get(element.getURI());
        } catch (NotFoundException e) {
          receipt.setStatus(Status.FAILED);
          rs.updateReceipt(receipt);
          throw new RuntimeException(e);
        }
        if (element.getChecksum() == null || override) {
          try {
            element.setChecksum(Checksum.create(ChecksumType.DEFAULT_TYPE, file));
          } catch (Exception e) {
            receipt.setStatus(Status.FAILED);
            rs.updateReceipt(receipt);
            throw new RuntimeException(e);
          }
        }
        if (element.getMimeType() == null || override) {
          try {
            element.setMimeType(MimeTypes.fromURL(file.toURI().toURL()));
          } catch (UnknownFileTypeException e) {
            logger.info("unable to determine the mime type for {}", file.getName());
          }
        }
        receipt.setElement(element);
        receipt.setStatus(Status.FINISHED);
        rs.updateReceipt(receipt);
        return element;
      }
    };
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.inspection.api.MediaInspectionService#enrich(org.opencastproject.media.mediapackage.AbstractMediaPackageElement,
   *      boolean, boolean)
   */
  @Override
  public Receipt enrich(final MediaPackageElement element, final boolean override, final boolean block) {
    Callable<MediaPackageElement> command;
    final Receipt receipt = receiptService.createReceipt(RECEIPT_TYPE);
    if (element instanceof Track) {
      final Track originalTrack = (Track) element;
      command = getEnrichTrackCommand(originalTrack, override, receipt);
    } else {
      command = getEnrichElementCommand(element, override, receipt);
    }

    Future<MediaPackageElement> future = executor.submit(command);

    if (block) {
      try {
        future.get();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    return receipt;
  }

  private MediaContainerMetadata getFileMetadata(File file) {
    if (file == null) {
      throw new IllegalArgumentException("file to analyze cannot be null");
    }
    MediaContainerMetadata metadata = null;
    try {
      MediaAnalyzer analyzer = new MediaInfoAnalyzer();
      analyzer.setConfig(analyzerConfig);
      metadata = analyzer.analyze(file);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to create media analyzer", e);
    }
    return metadata;
  }

}
