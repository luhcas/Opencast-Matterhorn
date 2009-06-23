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
package org.opencastproject.mediapackage.api;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="media-packages")
public class MediaPackageList {
  private List<MediaPackage> mediaPackages;

  public MediaPackageList() {}
  
  public MediaPackageList(List<MediaPackage> mediaPackages) {
    this.mediaPackages = mediaPackages;
  }

  @XmlElement(name="media-package")
  public List<MediaPackage> getMediaPackages() {
    return mediaPackages;
  }
  public void setMediaPackages(List<MediaPackage> mediaPackages) {
    this.mediaPackages = mediaPackages;
  }
}
