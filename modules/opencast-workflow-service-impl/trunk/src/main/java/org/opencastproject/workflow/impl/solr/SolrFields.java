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

package org.opencastproject.workflow.impl.solr;

/**
 * Interface defining the mapping between data and field names in solr.
 */
public interface SolrFields {

  public static final String WORKFLOW_ID = "id";

  // Dublin core fields
  public static final String DC_IDENTIFIER = "dc_identifier";
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
  public static final String OC_MEDIA_PACKAGE_ID = "oc_mediapackage_id";
  public static final String OC_WORKFLOW_INSTANCE = "oc_workflow_instance";
  public static final String OC_OPERATIONS = "oc_operations";
  public static final String OC_PROPERTIES = "oc_properties";
  public static final String OC_CURRENT_OPERATION = "oc_current_operation";
  public static final String OC_MODIFIED = "oc_modified";
  public static final String OC_STATE = "oc_state";

  /** Accumulative fulltext field */
  public static final String FULLTEXT = "fulltext";

  /** The solr date format string tag. */
  public static final String SOLR_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

}