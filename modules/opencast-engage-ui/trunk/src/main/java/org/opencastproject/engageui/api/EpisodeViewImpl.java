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
package org.opencastproject.engageui.api;

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * A JAXB-anotated implementation of {@link EpisodeView}
 */
@XmlType(name = "episode", namespace = "http://searchui.opencastproject.org/")
@XmlRootElement(name = "episode", namespace = "http://searchui.opencastproject.org/")
@XmlAccessorType(XmlAccessType.FIELD)
public class EpisodeViewImpl implements EpisodeView {

  @XmlElement(name = "dcTitle")
  private String dcTitle;

  @XmlElement(name = "dcCreator")
  private String dcCreator;

  @XmlElement(name = "mediaPackageId")
  private String mediaPackageId;

  @XmlElement(name = "cover")
  private String cover;

  @XmlElement(name = "dcAbstract")
  private String dcAbstract;

  @XmlElement(name = "dcContributor")
  private String dcContributor;

  @XmlElement(name = "dcRightsHolder")
  private String dcRightsHolder;

  @XmlElement(name = "dcCreated")
  private String dcCreated;

  @XmlElement(name = "videoUrl")
  private String videoUrl;

  public EpisodeViewImpl() {
  }

  static class Adapter extends XmlAdapter<EpisodeViewImpl, EpisodeView> {
    public EpisodeViewImpl marshal(EpisodeView op) throws Exception {
      return (EpisodeViewImpl) op;
    }

    public EpisodeView unmarshal(EpisodeViewImpl op) throws Exception {
      return op;
    }
  }

  @Override
  public String getURLEncodedMediaPackageId() {
    return this.mediaPackageId;
  }

  @Override
  public void setURLEncodedMediaPackageId(String mediaPackageId) {
    this.mediaPackageId = mediaPackageId;

  }

  @Override
  public String getDcTitle() {
    return this.dcTitle;
  }

  @Override
  public void setDcTitle(String dcTitle) {
    this.dcTitle = dcTitle;
  }

  @Override
  public String getCover() {
    return this.cover;
  }

  @Override
  public void setCover(String cover) {
    this.cover = cover;
  }

  @Override
  public String getDcAbstract() {
    return this.dcAbstract;
  }

  @Override
  public void setDcAbstract(String dcAbstract) {
    this.dcAbstract = dcAbstract;
  }

  @Override
  public String getDcContributor() {
    return this.dcContributor;
  }

  @Override
  public void setDcContributor(String dcContributor) {
    this.dcContributor = dcContributor;
  }

  @Override
  public String getDcRightsHolder() {
    return this.dcRightsHolder;
  }

  @Override
  public void setDcRightsHolder(String dcRightsHolder) {
    this.dcRightsHolder = dcRightsHolder;
  }

  @Override
  public String getDcCreated() {
    return this.dcCreated;
  }

  @Override
  public void setDcCreated(String dcCreated) {
    this.dcCreated = dcCreated;
  }

  @Override
  public String getDcCreator() {
    return this.dcCreator;
  }

  @Override
  public void setDcCreator(String dcCreator) {
    this.dcCreator = dcCreator;
  }

  @Override
  public String getVideoUrl() {
    return this.videoUrl;
  }

  @Override
  public void setVideoUrl(String videoUrl) {
    this.videoUrl = videoUrl;
  }
}
