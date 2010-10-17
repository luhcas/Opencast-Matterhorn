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
package org.opencastproject.serviceregistry.impl.endpoint;

import org.opencastproject.serviceregistry.api.ServiceStatistics;
import org.opencastproject.serviceregistry.impl.ServiceStatisticsImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * A wrapper for service statistics.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name="statistics", namespace="http://serviceregistry.opencastproject.org")
@XmlRootElement(name="statistics", namespace="http://serviceregistry.opencastproject.org")
public class ServiceStatisticsList {
  /** A list of search items. */
  @XmlElement(name="services")
  protected List<ServiceStatisticsImpl> stats = new ArrayList<ServiceStatisticsImpl>();

  public ServiceStatisticsList() {}
  
  public ServiceStatisticsList(Collection<ServiceStatistics> stats) {
    for(ServiceStatistics stat : stats) this.stats.add((ServiceStatisticsImpl)stat);
  }
  
  /**
   * @return the stats
   */
  public List<ServiceStatisticsImpl> getStats() {
    return stats;
  }
  
  /**
   * @param stats the stats to set
   */
  public void setStats(List<ServiceStatisticsImpl> stats) {
    this.stats = stats;
  }
}
