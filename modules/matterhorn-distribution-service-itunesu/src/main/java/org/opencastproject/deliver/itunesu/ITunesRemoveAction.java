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
package org.opencastproject.deliver.itunesu;

import org.opencastproject.deliver.actions.RemoveAction;

import org.opencastproject.deliver.schedule.FailedException;
import org.opencastproject.deliver.schedule.InvalidException;
import org.opencastproject.deliver.schedule.Task;
import org.opencastproject.deliver.schedule.Action;

/**
 * RemoveAction to remove iTunes media.
 */
public class ITunesRemoveAction extends RemoveAction {

  /** The name of the task that published the video clip. */
  private String publish_task;

  /**
   * Constructs a ITunesRemoveAction.
   */
  public ITunesRemoveAction() {
    super();
  }

  /**
   * Returns the name of the task that published the clip to be removed.
   *
   * @return task name
   */
  public String getPublishTask() {
    return publish_task;
  }

  /**
   * Sets the name of the task that published the clip to be removed.
   *
   * @param publish_task Task name
   */
  public void setPublishTask(String publish_task) {
    this.publish_task = publish_task;
  }

  /**
   * Checks the members of the action.
   */
  public void validate() {
    if (publish_task == null || publish_task.equals(""))
      throw new InvalidException("Missing publish task");
  }

  /**
   * Execute the action.
   */
  @Override
  protected void execute() {
    String destination = getTrackURL(); 
    // separate the URL
    int index = destination.lastIndexOf(".");
    // handle of the track to remove
    String handle = destination.substring(index + 1);
    // handle of the parent group
    destination = destination.substring(0, index);
    // Web service API instance
    ITunesWSAPI api = new ITunesWSAPI(destination);
    
    String response = api.deleteTrack(handle);
    
    status("Media removed");
    succeed("Media removed: " + handle + " - " + response);
  }

  /**
   * Obtains the track handle from the publish task.
   *
   * @return iTunes track handle
   */
  private String getTrackURL() {
    // Get the publish task
    Task task = getTaskNamed(publish_task);
    if (publish_task == null) {
      throw new FailedException("Missing task: " + publish_task);
    }

    // Get the action
    Action action = task.getAction();
    if (!(action instanceof ITunesDeliveryAction))
      throw new FailedException("Invalid action type" + action.getClass().getName());

    String url = ((ITunesDeliveryAction) action).getTrackURL();
    if (url == null) {
      throw new FailedException("Unknown track URL");
    }

    return url;
  }
}
