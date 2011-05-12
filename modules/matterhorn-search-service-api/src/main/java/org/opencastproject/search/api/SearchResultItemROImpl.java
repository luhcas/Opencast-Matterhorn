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

package org.opencastproject.search.api;

import org.opencastproject.mediapackage.MediaPackage;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Date;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Another implementation with a read-only XML binding.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "result", namespace = "http://search.opencastproject.org")
@XmlRootElement(name = "result", namespace = "http://search.opencastproject.org")
public abstract class SearchResultItemROImpl implements SearchResultItem {

  @Override
  @XmlElement(name = "id")
  public abstract String getId();

  @Override
  @XmlElement(name = "mediapackage")
  public abstract MediaPackage getMediaPackage();

  @Override
  @XmlElement(name = "dcExtent")
  public abstract long getDcExtent();

  @Override
  @XmlElement(name = "dcTitle")
  public abstract String getDcTitle();

  @Override
  @XmlElement(name = "dcSubject")
  public abstract String getDcSubject();

  @Override
  @XmlElement(name = "dcDescription")
  public abstract String getDcDescription();

  @Override
  @XmlElement(name = "dcCreator")
  public abstract String getDcCreator();

  @Override
  @XmlElement(name = "dcPublisher")
  public abstract String getDcPublisher();

  @Override
  @XmlElement(name = "dcContributor")
  public abstract String getDcContributor();

  @Override
  @XmlElement(name = "dcAbstract")
  public abstract String getDcAbstract();

  @Override
  @XmlElement(name = "dcCreated")
  public abstract Date getDcCreated();

  @Override
  @XmlElement(name = "dcAvailableFrom")
  public abstract Date getDcAvailableFrom();

  @Override
  @XmlElement(name = "dcAvailableTo")
  public abstract Date getDcAvailableTo();

  @Override
  @XmlElement(name = "dcLanguage")
  public abstract String getDcLanguage();

  @Override
  @XmlElement(name = "dcRightsHolder")
  public abstract String getDcRightsHolder();

  @Override
  @XmlElement(name = "dcSpatial")
  public abstract String getDcSpatial();

  @Override
  @XmlElement(name = "dcTemporal")
  public abstract String getDcTemporal();

  @Override
  @XmlElement(name = "dcIsPartOf")
  public abstract String getDcIsPartOf();

  @Override
  @XmlElement(name = "dcReplaces")
  public abstract String getDcReplaces();

  @Override
  @XmlElement(name = "dcType")
  public abstract String getDcType();

  @Override
  @XmlElement(name = "dcAccessRights")
  public abstract String getDcAccessRights();

  @Override
  @XmlElement(name = "dcLicense")
  public abstract String getDcLicense();

  @Override
  @XmlElement(name = "dcType")
  public abstract SearchResultItemType getType();

  @Override
  @XmlElement(name = "dcKeywords")
  public abstract String[] getKeywords();

  @Override
  @XmlElement(name = "dcCover")
  public abstract String getCover();

  @Override
  @XmlElement(name = "dcModified")
  public abstract Date getModified();

  @Override
  @XmlElement(name = "dcScore")
  public abstract double getScore();

  public abstract MediaSegmentImpl[] getMediaSegments();

  /**
   * Media segment list *
   */
  @XmlElementWrapper(name = "segments")
  @XmlElement(name = "segment")
  public final synchronized SortedSet<MediaSegmentImpl> _getSegments() {
    if (mediaSegments == null) {
      mediaSegments = new TreeSet<MediaSegmentImpl>();
      for (MediaSegmentImpl s : getMediaSegments()) {
        mediaSegments.add(s);
      }
    }
    return mediaSegments;
  }

  private SortedSet<MediaSegmentImpl> mediaSegments = null;

  @Override
  public final MediaSegment[] getSegments() {
    return getMediaSegments();
  }
}
