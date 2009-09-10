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
package org.opencastproject.composer.impl;

import org.opencastproject.composer.api.ComposerService;

import org.opencastproject.notification.api.NotificationMessage;

import java.util.UUID;

public class ComposerServiceImpl implements ComposerService {
  
  public NotificationMessage encode(String mediaPackageId, String trackId, String pathOut,
          String notificationServiceEndpoint) {

    // TODO actually start or queue (and change the message parameter) the encoding job
    StringBuilder message = new StringBuilder();
    message.append("encoding job started on:\nMedia package=");
    message.append(mediaPackageId);
    message.append("\nTrack ID=");
    message.append(trackId);
    message.append("\nPath Out=");
    message.append("\nNotification Endpoint=");
    message.append(notificationServiceEndpoint);

    return new NotificationMessage(message.toString(), UUID.randomUUID().toString(), ComposerService.class.getName());
  }

  public String getDocumentation() {
    return "The encoder service documentation needs to be written.";
  }

}