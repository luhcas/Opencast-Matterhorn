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
package org.opencastproject.distribution.youtube;

import org.opencastproject.deliver.schedule.Schedule;
import org.opencastproject.deliver.schedule.Task;
import org.opencastproject.deliver.youtube.YouTubeConfiguration;
import org.opencastproject.deliver.youtube.YouTubeDeliveryAction;
import org.opencastproject.distribution.api.DistributionService;
import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageElement;
import org.opencastproject.media.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.workspace.api.Workspace;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Distributes media to the youtube media delivery directory.
 */
public class YoutubeDistributionService implements DistributionService {

  private static final Logger logger = LoggerFactory.getLogger(YoutubeDistributionService.class);
  protected Workspace workspace = null;

  private static YouTubeConfiguration config = null;

  public void activate(ComponentContext cc) {
    config = YouTubeConfiguration.getInstance();
    // client ID may not be necessary
    config.setClientId("abcde");
    config.setDeveloperKey("AI39si7bx2AbnOM6RM8J7mdrljfZCzisYzDkqvIqEjV3zjbqQIr6-u_bg3R0MLAVVXLqKjSsxu4ReytWFn7ylIlDk6OC7pdXpQ");
    config.setUploadUrl("http://uploads.gdata.youtube.com/feeds/api/users/default/uploads");
    config.setUserId("utubedelivery");
    config.setPassword("utubedelivery");
    config.setCategory("Education");
  }

  /**
   * Uploads media files to Youtube and inserts into a playlist. {@inheritDoc}
   * 
   * @see org.opencastproject.distribution.api.DistributionService#distribute(org.opencastproject.media.mediapackage.MediaPackage)
   */
  public MediaPackage distribute(MediaPackage mediaPackage, String... elementIds) {
    Schedule schedule = new Schedule();

    try {
      // CHANGE ME: no iteration is needed since catalog and attachments won't
      // be delivered to Youtube.
      for (String id : elementIds) {
        MediaPackageElement element = mediaPackage.getElementById(id);
        File sourceFile = workspace.get(element.getURI());
        if( ! sourceFile.exists() || ! sourceFile.isFile()) {
          throw new IllegalStateException("Could not retrieve a file for element " + element.getIdentifier());
        }
        
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        String s = dateFormat.format(date);
        YouTubeDeliveryAction act = new YouTubeDeliveryAction();
        // CHANGE ME: use timestamp for now
        String name = "TV" + s;
        // CHNAGE ME: set metadata elements here
        act.setName(name);
        act.setTitle(sourceFile.getName());
        act.setTags(new String [] {"whatever"}); // METADATA.keywords as String[]);
        act.setAbstract("A HD Test Video Clip");
        act.setMediaPath(sourceFile.getAbsolutePath());
        // deliver to a play list
//        act.setDestination("B8B47104C2C1663B"); // FIXME: replace this with a playlist based on the episode's series

        logger.info("Delivering from {}", sourceFile.getAbsolutePath());

        // start the scheduler
        schedule.start(act);
        
        while (true) {
          Task task = schedule.getTask(name);
          synchronized (task) {
            Task.State state = task.getState();
            if (state == Task.State.INITIAL || state == Task.State.ACTIVE) {
              try {
                Thread.sleep(1000L);
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
              // still running
              continue;
            }
            else if (state == Task.State.COMPLETE) {
              // logger.info("Succeeded delivering from {}", 
              // sourceFile.getAbsolutePath());
              String videoURL = act.getEntryUrl();
              videoURL = videoURL.replace("?client=" + config.getClientId(), "");
              videoURL = videoURL.replace("http://gdata.youtube.com/feeds/api/users/utubedelivery/uploads/", "http://www.youtube.com/watch?v=");
              URI newTrackUri = new URI(videoURL);
              MediaPackageElement newElement = MediaPackageElementBuilderFactory.newInstance().newElementBuilder().elementFromURI(
                      newTrackUri, element.getElementType(), element.getFlavor());
              newElement.setIdentifier(element.getIdentifier() + "-dist");
              mediaPackage.addDerived(newElement, element);
              break;
            }
            else if (state == Task.State.FAILED) {
              // logger.info("Failed delivering from {}", 
              // sourceFile.getAbsolutePath());
              break;
            }
          }
        } // end of schedule loop
      } // end of media package element loop
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    } finally {
      // shutdown the scheduler
      schedule.shutdown();
    }

    return mediaPackage;
  }

  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }
}
