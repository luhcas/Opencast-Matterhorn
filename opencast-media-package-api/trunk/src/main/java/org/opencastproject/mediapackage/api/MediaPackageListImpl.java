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

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;

@XmlRootElement(name="media-packages", namespace="http://mediapackage.opencastproject.org")
public class MediaPackageListImpl implements MediaPackageList {
  protected List<MediaPackage> list;
  public MediaPackageListImpl() {}
  public MediaPackageListImpl(List<MediaPackage> list) {
    this.list = list;
  }
  @XmlAnyElement
  public List<MediaPackage> getMediaPackages() {
    return list;
  }
  public static class Adapter extends XmlAdapter<MediaPackageListImpl,MediaPackageList> {
    public MediaPackageList unmarshal(MediaPackageListImpl v) { return v; }
    public MediaPackageListImpl marshal(MediaPackageList v) { return (MediaPackageListImpl)v; }
  }
}
