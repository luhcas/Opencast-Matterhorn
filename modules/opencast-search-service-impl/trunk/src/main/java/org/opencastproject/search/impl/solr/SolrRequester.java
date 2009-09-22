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

import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageBuilder;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.api.SearchResultItem.SearchResultItemType;
import org.opencastproject.search.impl.MediaSegmentImpl;
import org.opencastproject.search.impl.SearchResultImpl;
import org.opencastproject.search.impl.SearchResultItemImpl;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

/**
 * Class implementing <code>LookupRequester</code> to provide connection to solr indexing facility.
 */
public class SolrRequester {

  /** Logging facility */
  private static Logger log_ = LoggerFactory.getLogger(SolrRequester.class);

  /** The regular filter expression */
  private static final String queryCleanerRegex = "[^0-9a-zA-ZöäüßÖÄÜ/\" +-.,]";

  /** The connection to the solr database */
  private SolrConnection solrConnection = null;

  /**
   * Creates a new requester for solr that will be using the given connection object to query the search index.
   * 
   * @param connection
   *          the solr connection
   */
  public SolrRequester(SolrConnection connection) {
    if (connection == null)
      throw new IllegalStateException("Unable to run queries on null connection");
    this.solrConnection = connection;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.search.impl.solr.Test#getEpisodesAndSeriesByText(java.lang.String, int, int)
   */
  public SearchResult getEpisodesAndSeriesByText(String text, int offset, int limit) throws SolrServerException {
    String uq = cleanQuery(text);
    StringBuffer sb = boost(uq);
    SolrQuery query = new SolrQuery(sb.toString());
    query.setStart(offset);
    query.setRows(limit);
    query.setFields("* score");
    return createSearchResult(query);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.search.impl.solr.Test#getEpisodesBySeries(java.lang.String)
   */
  public SearchResult getEpisodesBySeries(String seriesId) throws SolrServerException {
    String q = SolrFields.DC_IS_PART_OF + ":" + cleanQuery(seriesId);
    SolrQuery query = new SolrQuery(q);
    query.addSortField(SolrFields.DC_CREATED, ORDER.desc);
    query.setFields("* score");
    return createSearchResult(query);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.search.impl.solr.Test#getSeriesByDate(int, int)
   */
  public SearchResult getSeriesByDate(int limit, int offset) throws SolrServerException {
    String q = SolrFields.OC_MEDIATYPE + ":" + SearchResultItemType.Series;
    SolrQuery query = new SolrQuery(q);
    query.setStart(offset);
    query.addSortField(SolrFields.DC_CREATED, ORDER.desc);
    query.setRows(limit);
    query.setFields("* score");
    return createSearchResult(query);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.search.impl.solr.Test#getSeriesById(java.lang.String, int, int)
   */
  public SearchResult getSeriesById(String seriesId) throws SolrServerException {
    String q = SolrFields.OC_MEDIATYPE + ":" + SearchResultItemType.Series + " AND " + SolrFields.ID + ":" + seriesId;
    SolrQuery query = new SolrQuery(q);
    query.setFields("* score");
    return createSearchResult(query);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.search.impl.solr.Test#getSeriesByText(java.lang.String, int, int)
   */
  public SearchResult getSeriesByText(String text, int offset, int limit) throws SolrServerException {
    StringBuffer sb = boost(cleanQuery(text));
    SolrQuery query = new SolrQuery(sb.toString());
    query.setFilterQueries(SolrFields.OC_MEDIATYPE + ":" + SearchResultItemType.Series);
    query.setStart(offset);
    query.setRows(limit);
    query.setFields("* score");
    return createSearchResult(query);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.search.impl.solr.Test#getEpisodesAndSeriesById(java.lang.String)
   */
  public SearchResult getEpisodeAndSeriesById(String seriesId) throws SolrServerException {
    seriesId = cleanQuery(seriesId);
    String q = SolrFields.ID + ":" + seriesId + " OR " + SolrFields.DC_IS_PART_OF + ":" + seriesId;
    SolrQuery query = new SolrQuery(q);
    query.setSortField(SolrFields.OC_MEDIATYPE, SolrQuery.ORDER.asc);
    query.setSortField(SolrFields.DC_CREATED, SolrQuery.ORDER.desc);
    query.setFields("* score");
    return createSearchResult(query);
  }

  /**
   * Just for testing. Returns all solr entries as regular search result.
   * 
   * @param offset
   *          The offset.
   * @param limit
   *          The limit.
   * @return The regular result.
   * @throws SolrServerException
   */
  public SearchResult getEverything(int limit, int offset) throws SolrServerException {
    String q = "*:*";
    SolrQuery query = new SolrQuery(q);
    query.setStart(offset);
    query.setRows(limit);
    query.setFields("* score");
    return createSearchResult(query);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.search.impl.solr.Test#getEpisodeById(java.lang.String)
   */
  public SearchResult getEpisodeById(String episodeId) throws SolrServerException {
    String q = SolrFields.ID + ":" + episodeId;
    SolrQuery query = new SolrQuery(q);
    query.setFilterQueries(SolrFields.OC_MEDIATYPE + ":" + SearchResultItemType.AudioVisual);
    query.setFields("* score");
    return createSearchResult(query);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.search.impl.solr.Test#getEpisodesByDate(int, int)
   */
  public SearchResult getEpisodesByDate(int offset, int limit) throws SolrServerException {
    String q = SolrFields.OC_MEDIATYPE + ":" + SearchResultItemType.AudioVisual;
    SolrQuery query = new SolrQuery(q);
    query.addSortField(SolrFields.DC_CREATED, ORDER.desc);
    query.setStart(offset);
    query.setRows(limit);
    query.setFields("* score");
    return createSearchResult(query);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.search.impl.solr.Test#getEpisodesByText(java.lang.String, int, int)
   */
  public SearchResult getEpisodesByText(String text, int offset, int limit) throws SolrServerException {
    StringBuffer sb = boost(cleanQuery(text));
    SolrQuery query = new SolrQuery(sb.toString());
    query.setFilterQueries(SolrFields.OC_MEDIATYPE + ":" + SearchResultItemType.AudioVisual);
    query.setStart(offset);
    query.setRows(limit);
    query.setFields("* score");
    return createSearchResult(query);
  }

  /**
   * Creates a search result from a given solr response.
   * 
   * @param solrResponse
   *          The solr response.
   * @return The search result.
   * @throws SolrServerException
   *           if the solr server is not working as expected
   */
  private SearchResult createSearchResult(SolrQuery query) throws SolrServerException {

    // Execute the query and try to get hold of a query response
    QueryResponse solrResponse = null;
    try {
      solrResponse = solrConnection.request(query.toString());
    } catch (Exception e1) {
      throw new SolrServerException(e1);
    }

    // Create and configure the query result
    SearchResultImpl result = new SearchResultImpl(query.getQuery());
    result.setSearchTime(solrResponse.getQTime());
    result.setOffset(solrResponse.getResults().getStart());
    result.setLimit(solrResponse.getResults().getNumFound());

    // Walk through response and create new items with title, creator, etc:
    for (SolrDocument doc : solrResponse.getResults()) {

      SearchResultItemImpl item = new SearchResultItemImpl();
      item.setId(doc.getFieldValue(SolrFields.ID).toString());

      // the common dc fields (for series and episodes)
      item.setDcTitle(toString(doc.getFieldValue(SolrFields.DC_TITLE)));
      item.setDcSubject(toString(doc.getFieldValue(SolrFields.DC_SUBJECT)));
      item.setDcCreator(toString(doc.getFieldValue(SolrFields.DC_CREATOR)));
      item.setDcPublisher(toString(doc.getFieldValue(SolrFields.DC_PUBLISHER)));
      item.setDcContributor(toString(doc.getFieldValue(SolrFields.DC_CONTRIBUTOR)));
      item.setDcAbstract(toString(doc.getFieldValue(SolrFields.DC_ABSTRACT)));
      item.setDcCreated((Date) (doc.getFieldValue(SolrFields.DC_CREATED)));
      item.setDcAvailableFrom((Date) (doc.getFieldValue(SolrFields.DC_AVAILABLE_FROM)));
      item.setDcAvailableTo((Date) (doc.getFieldValue(SolrFields.DC_AVAILABLE_TO)));
      item.setDcLanguage(toString(doc.getFieldValue(SolrFields.DC_LANGUAGE)));
      item.setDcRightsHolder(toString(doc.getFieldValue(SolrFields.DC_RIGHTS_HOLDER)));
      item.setDcSpatial(toString(doc.getFieldValue(SolrFields.DC_SPATIAL)));
      item.setDcTemporal(toString(doc.getFieldValue(SolrFields.DC_TEMPORAL)));
      item.setDcIsPartOf(toString(doc.getFieldValue(SolrFields.DC_IS_PART_OF)));
      item.setDcReplaces(toString(doc.getFieldValue(SolrFields.DC_REPLACES)));
      item.setDcType(toString(doc.getFieldValue(SolrFields.DC_TYPE)));
      item.setDcAccessRights(toString(doc.getFieldValue(SolrFields.DC_ACCESS_RIGHTS)));
      item.setDcLicense(toString(doc.getFieldValue(SolrFields.DC_LICENSE)));

      // the media package
      MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
      Object mediaPackageFieldValue = doc.getFirstValue(SolrFields.OC_MEDIAPACKAGE);
      if (mediaPackageFieldValue != null) {
        try {
          MediaPackage mediaPackage = null;
          mediaPackage = builder.loadFromManifest(new ByteArrayInputStream(mediaPackageFieldValue.toString().getBytes()));
          item.setMediaPackage(mediaPackage);
        } catch (MediaPackageException e) {
          log_.warn("Unable to read media package from search result", e);
        }
      }

      // the media type
      item.setMediaType(SearchResultItemType.valueOf(doc.getFieldValue(SolrFields.OC_MEDIATYPE).toString()));

      // the cover image
      item.setCover(toString(doc.getFieldValue(SolrFields.OC_COVER)));

      // the solr ranking score
      item.setScore(Double.parseDouble(toString(doc.getFieldValue(SolrFields.SCORE))));

      // the last modified datetime
      item.setModified((Date) doc.getFieldValue(SolrFields.DC_CREATED));

      // if it is a video or audio episode
      if (item.getType().equals(SearchResultItemType.AudioVisual)) {
        try {
          // the video duration
          item.setDcExtent(Long.parseLong(toString(doc.getFieldValue(SolrFields.DC_EXTENT))));
        } catch (NumberFormatException e) {
          item.setDcExtent(-1);
          log_.warn("Cannot parse duration from solr response document. Setting duration to -1.");
        }

        // Add the list of most important keywords
        String kw[] = toString(doc.getFieldValue(SolrFields.OC_KEYWORDS)).split(" ");
        log_.trace(toString(doc.getFieldValue(SolrFields.OC_KEYWORDS)));
        for (String keyword : kw) {
          item.addKeyword(keyword);
        }

        // Loop over
        for (String fieldName : doc.getFieldNames()) {
          if (fieldName.startsWith(SolrFields.SEGMENT_TEXT)) {

            // Ceate a new segment
            int segmentId = Integer.parseInt(fieldName.substring(SolrFields.SEGMENT_TEXT.length()));
            MediaSegmentImpl segment = new MediaSegmentImpl(segmentId);
            segment.setText(toString(doc.getFieldValue(fieldName)));

            // Read the hints for this segment
            Properties segmentHints = new Properties();
            try {
              String hintFieldName = SolrFields.SEGMENT_HINTS + segment.getIndex();
              Object hintFieldValue = doc.getFieldValue(hintFieldName);
              segmentHints.load(new ByteArrayInputStream(hintFieldValue.toString().getBytes()));
            } catch (IOException e) {
              log_.warn("Cannot load hint properties.");
            }

            // get segment time
            String segmentTime = segmentHints.getProperty("time");
            if (segmentTime == null)
              throw new IllegalStateException("Found segment without time hint");
            segment.setTime(Long.parseLong(segmentTime));

            // the relevance
            segment.setRelevance((int) Math.round(Math.random() * 4));
            item.addSegment(segment);
          }
        }
      }

      // Add the item to the result set
      result.addItem(item);
    }

    return result;
  }

  /**
   * Modifies the query such that certain fields are being boosted (meaning they gain some weight).
   * 
   * @param query
   *          The user query.
   * @return The boosted query
   */
  public StringBuffer boost(String query) {
    String uq = cleanQuery(query);
    StringBuffer sb = new StringBuffer();

    sb.append("(");

    sb.append(SolrFields.DC_TITLE);
    sb.append(":(");
    sb.append(uq);
    sb.append(")^");
    sb.append(SolrFields.DC_TITLE_BOOST);
    sb.append(" ");

    sb.append(SolrFields.DC_CREATOR);
    sb.append(":(");
    sb.append(uq);
    sb.append(")^");
    sb.append(SolrFields.DC_CREATOR_BOOST);
    sb.append(" ");

    sb.append(SolrFields.DC_SUBJECT);
    sb.append(":(");
    sb.append(uq);
    sb.append(")^");
    sb.append(SolrFields.DC_SUBJECT_BOOST);
    sb.append(" ");

    sb.append(SolrFields.DC_PUBLISHER);
    sb.append(":(");
    sb.append(uq);
    sb.append(")^");
    sb.append(SolrFields.DC_PUBLISHER_BOOST);
    sb.append(" ");

    sb.append(SolrFields.DC_CONTRIBUTOR);
    sb.append(":(");
    sb.append(uq);
    sb.append(")^");
    sb.append(SolrFields.DC_CONTRIBUTOR_BOOST);
    sb.append(" ");

    sb.append(SolrFields.DC_ABSTRACT);
    sb.append(":(");
    sb.append(uq);
    sb.append(")^");
    sb.append(SolrFields.DC_ABSTRACT_BOOST);
    sb.append(" ");

    sb.append(SolrFields.FULLTEXT);
    sb.append(":(");
    sb.append(uq);
    sb.append(") ");

    sb.append(")");

    return sb;
  }

  /**
   * Clean up the user query input string to avoid invalid input parameters.
   * 
   * @param q
   *          The input String.
   * @return The cleaned string.
   */
  protected String cleanQuery(String q) {
    return q.replaceAll(queryCleanerRegex, " ").trim();
  }

  /**
   * Simple helper method to avoid null strings.
   * 
   * @param An
   *          object which implements <code>toString()</code> method.
   * @return The input object or empty string.
   */
  private String toString(Object f) {
    if (f != null)
      return f.toString();
    else
      return "";
  }

}