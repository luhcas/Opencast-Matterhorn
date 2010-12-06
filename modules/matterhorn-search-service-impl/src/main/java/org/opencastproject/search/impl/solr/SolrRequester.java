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

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.search.api.MediaSegment;
import org.opencastproject.search.api.MediaSegmentImpl;
import org.opencastproject.search.api.SearchQuery;
import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.api.SearchResultImpl;
import org.opencastproject.search.api.SearchResultItem.SearchResultItemType;
import org.opencastproject.search.api.SearchResultItemImpl;
import org.opencastproject.search.impl.SearchQueryImpl;
import org.opencastproject.util.SolrUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class implementing <code>LookupRequester</code> to provide connection to solr indexing facility.
 */
public class SolrRequester {

  /** Logging facility */
  private static Logger logger = LoggerFactory.getLogger(SolrRequester.class);

  /** The connection to the solr database */
  private SolrServer solrServer = null;

  /**
   * Creates a new requester for solr that will be using the given connection object to query the search index.
   * 
   * @param connection
   *          the solr connection
   */
  public SolrRequester(SolrServer connection) {
    if (connection == null)
      throw new IllegalStateException("Unable to run queries on null connection");
    this.solrServer = connection;
  }

  /**
   * Gets search results for a solr query string
   * 
   * @param q
   *          the query
   * @param limit
   *          the limit
   * @param offset
   *          the offset
   * @return the search results
   * @throws SolrServerException
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
      solrResponse = solrServer.query(query);
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
      item.setDcDescription(toString(doc.getFieldValue(SolrFields.DC_DESCRIPTION)));
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
          logger.warn("Unable to read media package from search result", e);
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
            logger.warn("Cannot parse duration from solr response document. Setting duration to -1.");
          }
        }

        // Add the list of most important keywords
        String kw[] = toString(doc.getFieldValue(SolrFields.OC_KEYWORDS)).split(" ");
        logger.trace(toString(doc.getFieldValue(SolrFields.OC_KEYWORDS)));
        for (String keyword : kw) {
          item.addKeyword(keyword);
        }

        // Loop over the segments
        for (MediaSegment segment : createSearchResultSegments(doc, query)) {
          item.addSegment(segment);
        }

      }

      // Add the item to the result set
      result.addItem(item);
    }

    return result;
  }

  /**
   * Creates a list of <code>MediaSegment</code>s from the given result document.
   * 
   * @param doc
   *          the result document
   * @param query
   *          the original query
   */
  private List<MediaSegmentImpl> createSearchResultSegments(SolrDocument doc, SolrQuery query) {
    List<MediaSegmentImpl> segments = new ArrayList<MediaSegmentImpl>();
    
    // The maximum number of hits in a segment
    int maxHits = 0;

    // Loop over every segment
    for (String fieldName : doc.getFieldNames()) {
      if (!fieldName.startsWith(SolrFields.SEGMENT_TEXT))
        continue;

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
        logger.warn("Cannot load hint properties.");
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

      // calculate the segment's relevance with respect to the query
      String queryText = query.getQuery();
      String segmentText = segment.getText();
      if (!StringUtils.isBlank(queryText) && !StringUtils.isBlank(segmentText)) {
        segmentText = segmentText.toLowerCase();
        Pattern p = Pattern.compile(".*fulltext:\\(([^)]*)\\).*");
        Matcher m = p.matcher(queryText);
        if (m.matches()) {
          String[] queryTerms = StringUtils.split(m.group(1).toLowerCase());
          int segmentHits = 0;
          int textLength = segmentText.length();
          for (String t : queryTerms) {
            int startIndex = 0;
            while (startIndex < textLength - 1) {
              int foundAt = segmentText.indexOf(t, startIndex);
              if (foundAt < 0)
                break;
              segmentHits++;
              startIndex = foundAt + t.length();
            }
          }
          
          // for now, just store the number of hits, but keep track of the maximum hit count
          segment.setRelevance(segmentHits);
          if (segmentHits > maxHits)
            maxHits = segmentHits;
        }
      }

      segments.add(segment);
    }
    
    for (MediaSegmentImpl segment : segments) {
      int hitsInSegment = segment.getRelevance();
      if (hitsInSegment > 0)
        segment.setRelevance((int)((100 * hitsInSegment) / maxHits));
    }

    return segments;
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

    sb.append(SolrFields.DC_DESCRIPTION);
    sb.append(":(");
    sb.append(uq);
    sb.append(")^");
    sb.append(SolrFields.DC_DESCRIPTION_BOOST);
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
   * Converts the query object into a solr query and returns the results.
   * 
   * @param q
   *          the query
   * @return the search results
   */
  public SearchResult getByQuery(SearchQuery q) throws SolrServerException {
    StringBuilder sb = new StringBuilder();

    String solrQueryRequest = q.getQuery();
    if (solrQueryRequest != null) {
      sb.append(q.getQuery());
    }

    String solrIdRequest = StringUtils.trimToNull(q.getId());
    if (solrIdRequest != null) {
      String cleanSolrIdRequest = SolrUtils.clean(solrIdRequest);
      if (sb.length() > 0)
        sb.append(" AND ");
      sb.append("(");
      sb.append(SolrFields.ID);
      sb.append(":");
      sb.append(cleanSolrIdRequest);
      if (q.isIncludeEpisodes()) {
        sb.append(" OR ");
        sb.append(SolrFields.DC_IS_PART_OF);
        sb.append(":");
        sb.append(cleanSolrIdRequest);
      }
      sb.append(")");
    }

    String solrTextRequest = StringUtils.trimToNull(q.getText());
    if (solrTextRequest != null) {
      String cleanSolrTextRequest = SolrUtils.clean(q.getText());
      if (StringUtils.isNotEmpty(cleanSolrTextRequest)) {
        if (sb.length() > 0)
          sb.append(" AND ");
        sb.append("*:");
        sb.append(boost(cleanSolrTextRequest));
      }
    }

    if (q.getElementTags() != null && q.getElementTags().length > 0) {
      if (sb.length() > 0)
        sb.append(" AND ");
      StringBuilder tagBuilder = new StringBuilder();
      for (int i = 0; i < q.getElementTags().length; i++) {
        String tag = SolrUtils.clean(q.getElementTags()[i]);
        if (StringUtils.isEmpty(tag))
          continue;
        if (tagBuilder.length() == 0) {
          tagBuilder.append("(");
        } else {
          tagBuilder.append(" OR ");
        }
        tagBuilder.append(SolrFields.OC_ELEMENTTAGS);
        tagBuilder.append(":");
        tagBuilder.append(tag);
      }
      if (tagBuilder.length() > 0) {
        tagBuilder.append(") ");
        sb.append(tagBuilder);
      }
    }

    if (q.getElementFlavors() != null && q.getElementFlavors().length > 0) {
      if (sb.length() > 0)
        sb.append(" AND ");
      StringBuilder flavorBuilder = new StringBuilder();
      for (int i = 0; i < q.getElementFlavors().length; i++) {
        String flavor = SolrUtils.clean(q.getElementFlavors()[i].toString());
        if (StringUtils.isEmpty(flavor))
          continue;
        if (flavorBuilder.length() == 0) {
          flavorBuilder.append("(");
        } else {
          flavorBuilder.append(" OR ");
        }
        flavorBuilder.append(SolrFields.OC_ELEMENTFLAVORS);
        flavorBuilder.append(":");
        flavorBuilder.append(flavor);
      }
      if (flavorBuilder.length() > 0) {
        flavorBuilder.append(") ");
        sb.append(flavorBuilder);
      }
    }

    if (sb.length() == 0)
      sb.append("*:*");

    SolrQuery query = new SolrQuery(sb.toString());

    if (q.isIncludeSeries() && ! q.isIncludeEpisodes()) {
      query.setFilterQueries(SolrFields.OC_MEDIATYPE + ":" + SearchResultItemType.Series);
    }

    if (q.isIncludeEpisodes() && ! q.isIncludeSeries()) {
      query.setFilterQueries(SolrFields.OC_MEDIATYPE + ":" + SearchResultItemType.AudioVisual);
    }

    if (q.getLimit() > 0)
      query.setRows(q.getLimit());

    if (q.getOffset() > 0)
      query.setStart(q.getOffset());

    if (q.isSortByPublicationDate()) {
      query.addSortField(SolrFields.OC_MODIFIED, ORDER.desc);
    } else if (q.isSortByCreationDate()) {
      query.addSortField(SolrFields.DC_CREATED, ORDER.desc);
      // If the dublin core field dc:created has not been filled in...
      query.addSortField(SolrFields.OC_MODIFIED, ORDER.desc);
    }

    query.setFields("* score");
    return createSearchResult(query);
  }

}
