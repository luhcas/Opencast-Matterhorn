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
package org.opencastproject.series.endpoint;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.opencastproject.series.api.Series;
import org.opencastproject.series.api.SeriesMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JaxB implementation of the SchedulerEvent
 *
 */
@XmlType(name="Series", namespace="http://series.opencastproject.org")
@XmlRootElement(name="Series", namespace="http://series.opencastproject.org")
@XmlAccessorType(XmlAccessType.FIELD)
public class SeriesJaxbImpl {
  private static final Logger logger = LoggerFactory.getLogger(SeriesJaxbImpl.class);
  
  @XmlID
  String seriesId;
  @XmlElementWrapper(name="metadata")
  @XmlElement(name="metdata")
  LinkedList <SeriesMetadataJaxbImpl> metadata;  
  
  /**
   * Default constructor without any import.
   */
  public SeriesJaxbImpl() {
    metadata = new LinkedList<SeriesMetadataJaxbImpl>();
  }
  
  /**
   * Constructs a JaxB representations of a SchedulerEvent
   * @param event
   */
  public SeriesJaxbImpl(Series series) {
    logger.info("Creating a {} from {}", SeriesJaxbImpl.class.getName(), series);
    seriesId = series.getSeriesId();
    metadata = new LinkedList<SeriesMetadataJaxbImpl>();
    for (SeriesMetadata m : series.getMetadata()) {
      metadata.add(new SeriesMetadataJaxbImpl(m));
    }
  }
  
  
  /**
   * Converts the JaxB representation of the Series into a regular series
   * @return the Series represented by this object
   */
  @XmlTransient
  public Series getSeries() {
    Series s = new Series();
    if(seriesId != ""){
      s.setSeriesId(seriesId);
    }else{
      s.generateSeriesId();
    }
    
    List<SeriesMetadata> list = new LinkedList<SeriesMetadata>(); 
    for (SeriesMetadataJaxbImpl m : metadata) {
      list.add(m.getSeriesMetadata());
    }
    s.setMetadata(list);
    return s;
  }
  
  /**
   * valueOf function is called by JAXB to bind values. This function calls the series factory.
   *
   *  @param    xmlString string representation of an series.
   *  @return   instantiated event SeriesJaxbImpl.
   */
  public static SeriesJaxbImpl valueOf(String xmlString) throws Exception {
    return (SeriesJaxbImpl) SeriesBuilder.getInstance().parseSeriesJaxbImpl(xmlString);
  }
}
