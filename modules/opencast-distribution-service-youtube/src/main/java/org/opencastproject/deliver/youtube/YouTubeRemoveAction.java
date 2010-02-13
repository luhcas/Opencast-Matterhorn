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

package org.opencastproject.deliver.youtube;

import org.opencastproject.deliver.actions.RemoveAction;
import org.opencastproject.deliver.schedule.Action;
import org.opencastproject.deliver.schedule.FailedException;
import org.opencastproject.deliver.schedule.InvalidException;
import org.opencastproject.deliver.schedule.RetryException;
import org.opencastproject.deliver.schedule.Task;

import com.google.gdata.client.youtube.YouTubeService;
import com.google.gdata.data.youtube.VideoEntry;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ServiceException;

import java.io.IOException;
import java.net.URL;

public class YouTubeRemoveAction extends RemoveAction {

    /** Configuration parameters for YouTube service. */
    private YouTubeConfiguration configuration;

    /** YouTube service connector. */
    private YouTubeService service;

    /** The name of the task that published the video clip. */
    private String publish_task;

    /**
     * Constructs a YouTubeRemoveAction.
     */

    public YouTubeRemoveAction() {
        super();
        this.configuration = YouTubeConfiguration.getInstance();
        service = new YouTubeService(configuration.getClientId(),
                configuration.getDeveloperKey());
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
        authenticate(configuration.getUserId(), configuration.getPassword());
        VideoEntry entry = getVideoEntry();

        // Delete the video entry
        try {
            entry.delete();
            status("Video removed");
            succeed("Video removed");
        }
        catch (IOException except) {
            throw new FailedException(except);
        }
        catch (ServiceException except) {
            throw new FailedException(except);
        }
    }

    /**
     * Authenticate the delivery user.
     *
     * @param user YouTube user id or gmail name
     * @param password user's password
     */

    private void authenticate(String user, String password) {
        log("Auth user=" + user);
        try {
            service.setUserCredentials(user, password);
        }
        catch (AuthenticationException except) {
            throw new RetryException(except);
        }
    }

     /**
     * Gets a new copy of the video entry.
     *
     * @return current entry
     */

    private VideoEntry getVideoEntry() {
        String entry_url = getEntryURL();
        try {
            return service.getEntry(new URL(entry_url), VideoEntry.class);
        }
        catch (IOException except) {
            throw new FailedException(except);
        }
        catch (ServiceException except) {
            throw new FailedException(except);
        }
    }

    /**
     * Obtains the YouTube entry URL from the publish task.
     *
     * @return YouTube entry URL
     */

    private String getEntryURL() {
        // Get the publish task
        Task task = getTaskNamed(publish_task);
        if (publish_task == null)
            throw new FailedException("Missing task: " + publish_task);

        // Get the action
        Action action = task.getAction();
        if (!(action instanceof YouTubeDeliveryAction))
            throw new FailedException("Invalid action type" +
                    action.getClass().getName());

        String url = ((YouTubeDeliveryAction)action).getEntryUrl();
        if (url == null)
            throw new FailedException("Unknown entry URL");

        return url;
    }
}
