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

package org.opencastproject.series.impl;

import org.opencastproject.mediapackage.EName;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogImpl;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.series.api.Series;
import org.opencastproject.series.api.SeriesMetadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * Data type for a Series that stores the metadata that belongs to the series
 *
 */
 
@Entity
@Table(name="SERIES")
public class SeriesImpl implements Series {
  private static final Logger logger = LoggerFactory.getLogger(SeriesImpl.class);
  
  @Id
  @GeneratedValue
  @Column(name="ID", length=128)
  String seriesId;
  
  @Transient
  DublinCoreCatalog dublinCore;
  
  @OneToMany (fetch=FetchType.EAGER, cascade=CascadeType.ALL, mappedBy="series")
  List<SeriesMetadataImpl> metadata;
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.series.api.Series#getSeriesId()
   */
  public String getSeriesId () {
    return seriesId;
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.series.api.Series#setSeriesId(java.lang.String)
   */
  public void setSeriesId (String seriesId) {
    this.seriesId = seriesId;
    addToMetadata("identifier", seriesId);
    
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.series.api.Series#addToMetadata(java.lang.String, java.lang.String)
   */
  public void addToMetadata (String key, String value) {
    boolean updated = false;
    if (getMetadata() == null) {
      LinkedList<SeriesMetadataImpl> metadata = new LinkedList<SeriesMetadataImpl>();
      metadata.add(new SeriesMetadataImpl(this, key, value));
      return;
    }
    for (SeriesMetadata m : getMetadata()) {
      if (m.getKey().equals(key)) {
        m.setValue(value);
        updated = true;
        break;
      }
    }
    if (! updated) {
      metadata.add(new SeriesMetadataImpl(this, key, value));
    }
  }
  
  public String getFromMetadata (String key) {
    for (SeriesMetadata m: getMetadata()) 
      if (m.getKey().equals(key)) return m.getValue();
    return null;
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.series.api.Series#getMetadata()
   */
  public  List<SeriesMetadata> getMetadata () {
    if (metadata == null) return null;
    LinkedList<SeriesMetadata> list = new LinkedList<SeriesMetadata>();
    for (SeriesMetadata m : metadata) 
      list.add(m);
    return list;
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.series.api.Series#setMetadata(java.util.List)
   */
  public void setMetadata (List<SeriesMetadata> metadata) {
    LinkedList<SeriesMetadataImpl> list = new LinkedList<SeriesMetadataImpl>();
    for (SeriesMetadata m : metadata) {
      m.setSeries(this);
      list.add((SeriesMetadataImpl)m);
    }
    this.metadata = list;
    dublinCore = null;
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.series.api.Series#getDublinCore()
   */
  public DublinCoreCatalog getDublinCore () {
      if (dublinCore == null) dublinCore = buildDublinCore();
      return dublinCore;
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.series.api.Series#valid()
   */
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
  
  private DublinCoreCatalog buildDublinCore () {
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
  
  public static SeriesImpl buildSeries(DublinCoreCatalog dc) {
    SeriesImpl s = new SeriesImpl();
    s.setSeriesId(dc.getFirst(DublinCoreCatalog.PROPERTY_IDENTIFIER));
    s.updateMetadata(dc);
    return s;
  }
  
  public void updateMetadata(DublinCoreCatalog dc) {
    for(EName prop : dc.getProperties()) {
      addToMetadata(prop.getLocalName(), dc.getFirst(prop));
    }
  }

  @Override
  public String getDescription() {
    String result = getFromMetadata("title");
    String creator = getFromMetadata("creator");
    String temporal = getFromMetadata("temporal");
    if(creator != null && !creator.isEmpty()) {
      result += " - " + creator;
    }
    if(temporal != null && !temporal.isEmpty()){
      result += " (" + temporal + ")";
    }
    return result;
  }

  @Override
  public int compareTo(Series o) {
    return getDescription().compareTo(o.getDescription());
  }
}
