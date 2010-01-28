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
package org.opencastproject.adminui.api;

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * A JAXB-anotated implementation of {@link RecordingDataView}
 */
@XmlType(name="recording", namespace="http://adminui.opencastproject.org/")
@XmlRootElement(name="recording", namespace="http://adminui.opencastproject.org/")
@XmlAccessorType(XmlAccessType.FIELD)
public class RecordingDataViewImpl implements RecordingDataView {

  @XmlElement(name="id")
  private String id;
  
  @XmlElement(name="title")
  private String title;

  @XmlElement(name="presenter")
  private String presenter;

  @XmlElement(name="series")
  private String series;

  @XmlElement(name="startTime")
  private String startTime;

  @XmlElement(name="endTime")
  private String endTime;

  @XmlElement(name="captureAgent")
  private String agent;

  @XmlElement(name="processingStatus")
  private String processing_status;

  @XmlElement(name="distributionStatus")
  private String distribution_status;

  public RecordingDataViewImpl() {}

  static class Adapter extends XmlAdapter<RecordingDataViewImpl, RecordingDataView> {
    public RecordingDataViewImpl marshal(RecordingDataView op) throws Exception {return (RecordingDataViewImpl)op;}
    public RecordingDataView unmarshal(RecordingDataViewImpl op) throws Exception {return op;}
  }
  
  public String getId() {
    return id;
  }
  
  public void setId(String id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getPresenter() {
    return presenter;
  }

  public void setPresenter(String presenter) {
    this.presenter = presenter;
  }

  public String getSeries() {
    return series;
  }

  public void setSeries(String series) {
    this.series = series;
  }

  public String getStartTime() {
    return startTime;
  }

  public void setStartTime(String date) {
    this.startTime = date;
  }

  public String getEndTime() {
    return endTime;
  }

  public void setEndTime(String date) {
    endTime = date;
  }

  public String getCaptureAgent() {
    return agent;
  }

  public void setCaptureAgent(String agent) {
    this.agent = agent;
  }

  public String getProcessingStatus() {
    return processing_status;
  }

  public void setProcessingStatus(String status) {
    this.processing_status = status;
  }

  public String getDistributionStatus() {
    return distribution_status;
  }

  public void setDistributionStatus(String status) {
    this.distribution_status = status;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("ID: " + this.getId() + "\n");
    sb.append("Presenter: " + this.getPresenter() + "\n");
    sb.append("Series: " + this.getSeries() + "\n");
    sb.append("Agent: " + this.getCaptureAgent() + "\n");
    sb.append("Time: " + this.getStartTime() + "\n");
    sb.append("Proc state: " + this.getProcessingStatus() + "\n");
    sb.append("Dist state: " + this.getDistributionStatus() + "\n");

    return sb.toString();
  }
}
