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
package org.opencastproject.status.impl;

import org.opencastproject.status.api.StatusMessage;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * TODO Does this really belong in the media osgi bundle?
 * 
 * @see StatusMessage
 */
@XmlRootElement(name="status-message")
public class StatusMessageImpl implements StatusMessage {
  private String message;
  private String reference;
  private String source;

  public StatusMessageImpl() {
  }
  
  public StatusMessageImpl(String message, String reference, String source) {
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
  public static class Adapter extends XmlAdapter<StatusMessageImpl,StatusMessage> {
    public StatusMessage unmarshal(StatusMessageImpl v) { return v; }
    public StatusMessageImpl marshal(StatusMessage v) { return (StatusMessageImpl)v; }
  }

}
