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

package org.opencastproject.search.impl.solr;

import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageReference;
import org.opencastproject.metadata.dublincore.DCMIPeriod;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.metadata.mpeg7.AudioVisual;
import org.opencastproject.metadata.mpeg7.FreeTextAnnotation;
import org.opencastproject.metadata.mpeg7.KeywordAnnotation;
import org.opencastproject.metadata.mpeg7.MediaDuration;
import org.opencastproject.metadata.mpeg7.MediaTime;
import org.opencastproject.metadata.mpeg7.MediaTimePoint;
import org.opencastproject.metadata.mpeg7.Mpeg7Catalog;
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogService;
import org.opencastproject.metadata.mpeg7.MultimediaContent;
import org.opencastproject.metadata.mpeg7.MultimediaContentType;
import org.opencastproject.metadata.mpeg7.SpatioTemporalDecomposition;
import org.opencastproject.metadata.mpeg7.TextAnnotation;
import org.opencastproject.metadata.mpeg7.Video;
import org.opencastproject.metadata.mpeg7.VideoSegment;
import org.opencastproject.metadata.mpeg7.VideoText;
import org.opencastproject.search.api.SearchResultItem.SearchResultItemType;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.request.UpdateRequest.ACTION;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map.Entry;

/**
 * Utility class used to manage the search index.
 */
public class SolrIndexManager {

  /** Logging facility */
  static Logger logger = LoggerFactory.getLogger(SolrIndexManager.class);

  /** Connection to the database */
  private SolrConnection solrConnection = null;

  /**
   * Factor multiplied to fine tune relevance and confidence impact on important keyword decision. importance =
   * RELEVANCE_BOOST * relevance + confidence
   ***/
  private static final double RELEVANCE_BOOST = 2.0;

  /** Number of characters an important should have at least. */
  private static final int MAX_CHAR = 3;

  /** Maximum number of important keywords to detect. */
  private static final int MAX_IMPORTANT_COUNT = 10;

  /** The solr supported date format. **/
  private DateFormat dateFormat = new SimpleDateFormat(SolrFields.SOLR_DATE_FORMAT);

  private DublinCoreCatalogService dcService;

  private Mpeg7CatalogService mpeg7Service;

  private Workspace workspace;
  
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  public void setDcService(DublinCoreCatalogService dcService) {
    this.dcService = dcService;
  }

  public void setMpeg7Service(Mpeg7CatalogService mpeg7Service) {
    this.mpeg7Service = mpeg7Service;
  }

  /**
   * Creates a new management instance for the search index.
   * 
   * @param connection
   *          connection to the database
   */
  public SolrIndexManager(SolrConnection connection, Workspace workspace) {
    if (connection == null)
      throw new IllegalArgumentException("Unable to manage solr with null connection");
    if(workspace == null)
      throw new IllegalArgumentException("Unable to manager solr without a workspace");
    this.solrConnection = connection;
    this.workspace = workspace;
  }

  /**
   * Clears the search index. Make sure you know what you are doing.
   * 
   * @throws SolrServerException
   *           if an errors occurs while talking to solr
   */
  public void clear() throws SolrServerException {
    UpdateRequest solrRequest = new UpdateRequest();
    solrRequest.deleteByQuery("*:*");
    solrRequest.setAction(ACTION.COMMIT, true, true);
    try {
      solrConnection.update(solrRequest);
    } catch (Exception e) {
      logger.error("Cannot clear solr index", e);
    }
  }

  /**
   * Removes the entry with the given <code>id</code> from the database. The entry can either be a series or an episode.
   * 
   * @param id
   *          identifier of the series or episode to delete
   * @throws SolrServerException
   *           if an errors occurs while talking to solr
   */
  public boolean delete(String id) throws SolrServerException {
    UpdateRequest solrRequest = new UpdateRequest();
    solrRequest.deleteById(id);
    solrRequest.setAction(ACTION.COMMIT, true, true);
    try {
      solrConnection.update(solrRequest);
      return true;
    } catch (Exception e) {
      logger.error("Cannot clear solr index");
      return false;
    }
  }

  /**
   * Posts the media package to solr. Depending on what is referenced in the media package, the method might create one
   * or two entries: one for the episode and one for the series that the episode belongs to.
   * 
   * This implementation of the search service removes all references to non "engage/download" media tracks
   * 
   * @param sourceMediaPackage
   *          the media package to post
   * @throws SolrServerException
   *           if an errors occurs while talking to solr
   */
  public boolean add(MediaPackage sourceMediaPackage) throws SolrServerException {
    UpdateRequest solrRequest = new UpdateRequest();
    solrRequest.setAction(ACTION.COMMIT, true, true);

    SolrUpdateableInputDocument episodeDocument = null;
    SolrUpdateableInputDocument seriesDocument = null;
    try {
      episodeDocument = createEpisodeInputDocument(sourceMediaPackage);
      seriesDocument = createSeriesInputDocument(sourceMediaPackage);
    } catch (Exception e) {
      throw new SolrServerException(e);
    }

    // If neither an episode nor a series was contained, there is no point in
    // trying to update
    if (episodeDocument == null && seriesDocument == null) {
      logger.warn("Neither episode nor series metadata found");
      return false;
    }

    // Add the episode metadata
    if (episodeDocument != null) {
      if (seriesDocument != null)
        episodeDocument.setField(SolrFields.DC_IS_PART_OF, seriesDocument.getFieldValue(SolrFields.ID));
      solrRequest.add(episodeDocument);
    }

    // Has a series dublincore been included?
    if (seriesDocument != null)
      solrRequest.add(seriesDocument);

    // Post everything to the search index
    try {
      solrConnection.update(solrRequest);
      return true;
    } catch (Exception e) {
      logger.error("Cannot clear solr index");
      return false;
    }
  }

  /**
   * Creates a solr input document for the episode metadata of the media package.
   * 
   * @param mediaPackage
   *          the media package
   * @return an input document ready to be posted to solr
   * @throws MediaPackageException
   *           if serialization of the media package fails
   */
  private SolrUpdateableInputDocument createEpisodeInputDocument(MediaPackage mediaPackage)
          throws MediaPackageException, IOException {

    SolrUpdateableInputDocument solrEpisodeDocument = new SolrUpdateableInputDocument();
    String mediaPackageId = mediaPackage.getIdentifier().toString();

    // Set common fields
    solrEpisodeDocument.setField(SolrFields.ID, mediaPackageId);
    solrEpisodeDocument.setField(SolrFields.OC_MEDIATYPE, SearchResultItemType.AudioVisual);
    solrEpisodeDocument.setField(SolrFields.OC_MODIFIED, dateFormat.format((new Date()).getTime()));

    // Add standard dublin core fields
    addStandardDublincCoreFields(solrEpisodeDocument, mediaPackage, MediaPackageElements.EPISODE);

    // Add media package
    solrEpisodeDocument.setField(SolrFields.OC_MEDIAPACKAGE, mediaPackage.toXml());

    // Add tags
    StringBuilder sb = new StringBuilder();
    for (MediaPackageElement element : mediaPackage.getElements()) {
      for (String tag : element.getTags()) {
        sb.append(tag);
        sb.append(" ");
      }
    }
    solrEpisodeDocument.addField(SolrFields.OC_ELEMENTTAGS, sb.toString());

    // Add flavors
    sb = new StringBuilder();
    for (MediaPackageElement element : mediaPackage.getElements()) {
      if (element.getFlavor() != null) {
        sb.append(element.getFlavor().toString());
        sb.append(" ");
      }
    }
    solrEpisodeDocument.addField(SolrFields.OC_ELEMENTFLAVORS, sb.toString());

    // Add cover
    Attachment[] cover = mediaPackage.getAttachments(MediaPackageElements.MEDIAPACKAGE_COVER_FLAVOR);
    if (cover != null && cover.length > 0) {
      solrEpisodeDocument.addField(SolrFields.OC_COVER, cover[0].getURI().toString());
    }

    // Add mpeg7
    logger.debug("Looking for mpeg-7 catalogs containing segment texts");
    Catalog mpeg7Catalogs[] = mediaPackage.getCatalogs(MediaPackageElements.TEXTS);
    if(mpeg7Catalogs.length == 0) {
      logger.debug("No text catalogs found, trying segments only");
      mpeg7Catalogs = mediaPackage.getCatalogs(MediaPackageElements.SEGMENTS);
    }
    // TODO: merge the segments from each mpeg7 if there is more than one mpeg7 catalog
    if (mpeg7Catalogs.length > 0) {
      Mpeg7Catalog mpeg7Catalog = loadMpeg7Catalog(mpeg7Catalogs[0]);
      addMpeg7Metadata(solrEpisodeDocument, mediaPackage, mpeg7Catalog);
    } else {
      logger.debug("No segmentation catalog found");
    }
    return solrEpisodeDocument;
  }
  
  protected DublinCoreCatalog loadDublinCoreCatalog(Catalog cat) throws IOException {
    InputStream in = null;
    try {
      File f = workspace.get(cat.getURI());
      in = new FileInputStream(f);
      return dcService.load(in);
    } catch (NotFoundException e) {
      throw new IOException("Unable to load metadata from dublin core catalog " + cat);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  protected Mpeg7Catalog loadMpeg7Catalog(Catalog cat) throws IOException {
    InputStream in = null;
    try {
      File f = workspace.get(cat.getURI());
      in = new FileInputStream(f);
      return mpeg7Service.load(in);
    } catch (NotFoundException e) {
      throw new IOException("Unable to load metadata from mpeg7 catalog " + cat);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  /**
   * Creates a solr input document for the series metadata of the media package.
   * 
   * @param mediaPackage
   *          the media package
   * @return an input document ready to be posted to solr
   */
  private SolrUpdateableInputDocument createSeriesInputDocument(MediaPackage mediaPackage) throws IOException {
    SolrUpdateableInputDocument solrSeriesDocument = new SolrUpdateableInputDocument();

    // Check if there is a dublin core for series
    if (mediaPackage.getCatalogs(MediaPackageElements.SERIES).length == 0) {
      logger.debug("No series dublincore found in media package " + mediaPackage);
      return null;
    }

    // If this is the case, try to get a hold on it
    Catalog dcCatalogs[] = mediaPackage.getCatalogs(MediaPackageElements.SERIES);
    DublinCoreCatalog dublinCore = loadDublinCoreCatalog(dcCatalogs[0]);
    String seriesId = dublinCore.getFirst(DublinCore.PROPERTY_IDENTIFIER);

    // Populate document with existing data
    try {
      StringBuffer query = new StringBuffer("q=");
      query = query.append(SolrFields.ID).append(":").append(SolrUtils.clean(seriesId));
      QueryResponse solrResponse = solrConnection.request(query.toString());
      if (solrResponse.getResults().size() > 0) {
        SolrDocument existingsolrDocument = solrResponse.getResults().get(0);
        for (String fieldName : existingsolrDocument.getFieldNames()) {
          solrSeriesDocument.addField(fieldName, existingsolrDocument.getFieldValue(fieldName));
        }
      }
    } catch (Exception e) {
      logger.error("Error trying to load series " + seriesId, e);
    }

    // Set common fields
    solrSeriesDocument.setField(SolrFields.ID, seriesId);
    solrSeriesDocument.setField(SolrFields.OC_MEDIATYPE, SearchResultItemType.Series);
    solrSeriesDocument.setField(SolrFields.OC_MODIFIED, dateFormat.format((new Date()).getTime()));
    addStandardDublincCoreFields(solrSeriesDocument, mediaPackage, MediaPackageElements.SERIES);

    return solrSeriesDocument;
  }

  /**
   * Takes the same field from a dublin core catalog and a mediapackage and chooses the mediapckage's one over the other
   * if it's not null.
   * 
   * @param dcValue
   *          the dublin core field value
   * @param mediaPackageValue
   *          the media package field value
   * @return the value to be used
   */
  private String getValue(String dcValue, String... mediaPackageValue) {
    String mpVal = mediaPackageValue == null || mediaPackageValue.length == 0 ? null : StringUtils
            .trimToNull(mediaPackageValue[0]);
    String dcVal = StringUtils.trimToNull(dcValue);
    return mpVal == null ? dcVal : mpVal;
  }

  /**
   * Adds the standard dublin core fields to the solr document.
   * 
   * @param solrInput
   *          the input document
   * @param mediaPackage
   *          the media package
   * @param flavor
   *          flavor of the dublin core catalog
   */
  private void addStandardDublincCoreFields(SolrUpdateableInputDocument solrInput, MediaPackage mediaPackage,
          MediaPackageElementFlavor flavor) throws IOException {

    // Add dublin core
    DublinCoreCatalog dc = null;
    Catalog dcCatalogs[] = mediaPackage.getCatalogs(flavor);
    if (dcCatalogs != null && dcCatalogs.length > 0) {
      dc = loadDublinCoreCatalog(dcCatalogs[0]);
    } else {
      dc = dcService.newInstance();
      logger.info("No episode dublincore metadata found in media package {}", mediaPackage);
    }

    // If this is the case, try to get a hold on it

    if (!dc.hasValue(DublinCore.PROPERTY_TITLE) && mediaPackage.getTitle() == null)
      throw new IllegalStateException("Found media package without a title");

    // dc:title
    solrInput.addField(SolrFields.DC_TITLE, getValue(dc.getFirst(DublinCore.PROPERTY_TITLE), mediaPackage.getTitle()));

    // dc:subject
    if (dc.hasValue(DublinCore.PROPERTY_SUBJECT)) {
      solrInput.addField(SolrFields.DC_SUBJECT, getValue(dc.getFirst(DublinCore.PROPERTY_SUBJECT), mediaPackage
              .getSubjects()));
    }

    // dc:creator
    if (dc.hasValue(DublinCore.PROPERTY_CREATOR)) {
      solrInput.addField(SolrFields.DC_CREATOR, getValue(dc.getFirst(DublinCore.PROPERTY_CREATOR), mediaPackage
              .getCreators()));
    }

    // dc:extent
    if (dc.hasValue(DublinCore.PROPERTY_EXTENT)) {
      long duration = mediaPackage.getDuration();
      if (duration < 0)
        duration = EncodingSchemeUtils.decodeDuration(dc.get(DublinCore.PROPERTY_EXTENT).get(0));
      solrInput.addField(SolrFields.DC_EXTENT, duration);
    }

    // dc:publisher
    if (dc.hasValue(DublinCore.PROPERTY_PUBLISHER)) {
      solrInput.addField(SolrFields.DC_PUBLISHER, dc.getFirst(DublinCore.PROPERTY_PUBLISHER));
    }

    // dc:contributor
    if (dc.hasValue(DublinCore.PROPERTY_CONTRIBUTOR)) {
      solrInput.addField(SolrFields.DC_CONTRIBUTOR, getValue(dc.getFirst(DublinCore.PROPERTY_CONTRIBUTOR), mediaPackage
              .getContributors()));
    }

    // dc:abstract
    if (dc.hasValue(DublinCore.PROPERTY_ABSTRACT)) {
      solrInput.addField(SolrFields.DC_ABSTRACT, dc.getFirst(DublinCore.PROPERTY_ABSTRACT));
    }

    // dc:description
    if (dc.hasValue(DublinCore.PROPERTY_DESCRIPTION)) {
      solrInput.addField(SolrFields.DC_DESCRIPTION, dc.getFirst(DublinCore.PROPERTY_DESCRIPTION));
    }

    // dc:created
    if (mediaPackage.getDate() != null) {
      solrInput.addField(SolrFields.DC_CREATED, mediaPackage.getDate());
    } else if (dc.hasValue(DublinCore.PROPERTY_CREATED)) {
      DublinCoreValue created = dc.get(DublinCore.PROPERTY_CREATED).get(0);
      Date date = null;
      // TODO: Is there a (more performing) way to do this without try/catch?
      try {
        date = EncodingSchemeUtils.decodeMandatoryDate(dc.get(DublinCore.PROPERTY_CREATED).get(0));
      } catch (IllegalArgumentException e) {
        DCMIPeriod period = EncodingSchemeUtils.decodeMandatoryPeriod(created);
        if (period != null)
          date = period.getStart();
        else
          throw new IllegalArgumentException("Created date is neither a date nor a period");
      }
      solrInput.addField(SolrFields.DC_CREATED, date);
    }

    // dc:language
    if (dc.hasValue(DublinCore.PROPERTY_LANGUAGE)) {
      solrInput.addField(SolrFields.DC_LANGUAGE, getValue(dc.getFirst(DublinCore.PROPERTY_LANGUAGE), mediaPackage
              .getLanguage()));
    }

    // dc:rightsholder
    if (dc.hasValue(DublinCore.PROPERTY_RIGHTS_HOLDER)) {
      solrInput.addField(SolrFields.DC_RIGHTS_HOLDER, dc.getFirst(DublinCore.PROPERTY_RIGHTS_HOLDER));
    }

    // dc:spatial
    if (dc.hasValue(DublinCore.PROPERTY_SPATIAL)) {
      solrInput.addField(SolrFields.DC_SPATIAL, dc.getFirst(DublinCore.PROPERTY_SPATIAL));
    }

    // dc:temporal
    if (dc.hasValue(DublinCore.PROPERTY_TEMPORAL)) {
      solrInput.addField(SolrFields.DC_TEMPORAL, dc.getFirst(DublinCore.PROPERTY_TEMPORAL));
    }

    // dc:replaces
    if (dc.hasValue(DublinCore.PROPERTY_REPLACES)) {
      solrInput.addField(SolrFields.DC_REPLACES, dc.getFirst(DublinCore.PROPERTY_REPLACES));
    }

    // dc:type
    if (dc.hasValue(DublinCore.PROPERTY_TYPE)) {
      solrInput.addField(SolrFields.DC_TYPE, dc.getFirst(DublinCore.PROPERTY_TYPE));
    }

    // dc: accessrights
    if (dc.hasValue(DublinCore.PROPERTY_ACCESS_RIGHTS)) {
      solrInput.addField(SolrFields.DC_ACCESS_RIGHTS, dc.getFirst(DublinCore.PROPERTY_ACCESS_RIGHTS));
    }

    // dc:license
    if (dc.hasValue(DublinCore.PROPERTY_LICENSE)) {
      solrInput.addField(SolrFields.DC_LICENSE, getValue(dc.getFirst(DublinCore.PROPERTY_LICENSE), mediaPackage
              .getLicense()));
    }

    // dc:available
    if (dc.hasValue(DublinCore.PROPERTY_AVAILABLE)) {
      Object temporal = EncodingSchemeUtils.decodeTemporal(dc.get(DublinCore.PROPERTY_AVAILABLE).get(0));
      // FIXME a Temporal will never be a Date
      if (temporal instanceof Date) {
        solrInput.addField(SolrFields.DC_AVAILABLE_FROM, temporal);
      }
      // FIXME a Temporal will never be a DCMIPeriod
      if (temporal instanceof DCMIPeriod) {
        DCMIPeriod period = ((DCMIPeriod) temporal);
        if (period.hasStart()) {
          solrInput.addField(SolrFields.DC_AVAILABLE_FROM, period.getStart());
        }
        if (period.hasEnd()) {
          solrInput.addField(SolrFields.DC_AVAILABLE_TO, period.getEnd());
        }
      }
    }
  }

  /**
   * Add the mpeg 7 catalog data to the solr document.
   * 
   * @param solrInput
   *          the input document to the solr index
   * @param mpeg7
   *          the mpeg7 catalog
   */
  @SuppressWarnings("unchecked")
  private void addMpeg7Metadata(SolrUpdateableInputDocument solrInput, MediaPackage mediaPackage, Mpeg7Catalog mpeg7) {

    // Check for multimedia content
    if (!mpeg7.multimediaContent().hasNext()) {
      logger.warn("Mpeg-7 doesn't contain  multimedia content");
      return;
    }

    // Get the content duration by looking at the first content track. This
    // of course assumes that all tracks are equally long.
    MultimediaContent<? extends MultimediaContentType> mc = mpeg7.multimediaContent().next();
    MultimediaContentType mct = mc.elements().next();
    MediaTime mediaTime = mct.getMediaTime();
    solrInput.addField(SolrFields.DC_EXTENT, mediaTime.getMediaDuration().getDurationInMilliseconds());

    // Check if the keywords have been filled by (manually) added dublin
    // core data. If not, look for the most relevant fields in mpeg-7.
    SortedSet<TextAnnotation> sortedAnnotations = null;
    if (solrInput.getFieldValue(SolrFields.OC_KEYWORDS) == null) {
      sortedAnnotations = new TreeSet<TextAnnotation>(new Comparator<TextAnnotation>() {
        public int compare(TextAnnotation a1, TextAnnotation a2) {
          if ((RELEVANCE_BOOST * a1.getRelevance() + a1.getConfidence()) > (RELEVANCE_BOOST * a2.getRelevance() + a2
                  .getConfidence()))
            return -1;
          else if ((RELEVANCE_BOOST * a1.getRelevance() + a1.getConfidence()) < (RELEVANCE_BOOST * a2.getRelevance() + a2
                  .getConfidence()))
            return 1;
          return 0;
        }
      });
    }

    // Iterate over the tracks and extract keywords and hints
    Iterator<MultimediaContent<? extends MultimediaContentType>> mmIter = mpeg7.multimediaContent();
    int segmentCount = 0;

    while (mmIter.hasNext()) {
      MultimediaContent<?> multimediaContent = mmIter.next();

      // We need to process visual segments first, due to the way they are handled in the ui.
      for (Iterator<?> iterator = multimediaContent.elements(); iterator.hasNext();) {
        
        MultimediaContentType type = (MultimediaContentType) iterator.next();
        if (!(type instanceof Video) && !(type instanceof AudioVisual))
          continue;

        // for every segment in the current multimedia content track

        Video video = (Video)type;
        Iterator<VideoSegment> vsegments = (Iterator<VideoSegment>)video.getTemporalDecomposition().segments();
        while (vsegments.hasNext()) {
          VideoSegment segment = vsegments.next();

          StringBuffer segmentText = new StringBuffer();
          StringBuffer hintField = new StringBuffer();

          // Collect the video text elements to a segment text
          SpatioTemporalDecomposition spt = segment.getSpatioTemporalDecomposition();
          if (spt != null) {
            for (VideoText videoText : spt.getVideoText()) {
              if (segmentText.length() > 0)
                segmentText.append(" ");
              segmentText.append(videoText.getText().getText());
              // TODO: Add hint on bounding box
            }
          }

          // Add keyword annotations
          Iterator<TextAnnotation> textAnnotations = segment.textAnnotations();
          while (textAnnotations.hasNext()) {
            TextAnnotation textAnnotation = textAnnotations.next();
            Iterator<?> kwIter = textAnnotation.keywordAnnotations();
            while (kwIter.hasNext()) {
              KeywordAnnotation keywordAnnotation = (KeywordAnnotation) kwIter.next();
              if (segmentText.length() > 0)
                segmentText.append(" ");
              segmentText.append(keywordAnnotation.getKeyword());
            }
          }

          // Add free text annotations
          Iterator<TextAnnotation> freeIter = segment.textAnnotations();
          if (freeIter.hasNext()) {
            Iterator<FreeTextAnnotation> freeTextIter = freeIter.next().freeTextAnnotations();
            while (freeTextIter.hasNext()) {
              FreeTextAnnotation freeTextAnnotation = freeTextIter.next();
              if (segmentText.length() > 0)
                segmentText.append(" ");
              segmentText.append(freeTextAnnotation.getText());
            }
          }

          // add segment text to solr document
          solrInput.addField(SolrFields.SEGMENT_TEXT + segmentCount, segmentText.toString());

          // get the segments time properties
          MediaTimePoint timepoint = segment.getMediaTime().getMediaTimePoint();
          MediaDuration duration = segment.getMediaTime().getMediaDuration();

          // TODO: define a class with hint field constants
          hintField.append("time=" + timepoint.getTimeInMilliseconds() + "\n");
          hintField.append("duration=" + duration.getDurationInMilliseconds() + "\n");

          // Look for preview images. Their characteristics are that they are
          // attached as attachments with a flavor of preview/<something>.
          String time = timepoint.toString();
          for (Attachment slide : mediaPackage.getAttachments(MediaPackageElements.PRESENTATION_SEGMENT_PREVIEW)) {
            MediaPackageReference ref = slide.getReference();
            if (ref != null && time.equals(ref.getProperty("time"))) {
              hintField.append("preview");
              hintField.append(".");
              hintField.append(ref.getIdentifier());
              hintField.append("=");
              hintField.append(slide.getURI().toString());
              hintField.append("\n");
            }
          }

          logger.trace("Adding segment: " + timepoint.toString());
          solrInput.addField(SolrFields.SEGMENT_HINTS + segmentCount, hintField.toString());

          // increase segment counter
          segmentCount++;
        }
      }
    }

    // Put the most important keywords into a special solr field
    if (sortedAnnotations != null) {
      StringBuffer buf = importantKeywordsString(sortedAnnotations);
      solrInput.addField(SolrFields.OC_KEYWORDS, buf.toString().trim());
    }
  }

  /**
   * Generates a string with the most important kewords from the text annotation.
   * 
   * @param sortedAnnotations
   * @return The keyword string.
   */
  private StringBuffer importantKeywordsString(SortedSet<TextAnnotation> sortedAnnotations) {

    // important keyword:
    // - high relevance
    // - high confidence
    // - occur often
    // - more than MAX_CHAR chars

    // calculate keyword occurences (histogram) and importance
    ArrayList<String> list = new ArrayList<String>();
    Iterator<TextAnnotation> textAnnotations = sortedAnnotations.iterator();
    TextAnnotation textAnnotation = null;
    String keyword = null;

    HashMap<String, Integer> histogram = new HashMap<String, Integer>();
    HashMap<String, Double> importance = new HashMap<String, Double>();
    int occ = 0;
    double imp;
    while (textAnnotations.hasNext()) {
      textAnnotation = textAnnotations.next();
      Iterator<KeywordAnnotation> keywordAnnotations = textAnnotation.keywordAnnotations();
      while (keywordAnnotations.hasNext()) {
        KeywordAnnotation annotation = keywordAnnotations.next();
        keyword = annotation.getKeyword().toLowerCase();
        if (keyword.length() > MAX_CHAR) {
          occ = 0;
          if (histogram.keySet().contains(keyword)) {
            occ = histogram.get(keyword);
          }
          histogram.put(keyword, occ + 1);

          // here the importance value is calculated
          // from relevance, confidence and frequency of occurence.
          imp = (RELEVANCE_BOOST * getMaxRelevance(keyword, sortedAnnotations) + getMaxConfidence(keyword,
                  sortedAnnotations))
                  * (occ + 1);
          importance.put(keyword, imp);
        }
      }
    }

    // get the MAX_IMPORTANT_COUNT most important keywords
    StringBuffer buf = new StringBuffer();

    while (list.size() < MAX_IMPORTANT_COUNT && importance.size() > 0) {
      double max = 0.0;
      String maxKeyword = null;

      // get maximum from importance list
      for (Entry<String, Double> entry : importance.entrySet()) {
        keyword = entry.getKey();
        if (max < entry.getValue()) {
          max = entry.getValue();
          maxKeyword = keyword;
        }
      }

      // pop maximum
      importance.remove(maxKeyword);

      // append keyword to string
      if (buf.length() > 0)
        buf.append(" ");
      buf.append(maxKeyword);
    }

    return buf;
  }

  /**
   * Gets the maximum confidence for a given keyword in the text annotation.
   * 
   * @param keyword
   * @param sortedAnnotations
   * @return The maximum confidence value.
   */
  private double getMaxConfidence(String keyword, SortedSet<TextAnnotation> sortedAnnotations) {
    double max = 0.0;
    String needle = null;
    TextAnnotation textAnnotation = null;
    Iterator<TextAnnotation> textAnnotations = sortedAnnotations.iterator();
    while (textAnnotations.hasNext()) {
      textAnnotation = textAnnotations.next();
      Iterator<KeywordAnnotation> keywordAnnotations = textAnnotation.keywordAnnotations();
      while (keywordAnnotations.hasNext()) {
        KeywordAnnotation ann = keywordAnnotations.next();
        needle = ann.getKeyword().toLowerCase();
        if (keyword.equals(needle)) {
          if (max < textAnnotation.getConfidence()) {
            max = textAnnotation.getConfidence();
          }
        }
      }
    }
    return max;
  }

  /**
   * Gets the maximum relevance for a given keyword in the text annotation.
   * 
   * @param keyword
   * @param sortedAnnotations
   * @return The maximum relevance value.
   */
  private double getMaxRelevance(String keyword, SortedSet<TextAnnotation> sortedAnnotations) {
    double max = 0.0;
    String needle = null;
    TextAnnotation textAnnotation = null;
    Iterator<TextAnnotation> textAnnotations = sortedAnnotations.iterator();
    while (textAnnotations.hasNext()) {
      textAnnotation = textAnnotations.next();
      Iterator<KeywordAnnotation> keywordAnnotations = textAnnotation.keywordAnnotations();
      while (keywordAnnotations.hasNext()) {
        KeywordAnnotation ann = keywordAnnotations.next();
        needle = ann.getKeyword().toLowerCase();
        if (keyword.equals(needle)) {
          if (max < textAnnotation.getRelevance()) {
            max = textAnnotation.getRelevance();
          }
        }
      }
    }
    return max;
  }

}
