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

import org.apache.solr.common.SolrInputDocument;


/**
 * Solr input document which adds not existing fields.
 */
public class SolrUpdateableInputDocument extends SolrInputDocument {

  /** Serieal version id */
  private static final long serialVersionUID = -1984560839468950690L;

  /**
   * Update or add solr field.
   * 
   * @param name
   *          The field name.
   * @param value
   *          The value.
   */
  public void addField(String name, Object value) {
    if (!getFieldNames().contains(name))
      super.addField(name, value);
    else
      super.setField(name, value);
  }

}
