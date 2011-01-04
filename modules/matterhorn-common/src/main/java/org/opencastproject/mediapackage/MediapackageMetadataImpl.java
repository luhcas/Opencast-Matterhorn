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
package org.opencastproject.mediapackage;

import java.util.Date;

/**
 * Provides metadata for a {@link MediaPackageMetadata}
 */
public class MediapackageMetadataImpl implements MediaPackageMetadata {
  protected String title;
  protected String seriesTitle;
  protected String identifier;
  protected String seriesIdentifier;
  protected String[] creators;
  protected String[] contributors;
  protected String[] subjects;
  protected String language;
  protected String license;
  protected Date date;

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getSeriesTitle() {
    return seriesTitle;
  }

  public void setSeriesTitle(String seriesTitle) {
    this.seriesTitle = seriesTitle;
  }

  public String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  public String getSeriesIdentifier() {
    return seriesIdentifier;
  }

  public void setSeriesIdentifier(String seriesIdentifier) {
    this.seriesIdentifier = seriesIdentifier;
  }

  public String[] getCreators() {
    return creators;
  }

  public void setCreators(String[] creators) {
    this.creators = creators;
  }

  public String[] getContributors() {
    return contributors;
  }

  public void setContributors(String[] contributors) {
    this.contributors = contributors;
  }

  public String[] getSubjects() {
    return subjects;
  }

  public void setSubjects(String[] subjects) {
    this.subjects = subjects;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public String getLicense() {
    return license;
  }

  public void setLicense(String license) {
    this.license = license;
  }

  public Date getDate() {
    return date;
  }

  public void setDate(Date date) {
    this.date = date;
  }
}
