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
package org.opencastproject.composer.impl.endpoint;

import org.opencastproject.composer.impl.EncodingProfileImpl;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * A JAXB annotated collection wrapper for {@link EncodingProfileImpl}s.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name="encoding-profiles", namespace="http://composer.opencastproject.org/")
public class EncodingProfileList {
  public EncodingProfileList() {}

  public EncodingProfileList(List<EncodingProfileImpl> list) {
    this.profile = list;
  }
  
  @XmlElement(name="encoding-profile")
  protected List<EncodingProfileImpl> profile;

  public List<EncodingProfileImpl> getProfile() {
    return profile;
  }

  public void setProfile(List<EncodingProfileImpl> profile) {
    this.profile = profile;
  }

}
