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

import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageBuilder;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.search.api.MediaSegmentImpl;
import org.opencastproject.search.api.SearchQuery;
import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.api.SearchResultImpl;
import org.opencastproject.search.api.SearchResultItemImpl;
import org.opencastproject.search.api.SearchResultItem.SearchResultItemType;
import org.opencastproject.search.impl.SearchQueryImpl;

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
import java.util.Map.Entry;

/**
 * Class implementing <code>LookupRequester</code> to provide connection to solr indexing facility.
 */
public class SolrRequester {

  /** Logging facility */
  private static Logger log_ = LoggerFactory.getLogger(SolrRequester.class);

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

//  /**
//   * {@inheritDoc}
//   * 
//   * @see org.opencastproject.search.api.SearchService#getEpisodesAndSeriesByText(java.lang.String, int, int)
//   */
//  public SearchResult getEpisodesAndSeriesByText(String text, int limit, int offset) throws SolrServerException {
//    SearchQueryImpl q1 = new SearchQueryImpl();
//    q1.withText(text).withLimit(limit).withOffset(offset).includeSeries(true);
//    return getByQuery(q1);
//  }
//
//  /**
//   * {@inheritDoc}
//   * 
//   * @see org.opencastproject.search.api.SearchService#getEpisodesBySeries(java.lang.String)
//   */
//  public SearchResult getEpisodesBySeries(String seriesId) throws SolrServerException {
//    SearchQueryImpl q1 = new SearchQueryImpl();
//    q1.withId(seriesId).includeSeries(false).includeEpisodes(true);
//    
//    return getByQuery(q1);
//  }
//
//  /**
//   * {@inheritDoc}
//   * 
//   * @see org.opencastproject.search.api.SearchService#getSeriesByDate(int, int)
//   */
//  public SearchResult getSeriesByDate(int limit, int offset) throws SolrServerException {
//    SearchQueryImpl q1 = new SearchQueryImpl();
//    q1.withLimit(limit).withOffset(offset).includeSeries(true).includeEpisodes(false);
//    q1.withCreationDateSort(true);
//    return getByQuery(q1);
//  }
//
//  /**
//   * {@inheritDoc}
//   * 
//   * @see org.opencastproject.search.api.SearchService#getSeriesById(java.lang.String, int, int)
//   */
//  public SearchResult getSeriesById(String seriesId) throws SolrServerException {
//    SearchQueryImpl q1 = new SearchQueryImpl();
//    q1.withId(seriesId).includeSeries(true).includeEpisodes(false);
//    return getByQuery(q1);
//  }
//
//  /**
//   * {@inheritDoc}
//   * 
//   * @see org.opencastproject.search.api.SearchService#getSeriesByText(java.lang.String, int, int)
//   */
//  public SearchResult getSeriesByText(String text, int limit, int offset) throws SolrServerException {
//    SearchQueryImpl q1 = new SearchQueryImpl();
//    q1.withText(text).withLimit(limit).withOffset(offset).includeSeries(true).includeEpisodes(false);
//    return getByQuery(q1);
//  }
//
//  /**
//   * {@inheritDoc}
//   * 
//   * @see org.opencastproject.search.api.SearchService#getEpisodesAndSeriesById(java.lang.String)
//   */
//  public SearchResult getEpisodeAndSeriesById(String seriesId) throws SolrServerException {
//    SearchQueryImpl q1 = new SearchQueryImpl();
//    q1.withId(seriesId).includeEpisodes(true).includeSeries(true);
//    return getByQuery(q1);
//  }
//
//  /**
//   * {@inheritDoc}
//   * 
//   * @see org.opencastproject.search.api.SearchService#getEpisodeById(java.lang.String)
//   */
//  public SearchResult getEpisodeById(String episodeId) throws SolrServerException {
//    SearchQueryImpl q1 = new SearchQueryImpl();
//    q1.withId(episodeId).includeEpisodes(true);
//    return getByQuery(q1);
//  }
//
//  /**
//   * {@inheritDoc}
//   * 
//   * @see org.opencastproject.search.api.SearchService#getEpisodesByDate(int, int)
//   */
//  public SearchResult getEpisodesByDate(int limit, int offset) throws SolrServerException {
//    SearchQueryImpl q1 = new SearchQueryImpl();
//    q1.withLimit(limit).withOffset(offset);
//    q1.withCreationDateSort(true);
//    return getByQuery(q1);
//  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.search.api.SearchService#getByQuery(java.lang.String, int, int)
   */
  public SearchResult getByQuery(String q, int limit, int offset) throws SolrServerException {
    SearchQueryImpl q1 = new SearchQueryImpl();
    q1.withQuery(q).withLimit(limit).withOffset(offset);
    return getByQuery(q1);
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
    result.setLimit(solrResponse.getResults().size());
    result.setTotal(solrResponse.getResults().getNumFound());

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
          mediaPackage = builder.loadFromXml(mediaPackageFieldValue.toString());
          item.setMediaPackage(mediaPackage);
        } catch (Exception e) {
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
      item.setModified((Date) doc.getFieldValue(SolrFields.OC_MODIFIED));

      // if it is a video or audio episode
      if (item.getType().equals(SearchResultItemType.AudioVisual)) {
        if (doc.getFieldValue(SolrFields.DC_EXTENT) != null) {
          try {
            // the video duration
            item.setDcExtent(Long.parseLong(toString(doc.getFieldValue(SolrFields.DC_EXTENT))));
          } catch (NumberFormatException e) {
            item.setDcExtent(-1);
            log_.warn("Cannot parse duration from solr response document. Setting duration to -1.");
          }
        }

        // Add the list of most important keywords
        String kw[] = toString(doc.getFieldValue(SolrFields.OC_KEYWORDS)).split(" ");
        log_.trace(toString(doc.getFieldValue(SolrFields.OC_KEYWORDS)));
        for (String keyword : kw) {
          item.addKeyword(keyword);
        }

        // Loop over the segments
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

            // get segment duration
            String segmentDuration = segmentHints.getProperty("duration");
            if (segmentDuration == null)
              throw new IllegalStateException("Found segment without duration hint");
            segment.setDuration(Long.parseLong(segmentDuration));
            
            // get preview urls
            for (Entry<Object, Object> entry : segmentHints.entrySet()) {
              if (entry.getKey().toString().startsWith("preview.")) {
                String parts[] = entry.getKey().toString().split("\\.");
                segment.addPreview(entry.getValue().toString(), parts[1]);
              }
            }

            // the relevance
            // TODO: Add real relevance
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
    String uq = SolrUtils.clean(query);
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

  /**
   * @param q
   * @return
   */
  public SearchResult getByQuery(SearchQuery q) throws SolrServerException {
    StringBuilder sb = new StringBuilder();
    
    if (q.getQuery() != null) {
      sb.append(q.getQuery());
    }
    
    if(q.getId() != null) {
      if (sb.length() > 0)
        sb.append(" AND ");
      sb.append("(");
      sb.append(SolrFields.ID);
      sb.append(":");
      sb.append(q.getId());
      if(q.isIncludeSeries() && q.isIncludeEpisodes()) {
        sb.append(" OR ");
        sb.append(SolrFields.DC_IS_PART_OF);
        sb.append(":");
        sb.append(q.getId());
      }
      sb.append(")");
    }
    
    if (q.getText() != null) {
      if (sb.length() > 0)
        sb.append(" AND ");
      sb.append("*:");
      sb.append(boost(SolrUtils.clean(q.getText())));      
    }
    
    if(q.getElementTags() != null && q.getElementTags().length > 0) {
      if (sb.length() > 0)
        sb.append(" AND ");
      sb.append("(");
      for(int i=0; i<q.getElementTags().length; i++) {
        if(i>0) {
          sb.append(" OR ");
        }
        sb.append(SolrFields.OC_ELEMENTTAGS);
        sb.append(":");
        sb.append(q.getElementTags()[i]);
      }
      sb.append(") ");
    }

    if(q.getElementFlavors() != null && q.getElementFlavors().length > 0) {
      if (sb.length() > 0)
        sb.append(" AND ");
      sb.append("(");
      for(int i=0; i<q.getElementFlavors().length; i++) {
        if(i>0) {
          sb.append(" OR ");
        }
        sb.append(SolrFields.OC_ELEMENTFLAVORS);
        sb.append(":");
        sb.append(q.getElementFlavors()[i]);
      }
      sb.append(") ");
    }
    
    if (sb.length() == 0)
      sb.append("*:*");
    
    SolrQuery query = new SolrQuery(sb.toString());
    
    if (q.isIncludeSeries()) {
      query.setFilterQueries(SolrFields.OC_MEDIATYPE + ":" + SearchResultItemType.Series);
    } 
    
    if (q.isIncludeEpisodes()) {
      query.setFilterQueries(SolrFields.OC_MEDIATYPE + ":" + SearchResultItemType.AudioVisual);
    }
        
    if (q.getLimit() > 0)
      query.setRows(q.getLimit());
    
    if (q.getOffset() > 0)
      query.setStart(q.getOffset());
    
    if(q.isSortByCreationDate()) {
      query.addSortField(SolrFields.DC_CREATED, ORDER.desc);
      query.addSortField(SolrFields.OC_MODIFIED, ORDER.desc);
    }

    query.setFields("* score");
    return createSearchResult(query);
  }

}
