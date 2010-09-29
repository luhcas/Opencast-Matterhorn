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

import org.opencastproject.series.api.Series;

import java.util.List;
import java.util.LinkedList;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * A List of {@link SeriesJaxbImpl}s
 */
@XmlRootElement(name="seriesList")
public class SeriesListImpl {

  @XmlElement
  protected List<SeriesImpl> series;
  
  public SeriesListImpl() {
    series = new LinkedList<SeriesImpl>();
  }
  
  public SeriesListImpl(List<Series> s) {
    this.setSeriesList(s);
  }
  
  public void setSeriesList(List<Series> series) {
    if(!this.series.isEmpty()){
      this.series.clear();
    }
    for(Series s : series){
      this.series.add((SeriesImpl) s);
    }
  }
}
