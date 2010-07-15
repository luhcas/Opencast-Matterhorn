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

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * A class that represents the items that are shown in the Recording
 * screens in the Admin App.
 *
 * FIXME Overkill: A simple Map<String,String> would have been sufficient and
 * more flexible.
 */
@XmlJavaTypeAdapter(AdminRecordingImpl.Adapter.class)
public interface AdminRecording {
  /**
   * returns the id of the recording
   * @return String id of recording
   */
  String getId();
  
  /**
   * sets the id
   * @param id
   */
  void setId(String id);
  
  /**
   * returns the title of the recording
   * @return String Title of recording
   */
  String getTitle();

  /**
   * sets the title
   * @param title
   */
  void setTitle(String title);

  /**
   * returns the presenter(s) shown in the recording
   * @return String Presenter(s)
   */
  String getPresenter();

  /**
   * sets the presenter(s)
   * @param presenter
   */
  void setPresenter(String presenter);

  /**
   * returns the series title this recording belongs to
   * @return String Series this recording belongs to
   */
  String getSeriesTitle();

  /**
   * sets the series title for this recording
   * @param seriesTitle
   */
  void setSeriesTitle(String seriesTitle);

  /**
   * returns the series id this recording belongs to
   * @return String Series this recording belongs to
   */
  String getSeriesId();

  /**
   * sets the series id for this recording
   * @param seriesId
   */
  void setSeriesId(String seriesId);
  
  /**
   * returns whether an event is a part of a recurrence or not
   * @return Boolean
   */
  //Boolean isRecurringEvent();
  
  /**
   * sets whether an event is part of a recurrence or not
   * @param Boolean
   */
  //void isRecurringEvent(Boolean recurs);

  /**
   * returns date and time the recording was captured as *nix timestamp
   * FIXME type should be  long  for timestamp, this is String for now since
   * different services deliver different date representation.
   * @return Integer date of recording as timestamp
   */
  String getStartTime();

  /**
   * sets the time and date of the recording (*nix timestamp)
   * @param date
   */
  void setStartTime(String date);

  /**
   * returns date and time the recording was captured as *nix timestamp
   * @return Integer date of recording as timestamp
   */
  String getEndTime();

  /**
   * sets the time and date of the recording (*nix timestamp)
   * @param date
   */
  void setEndTime(String date);


  /**
   * returns the name of the capture agent that did this recording
   * @return String capture agent that did the recording
   */
  String getCaptureAgent();

  /**
   * sets the capture agent
   * @param agent
   */
  void setCaptureAgent(String agent);
  
  /**
   * returns the recording status
   * @return String recording status
   */
  String getRecordingStatus();
  
  /**
   * sets the Recording's status 
   * @see org.opencastproject.capture.admin.api.RecordingState
   * @param agent
   */
  void setRecordingStatus(String agent);
  
  /**
   * returns the status of the recording processing (workflow)
   * @return String status of recordings processing
   */
  String getProcessingStatus();

  /**
   * sets the processing state
   * @param status
   */
  void setProcessingStatus(String status);

  /**
   * returns the distribution status of the recording processing (workflow)
   * @return String status of recordings distribution
   */
  String getDistributionStatus();

  /**
   * sets the distribution state
   * @param status
   */
  void setDistributionStatus(String status);

  void setHoldOperationTitle(String title);

  String getHoldOperationTitle();

  void setHoldActionTitle(String title);

  String getHoldActionTitle();

  void setHoldActionPanelURL(String url);

  String getHoldActionPanelURL();

  /** The URL to the zipped mediapackage for this recording, if it exists */
  String getZipUrl();

  /** Sets the URL tot he zipped mediapackage */
  void setZipUrl(String url);
  
  // indicates from which service this item originates
  public static enum ItemType {UNKNOWN, SCHEDULER_EVENT, CAPTUREAGENT_JOB, WORKFLOW};

  void setItemType(ItemType type);
  
  ItemType getItemType();
  
  String[] getErrorMessages();
  
  void setErrorMessages(String[] errorMessages);

  String getFailedOperation();
  
  void setFailedOperation(String failedOperation);
}
