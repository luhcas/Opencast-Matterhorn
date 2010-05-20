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

package org.opencastproject.series.api;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.opencastproject.media.mediapackage.EName;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogImpl;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data type for a Series that stores the metadata that belongs to the series
 *
 */
@Entity
@Table(name="Series")
public class Series {
  private static final Logger logger = LoggerFactory.getLogger(Series.class);
  
  @Id
  String seriesId;
  
  @Transient
  DublinCoreCatalog dublinCore;
  
  @OneToMany (fetch=FetchType.EAGER, cascade=CascadeType.ALL)
  @MapKey(name="metadata")
  List<SeriesMetadata> metadata;
  
  public String getSeriesId () {
    return seriesId;
  }
  
  public void setSeriesId (String seriesId) {
    this.seriesId = seriesId;
  }
  
  public String generateSeriesId() {
    return seriesId = UUID.randomUUID().toString();
  }
  
  public  List<SeriesMetadata> getMetadata () {
    return metadata;
  }
  
  public void setMetadata (List<SeriesMetadata> metadata) {
    this.metadata = metadata;
    dublinCore = null;
  }
  
  public DublinCore getDublinCore () {
      if (dublinCore == null) dublinCore = buildDublinCore();
      return dublinCore;
  }
  
  private DublinCoreCatalog buildDublinCore () {
    DublinCoreCatalog dc = DublinCoreCatalogImpl.newInstance();
    
    return dc;
  }
  
  public boolean valid () {
    for (SeriesMetadata m : getMetadata())  {
      if (! validMetadata(m)) {
        logger.warn("Metadata not valid for series: {}", m);
        return false;
      }
    }
    return true;
  }
  
  /**
   * Checks if the provided key is a valid Dublin Core XML tag
   * @param metadata the metadata that should be checked
   * @return true if the key is valid
   */
  private boolean validMetadata (SeriesMetadata m) {
    // check if key is valid for series at all
    String [] validKeys = new String [] {"license", "valid", "publisher", "creator", "subject", "temporal", "title", "audience", "spatial", "rightsHolder", "extent",
                           "created", "language", "identifier", "isReplacedBy", "type", "available", "modified", "replaces", "contributor", "description", "issued"};
    List<String> vk = Arrays.asList(validKeys);
    if (! vk.contains(m.getKey())) return false;
   

    //check value for fields that expect a date.
    String [] numberKeys = new String [] {"valid", "extent", "created", "available", "modified", "issued"};
    List<String> nk = Arrays.asList(numberKeys);   
    if (nk.contains(m.getKey())){
      try {
        Long.parseLong(m.getValue());
      } catch (NumberFormatException e) {
        return false;
      }
    }
    
    return true;
  }  
  
  private static boolean isDateKey (String key) {
    String [] dateKeys = new String [] {"valid", "extent", "created", "available", "modified", "issued"};
    List<String> dk = Arrays.asList(dateKeys);   
    if (dk.contains(key)) return true;
    return false;
  }
  
  public static String formatW3CDTF(Date date) {
    SimpleDateFormat  dateFormater = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
    dateFormater.setTimeZone(TimeZone.getTimeZone("GMT"));
    return dateFormater.format(date);
  }
  
  private DublinCoreCatalog generateDublinCore () {
    DublinCoreCatalog dc = DublinCoreCatalogImpl.newInstance();
    
    dc.add(DublinCoreCatalog.PROPERTY_IDENTIFIER, new DublinCoreValue(getSeriesId()));
    for (SeriesMetadata m : getMetadata()) {
      String v = null;
      if (isDateKey(m.getKey())) v = formatW3CDTF(new Date(Long.parseLong(m.getValue())));
      else v = m.getValue();
      DublinCoreValue value = new DublinCoreValue(v);
      EName property = new EName("http://purl.org/dc/terms/", m.getKey());
      dc.add(property, value);
    }
    
    return dc;
  }
}
