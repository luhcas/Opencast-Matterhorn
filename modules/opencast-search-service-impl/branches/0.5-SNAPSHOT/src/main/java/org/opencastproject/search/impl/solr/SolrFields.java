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

/**
 * Interface defining the mapping between data and field names in solr.
 */
public interface SolrFields {

  public static final String ID = "id";

  // Dublin core fields
  public static final String DC_EXTENT = "dc_extent";
  public static final String DC_TITLE = "dc_title";
  public static final String DC_SUBJECT = "dc_subject";
  public static final String DC_CREATOR = "dc_creator";
  public static final String DC_PUBLISHER = "dc_publisher";
  public static final String DC_CONTRIBUTOR = "dc_contributor";
  public static final String DC_ABSTRACT = "dc_abstract";
  public static final String DC_CREATED = "dc_created";
  public static final String DC_AVAILABLE_FROM = "dc_available_from";
  public static final String DC_AVAILABLE_TO = "dc_available_to";
  public static final String DC_LANGUAGE = "dc_language";
  public static final String DC_RIGHTS_HOLDER = "dc_rights_holder";
  public static final String DC_SPATIAL = "dc_spatial";
  public static final String DC_TEMPORAL = "dc_temporal";
  public static final String DC_IS_PART_OF = "dc_is_part_of";
  public static final String DC_REPLACES = "dc_replaces";
  public static final String DC_TYPE = "dc_type";
  public static final String DC_ACCESS_RIGHTS = "dc_access_rights";
  public static final String DC_LICENSE = "dc_license";

  // Additional fields
  public static final String OC_MEDIAPACKAGE = "oc_mediapackage";
  public static final String OC_KEYWORDS = "oc_keywords";
  public static final String OC_COVER = "oc_cover";
  public static final String OC_MODIFIED = "oc_modified";
  public static final String OC_MEDIATYPE = "oc_mediatype";
  public static final String OC_ELEMENTTAGS = "oc_elementtags";
  public static final String OC_ELEMENTFLAVORS = "oc_elementflavors";

  /** Solr ranking score */
  public static final String SCORE = "score";

  /** Accumulative fulltext field */
  public static final String FULLTEXT = "fulltext";

  /** Just a constant to set the solr dynamic field name for segment text. */
  public static final String SEGMENT_TEXT = "oc_text_";

  /** Just a constant to set the solr dynamic field name for segment hints. */
  public static final String SEGMENT_HINTS = "oc_hint_";

  /** The solr date format string tag. */
  public static final String SOLR_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

  /** The solr highlighting tag to use. */
  public static final String HIGHLIGHT_MATCH = "b";

  /** Boost values for ranking */
  // TODO: move this to configuration file
  public static final double DC_TITLE_BOOST = 6.0;
  public static final double DC_ABSTRACT_BOOST = 4.0;
  public static final double DC_CONTRIBUTOR_BOOST = 2.0;
  public static final double DC_PUBLISHER_BOOST = 2.0;
  public static final double DC_CREATOR_BOOST = 4.0;
  public static final double DC_SUBJECT_BOOST = 4.0;

}
