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
package org.opencastproject.notification.api;

import org.opencastproject.notification.api.NotificationMessage;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * @see NotificationMessage
 */
@XmlRootElement(name="status-message")
public class NotificationMessageImpl implements NotificationMessage {
  private String message;
  private String reference;
  private String source;

  public NotificationMessageImpl() {
  }
  
  public NotificationMessageImpl(String message, String reference, String source) {
    this.message = message;
    this.reference = reference;
    this.source = source;
  }

  @XmlElement(required=true, nillable=false)
  public String getMessage() {
    return message;
  }
  public void setMessage(String message) {
    this.message = message;
  }
  @XmlElement(required=true, nillable=false)
  public String getReference() {
    return reference;
  }
  public void setReference(String reference) {
    this.reference = reference;
  }
  @XmlElement(required=true, nillable=false)
  public String getSource() {
    return source;
  }
  public void setSource(String source) {
    this.source = source;
  }

  /**
   * Allows the StatusMessage interface to be serialized via jaxb
   */
  public static class Adapter extends XmlAdapter<NotificationMessageImpl,NotificationMessage> {
    public NotificationMessage unmarshal(NotificationMessageImpl v) { return v; }
    public NotificationMessageImpl marshal(NotificationMessage v) { return (NotificationMessageImpl)v; }
  }

}
