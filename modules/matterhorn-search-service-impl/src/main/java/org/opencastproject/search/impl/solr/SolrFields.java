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

/**
 * Interface defining the mapping between data and field names in solr.
 */
public interface SolrFields {

  String ID = "id";

  // Dublin core fields
  String DC_EXTENT = "dc_extent";
  String DC_TITLE = "dc_title";
  String DC_SUBJECT = "dc_subject";
  String DC_CREATOR = "dc_creator";
  String DC_PUBLISHER = "dc_publisher";
  String DC_CONTRIBUTOR = "dc_contributor";
  String DC_ABSTRACT = "dc_abstract";
  String DC_DESCRIPTION = "dc_description";
  String DC_CREATED = "dc_created";
  String DC_AVAILABLE_FROM = "dc_available_from";
  String DC_AVAILABLE_TO = "dc_available_to";
  String DC_LANGUAGE = "dc_language";
  String DC_RIGHTS_HOLDER = "dc_rights_holder";
  String DC_SPATIAL = "dc_spatial";
  String DC_TEMPORAL = "dc_temporal";
  String DC_IS_PART_OF = "dc_is_part_of";
  String DC_REPLACES = "dc_replaces";
  String DC_TYPE = "dc_type";
  String DC_ACCESS_RIGHTS = "dc_access_rights";
  String DC_LICENSE = "dc_license";

  // Additional fields
  String OC_MEDIAPACKAGE = "oc_mediapackage";
  String OC_KEYWORDS = "oc_keywords";
  String OC_COVER = "oc_cover";
  String OC_MODIFIED = "oc_modified";
  String OC_DELETED = "oc_deleted";
  String OC_MEDIATYPE = "oc_mediatype";
  String OC_ELEMENTTAGS = "oc_elementtags";
  String OC_ELEMENTFLAVORS = "oc_elementflavors";
  String OC_READ_PERMISSIONS = "oc_read";
  String OC_WRITE_PERMISSIONS = "oc_write";
  
  /** Solr ranking score */
  String SCORE = "score";

  /** Accumulative fulltext field */
  String FULLTEXT = "fulltext";

  /** Just a constant to set the solr dynamic field name for segment text. */
  String SEGMENT_TEXT = "oc_text_";

  /** Just a constant to set the solr dynamic field name for segment hints. */
  String SEGMENT_HINTS = "oc_hint_";

  /** The solr date format string tag. */
  String SOLR_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

  /** The solr highlighting tag to use. */
  String HIGHLIGHT_MATCH = "b";

  /** Boost values for ranking */
  // TODO: move this to configuration file
  double DC_TITLE_BOOST = 6.0;
  double DC_ABSTRACT_BOOST = 4.0;
  double DC_DESCRIPTION_BOOST = 4.0;
  double DC_CONTRIBUTOR_BOOST = 2.0;
  double DC_PUBLISHER_BOOST = 2.0;
  double DC_CREATOR_BOOST = 4.0;
  double DC_SUBJECT_BOOST = 4.0;

}
