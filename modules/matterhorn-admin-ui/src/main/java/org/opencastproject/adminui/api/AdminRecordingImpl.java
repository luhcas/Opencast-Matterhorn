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
package org.opencastproject.adminui.api;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * A JAXB-anotated implementation of {@link AdminRecording}
 */
@XmlRootElement(name="recording")
@XmlAccessorType(XmlAccessType.FIELD)
public class AdminRecordingImpl implements AdminRecording {

  @XmlElement(name="id")
  private String id;
  
  @XmlElement(name="title")
  private String title;

  @XmlElement(name="presenter")
  private String presenter;

  @XmlElement(name="seriesid")
  private String seriesId;

  @XmlElement(name="seriestitle")
  private String seriesTitle;
  
  @XmlElement(name="recurs")
  private Boolean recurs;

  @XmlElement(name="startTime")
  private String startTime;

  @XmlElement(name="endTime")
  private String endTime;

  @XmlElement(name="captureAgent")
  private String agent;
  
  @XmlElement(name="recordingStatus")
  private String recordingStatus;

  @XmlElement(name="processingStatus")
  private String processing_status;

  @XmlElement(name="distributionStatus")
  private String distribution_status;

  @XmlElement(name="holdOperationTitle")
  private String holdOperationTitle;

  @XmlElement(name="holdActionTitle")
  private String holdActionTitle;

  @XmlElement(name="holdActionPanelURL")
  private String holdActionPanelURL;

  @XmlElement(name="itemType")
  private ItemType type = ItemType.UNKNOWN;

  @XmlElement(name="error")
  @XmlElementWrapper(name="errors")
  protected String[] errorMessages;

  @XmlElement(name="failedOperation")
  protected String failedOperation;

  @XmlElement(name="zip")
  private String zipUrl;
  
  public AdminRecordingImpl() {}

  @Override
  public void setHoldOperationTitle(String title) {
    this.holdOperationTitle = title;
  }

  @Override
  public String getHoldOperationTitle() {
    return this.holdOperationTitle;
  }

  @Override
  public void setHoldActionTitle(String title) {
    this.holdActionTitle = title;
  }

  @Override
  public String getHoldActionTitle() {
    return this.holdActionTitle;
  }

  @Override
  public void setHoldActionPanelURL(String url) {
    this.holdActionPanelURL= url;
  }

  @Override
  public String getHoldActionPanelURL() {
    return this.holdActionPanelURL;
  }

  @Override
  public void setItemType(ItemType type) {
    this.type = type;
  }

  @Override
  public ItemType getItemType() {
    return type;
  }

  static class Adapter extends XmlAdapter<AdminRecordingImpl, AdminRecording> {
    public AdminRecordingImpl marshal(AdminRecording op) throws Exception {return (AdminRecordingImpl)op;}
    public AdminRecording unmarshal(AdminRecordingImpl op) throws Exception {return op;}
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

  public String getSeriesId() {
    return seriesId;
  }

  public void setSeriesId(String seriesId) {
    this.seriesId = seriesId;
  }

  public String getSeriesTitle() {
    return seriesTitle;
  }

  public void setSeriesTitle(String seriesTitle) {
    this.seriesTitle = seriesTitle;
  }
  
  /*
  public Boolean isRecurringEvent() {
    return recurs;
  }
  
  public void isRecurringEvent(Boolean recurs) {
    this.recurs = recurs;
  }
  */
  
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
  
  public String getRecordingStatus() {
    return recordingStatus;
  }

  public void setRecordingStatus(String status) {
    this.recordingStatus = status;
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

  public String getZipUrl() {
    return zipUrl;
  }

  public void setZipUrl(String zipUrl) {
    this.zipUrl = zipUrl;
  }

  public String[] getErrorMessages() {
    return errorMessages;
  }

  public void setErrorMessages(String[] errorMessages) {
    this.errorMessages = errorMessages;
  }

  public String getFailedOperation() {
    return failedOperation;
  }

  public void setFailedOperation(String failedOperation) {
    this.failedOperation = failedOperation;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("ID: " + this.getId() + "\n");
    sb.append("Presenter: " + this.getPresenter() + "\n");
    sb.append("Series ID: " + this.getSeriesId() + "\n");
    sb.append("Series Title: " + this.getSeriesTitle() + "\n");
    sb.append("Agent: " + this.getCaptureAgent() + "\n");
    sb.append("Time: " + this.getStartTime() + "\n");
    sb.append("Proc state: " + this.getProcessingStatus() + "\n");
    sb.append("Dist state: " + this.getDistributionStatus() + "\n");
    sb.append("Zip: " + this.getZipUrl() + "\n");
    return sb.toString();
  }
}
