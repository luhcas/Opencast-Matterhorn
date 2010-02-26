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
package org.opencastproject.deliver.itunesu;


import org.opencastproject.deliver.actions.DeliveryAction;


/**
 * DeliveryAction to upload iTunes media.
 */
public class ITunesDeliveryAction extends DeliveryAction {

  /** handle of uploaded track. */
  private String trackURL;

  /**
   * Constructs a ITunesDeliveryAction.
   */
  public ITunesDeliveryAction() {
    super();
  }

  /**
   * Returns the iTunes track URL or null if not yet set.
   * @return track URL
   */
  public String getTrackURL() {
    return trackURL;
  }

  /**
   * Sets the iTunes track URL.
   *
   * @param trackURL URL for uploaded track
   */
  public void setTrackURL(String trackURL) {
    this.trackURL = trackURL;
  }

  /**
   * Delivers a media file to iTunesU.
   */
  @Override
  protected void execute() {
    // update status
    status("Upload in progress");

    /** Web service API instance */
    ITunesWSAPI api = new ITunesWSAPI(getDestination());

    String handle = api.uploadFile(getMediaPath());

    status("Media uploaded");
    succeed("Media delivered: " + getMediaPath() + " - " + handle);
    
    // skip "xxx.edu."
    int index = api.getSiteURL().length() + 1;
    // set the handle as the URL
    setTrackURL(handle.substring(index + 1));
  }
}
