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

package org.opencastproject.media.bundle;

/**
 * <code>Timeline</code> contains events on the timeline of a multimedia
 * production like beginning and end, scene or slide changes etc.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id$
 */
public interface Timeline extends Catalog {

  /** Bundle element type */
  BundleElement.Type TYPE = BundleElement.Type.Timeline;

  /** Timeline flavor */
  BundleElementFlavor FLAVOR = new BundleElementFlavor("metadata", "timeline");

  /** The timeline's filename */
  String FILENAME = "timeline.xml";

}