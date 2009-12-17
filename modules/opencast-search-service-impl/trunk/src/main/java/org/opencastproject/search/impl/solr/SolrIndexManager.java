/**
 *  Copyright 2009 The Regents of the University of California
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

import org.opencastproject.media.mediapackage.Catalog;
import org.opencastproject.media.mediapackage.Cover;
import org.opencastproject.media.mediapackage.DublinCoreCatalog;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.opencastproject.media.mediapackage.MediaPackageReferenceImpl;
import org.opencastproject.media.mediapackage.Mpeg7Catalog;
import org.opencastproject.media.mediapackage.dublincore.DublinCore;
import org.opencastproject.media.mediapackage.dublincore.DublinCoreValue;
import org.opencastproject.media.mediapackage.dublincore.utils.DCMIPeriod;
import org.opencastproject.media.mediapackage.dublincore.utils.EncodingSchemeUtils;
import org.opencastproject.media.mediapackage.mpeg7.ContentSegment;
import org.opencastproject.media.mediapackage.mpeg7.KeywordAnnotation;
import org.opencastproject.media.mediapackage.mpeg7.MediaTime;
import org.opencastproject.media.mediapackage.mpeg7.MediaTimePoint;
import org.opencastproject.media.mediapackage.mpeg7.MultimediaContent;
import org.opencastproject.media.mediapackage.mpeg7.MultimediaContentType;
import org.opencastproject.media.mediapackage.mpeg7.TextAnnotation;
import org.opencastproject.search.api.SearchResultItem.SearchResultItemType;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.request.UpdateRequest.ACTION;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
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

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Utility class used to manage the search index.
 */
public class SolrIndexManager {

  /** Logging facility */
  static Logger log_ = LoggerFactory.getLogger(SolrIndexManager.class);

  /** Connection to the database */
  private SolrConnection solrConnection = null;

  /** Annotations with a lower confidence will be excluded from the search index */
  // TODO: Read from store configuration
  private static final float CONFIDENCE_THRESHOLD = 0.0f;

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

  /**
   * Creates a new management instance for the search index.
   * 
   * @param connection
   *          connection to the database
   */
  public SolrIndexManager(SolrConnection connection) {
    if (connection == null)
      throw new IllegalArgumentException("Unable to manage solr with null connection");
    this.solrConnection = connection;
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
      log_.error("Cannot clear solr index", e);
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
      log_.error("Cannot clear solr index");
      return false;
    }
  }

  /**
   * Posts the media package to solr. Depending on what is referenced in the media package, the method might create one
   * or two entries: one for the episode and one for the series that the episode belongs to.
   * 
   * @param mediaPackage
   *          the media package to post
   * @throws SolrServerException
   *           if an errors occurs while talking to solr
   */
  public boolean add(MediaPackage mediaPackage) throws SolrServerException {
    UpdateRequest solrRequest = new UpdateRequest();
    solrRequest.setAction(ACTION.COMMIT, true, true);
    SolrUpdateableInputDocument episodeDocument = createEpisodeInputDocument(mediaPackage);
    SolrUpdateableInputDocument seriesDocument = createSeriesInputDocument(mediaPackage);

    // If neither an episode nor a series was contained, there is no point in trying to update
    if (episodeDocument == null && seriesDocument == null)
      return false;

    // Add the episode metadata
    if (episodeDocument != null) {
      if (seriesDocument != null)
        episodeDocument.setField(SolrFields.DC_IS_PART_OF, seriesDocument.getField(SolrFields.ID));
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
      log_.error("Cannot clear solr index");
      return false;
    }
  }

  /**
   * Creates a solr input document for the episode metadata of the media package.
   * 
   * @param mediaPackage
   *          the media package
   * @return an input document ready to be posted to solr
   */
  private SolrUpdateableInputDocument createEpisodeInputDocument(MediaPackage mediaPackage) {
    SolrUpdateableInputDocument solrEpisodeDocument = new SolrUpdateableInputDocument();
    String mediaPackageId = mediaPackage.getIdentifier().toString();

    // Populate document with existing data
    try {
      StringBuffer query = new StringBuffer("q=");
      query = query.append(SolrFields.ID).append(":").append(SolrUtils.clean(mediaPackageId));
      QueryResponse solrResponse = solrConnection.request(query.toString());
      if (solrResponse.getResults().size() > 0) {
        SolrDocument existingsolrDocument = solrResponse.getResults().get(0);
        for (String fieldName : existingsolrDocument.getFieldNames()) {
          solrEpisodeDocument.addField(fieldName, existingsolrDocument.getFieldValue(fieldName));
        }
      }
    } catch (Exception e) {
      log_.error("Error trying to load series " + mediaPackageId, e);
    }

    // Add dublin core
    if (!mediaPackage.hasCatalogs(DublinCoreCatalog.FLAVOR, MediaPackageReferenceImpl.ANY_MEDIAPACKAGE)) {
      log_.debug("No episode dublincore metadata found in media package " + mediaPackage);
      return null;
    }

    // If this is the case, try to get a hold on it
    Catalog dcCatalogs[] = mediaPackage.getCatalogs(DublinCoreCatalog.FLAVOR,
            MediaPackageReferenceImpl.ANY_MEDIAPACKAGE);
    DublinCoreCatalog dublinCore = (DublinCoreCatalog) dcCatalogs[0];

    // Set common fields
    solrEpisodeDocument.setField(SolrFields.ID, mediaPackageId);
    solrEpisodeDocument.setField(SolrFields.OC_MEDIATYPE, SearchResultItemType.AudioVisual);
    addStandardDublincCoreFields(solrEpisodeDocument, dublinCore);

    // Add media package
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      DOMSource domSource = new DOMSource(mediaPackage.toXml());
      StreamResult streamResult = new StreamResult(out);
      TransformerFactory tf = TransformerFactory.newInstance();
      Transformer serializer = tf.newTransformer();
      serializer.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");
      serializer.transform(domSource, streamResult);
      solrEpisodeDocument.setField(SolrFields.OC_MEDIAPACKAGE, out);
    } catch (MediaPackageException e) {
      throw new IllegalStateException("Error serializing media package to search index", e);
    } catch (TransformerConfigurationException e) {
      throw new IllegalStateException("Error serializing media package to search index", e);
    } catch (TransformerException e) {
      throw new IllegalStateException("Error serializing media package to search index", e);
    }

    // Add cover
    Cover cover = mediaPackage.getCover();
    if (cover != null) {
      solrEpisodeDocument.addField(SolrFields.OC_COVER, cover.getIdentifier());
    }

    // Add mpeg-7
    // TODO: Merge mpeg-7 catalogs prior to adding them to solr
    /*
    Catalog[] mpeg7Catalogs = mediaPackage.getCatalogs(Mpeg7Catalog.FLAVOR);
    for (Catalog mpeg7Catalog : mpeg7Catalogs) {
      addMpeg7Metadata(solrEpisodeDocument, (Mpeg7Catalog) mpeg7Catalog);
    }
    */

    return solrEpisodeDocument;
  }

  /**
   * Creates a solr input document for the series metadata of the media package.
   * 
   * @param mediaPackage
   *          the media package
   * @return an input document ready to be posted to solr
   */
  private SolrUpdateableInputDocument createSeriesInputDocument(MediaPackage mediaPackage) {
    SolrUpdateableInputDocument solrSeriesDocument = new SolrUpdateableInputDocument();

    // Check if there is a dublin core for series
    if (!mediaPackage.hasCatalogs(DublinCoreCatalog.FLAVOR, MediaPackageReferenceImpl.ANY_SERIES)) {
      log_.debug("No series dublincore found in media package " + mediaPackage);
      return null;
    }

    // If this is the case, try to get a hold on it
    Catalog dcCatalogs[] = mediaPackage.getCatalogs(DublinCoreCatalog.FLAVOR, MediaPackageReferenceImpl.ANY_SERIES);
    DublinCoreCatalog dublinCore = (DublinCoreCatalog) dcCatalogs[0];
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
      log_.error("Error trying to load series " + seriesId, e);
    }

    // Set common fields
    solrSeriesDocument.setField(SolrFields.ID, seriesId);
    solrSeriesDocument.setField(SolrFields.OC_MEDIATYPE, SearchResultItemType.Series);
    addStandardDublincCoreFields(solrSeriesDocument, dublinCore);

    return solrSeriesDocument;
  }

  /**
   * Adds the standard dublin core fields to the solr document.
   * 
   * @param solrInput
   *          the input document
   * @param dc
   *          the dublin core catalog
   */
  private void addStandardDublincCoreFields(SolrUpdateableInputDocument solrInput, DublinCoreCatalog dc) {
    if (!dc.hasValue(DublinCore.PROPERTY_TITLE))
      throw new IllegalStateException("Found dublin core catalog withouth title");

    solrInput.addField(SolrFields.DC_TITLE, dc.getFirst(DublinCore.PROPERTY_TITLE));

    // dc:subject
    if (dc.hasValue(DublinCore.PROPERTY_SUBJECT)) {
      solrInput.addField(SolrFields.DC_SUBJECT, dc.getFirst(DublinCore.PROPERTY_SUBJECT));
    }

    // dc:creator
    if (dc.hasValue(DublinCore.PROPERTY_CREATOR)) {
      solrInput.addField(SolrFields.DC_CREATOR, dc.getFirst(DublinCore.PROPERTY_CREATOR));
    }

    // dc:publisher
    if (dc.hasValue(DublinCore.PROPERTY_PUBLISHER)) {
      solrInput.addField(SolrFields.DC_PUBLISHER, dc.getFirst(DublinCore.PROPERTY_PUBLISHER));
    }

    // dc:contributor
    if (dc.hasValue(DublinCore.PROPERTY_CONTRIBUTOR)) {
      solrInput.addField(SolrFields.DC_CONTRIBUTOR, dc.getFirst(DublinCore.PROPERTY_CONTRIBUTOR));
    }

    // dc:abstract
    if (dc.hasValue(DublinCore.PROPERTY_ABSTRACT)) {
      solrInput.addField(SolrFields.DC_ABSTRACT, dc.getFirst(DublinCore.PROPERTY_ABSTRACT));
    }

    // dc:created
    if (dc.hasValue(DublinCore.PROPERTY_CREATED)) {
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
      solrInput.addField(SolrFields.DC_LANGUAGE, dc.getFirst(DublinCore.PROPERTY_LANGUAGE));
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
      solrInput.addField(SolrFields.DC_LICENSE, dc.getFirst(DublinCore.PROPERTY_LICENSE));
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
  @SuppressWarnings("unused")
  private void addMpeg7Metadata(SolrUpdateableInputDocument solrInput, Mpeg7Catalog mpeg7) {

    // Check for multimedia content
    if (!mpeg7.multimediaContent().hasNext()) {
      log_.warn("Mpeg-7 doesn't contain  multimedia content");
      return;
    }

    // Get the content duration by looking at the first content track. This
    // of course assumes that all tracks are equally long.
    MultimediaContent<? extends MultimediaContentType> mc = mpeg7.multimediaContent().next();
    MediaTime mediaTime = mc.elements().next().getMediaTime();
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

      // for every multimedia content track
      for (Iterator<?> iterator = multimediaContent.elements(); iterator.hasNext();) {
        MultimediaContentType type = (MultimediaContentType) iterator.next();

        // for every segment in the current multimedia content track

        Iterator<? extends ContentSegment> ctIter = type.getTemporalDecomposition().segments();
        while (ctIter.hasNext()) {
          ContentSegment contentSegment = ctIter.next();

          // collect the keywords to a segment text
          StringBuffer segmentText = new StringBuffer();

          // Iterate over all text annotations
          Iterator<TextAnnotation> textAnnotations = contentSegment.textAnnotations();
          while (textAnnotations.hasNext()) {
            TextAnnotation textAnnotation = textAnnotations.next();

            // Skip annotations with confidence < threshold
            if (textAnnotation.getConfidence() <= CONFIDENCE_THRESHOLD)
              continue;

            // check if we are collecting the most important keywords
            if (sortedAnnotations != null)
              sortedAnnotations.add(textAnnotation);

            // for every keyword annotation
            Iterator<?> kwIter = textAnnotation.keywordAnnotations();
            while (kwIter.hasNext()) {
              KeywordAnnotation keywordAnnotation = (KeywordAnnotation) kwIter.next();
              segmentText.append(keywordAnnotation.getKeyword());
              segmentText.append(" ");
            }

            // TODO: process free text annotations
          }

          // add segment text to solr document
          solrInput.addField(SolrFields.SEGMENT_TEXT + segmentCount, segmentText.toString());

          // get the segments time point
          MediaTimePoint timepoint = contentSegment.getMediaTime().getMediaTimePoint();
          // System.out.println("MediaTimePoint: "+timepoint.getTimeInMilliseconds());
          // dont forget: hints are stores as properties
          StringBuffer hintField = new StringBuffer();

          // TODO: define a class with hint field constants
          hintField.append("time=" + timepoint.getTimeInMilliseconds() + "\n");
          // hintField.append("relevance="+timepoint.getTimeInMilliseconds() + "\n");

          log_.trace("Adding segment: " + timepoint.getTimeInMilliseconds());
          // add freetext annotation to solr document
          // Iterator<TextAnnotation> freeIter = contentSegment.textAnnotations();
          // if (freeIter.hasNext()) {
          // FreeTextAnnotation freeTextAnnotation = freeIter.next();
          // hintField.append(freeTextAnnotation.getText() + "\n");
          // }
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

    // dont forget the current time in milliseconds
    solrInput.addField(SolrFields.OC_MODIFIED, dateFormat.format((new Date()).getTime()));
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
      for(Entry<String, Double> entry : importance.entrySet()) {
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
