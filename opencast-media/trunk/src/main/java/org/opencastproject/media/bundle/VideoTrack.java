/**
 *  Copyright 2009 Opencast Project (http://www.opencastproject.org)
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
 * Interface description for video tracks.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id$
 */
public interface VideoTrack extends Track {

	/** Element flavor definition */
	BundleElementFlavor FLAVOR = new BundleElementFlavor("track", "video");

	/** Video track flavor description */
	String FLAVOR_DESCRIPTION = "Video track";

	/**
	 * Returns the track's video settings.
	 * 
	 * @return the video settings
	 */
	VideoSettings getVideoSettings();

}