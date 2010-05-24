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
package org.opencastproject.caption.impl;

import org.opencastproject.caption.api.Caption;
import org.opencastproject.caption.api.Time;

/**
 * Implementation of {@link Caption}.
 * 
 */
public class CaptionImpl implements Caption {

  private Time startTime;
  private Time stopTime;
  private String caption;

  // private HashMap<String, String> textStyles;

  public CaptionImpl(Time startTime, Time stopTime, String caption) {
    this.startTime = startTime;
    this.stopTime = stopTime;
    this.caption = caption;
    // this.textStyles = new HashMap<String, String>();
  }

  // public CaptionImpl(Time starTime, Time stopTime, String caption, HashMap<String, String> textStyles) {
  // this.startTime = starTime;
  // this.stopTime = stopTime;
  // this.caption = caption;
  // FIXME create new hashmap and copy keys?
  // this.textStyles = textStyles;
  // }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.caption.api.Caption#getContent()
   */
  @Override
  public String getCaption() {
    return caption;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.caption.api.Caption#getStart()
   */
  @Override
  public Time getStartTime() {
    return startTime;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.caption.api.Caption#getStop()
   */
  @Override
  public Time getStopTime() {
    return stopTime;
  }

  // do we export those

  // private void setStartTime(Time startTime) {
  // this.startTime = startTime;
  // }

  // private void setStopTime(Time stopTime) {
  // this.stopTime = stopTime;
  // }

  // private void setCaption(String caption) {
  // this.caption = caption;
  // }

  // TODO styles? (modify interface)

  // public HashMap<String, String> getTextStyles() {
  // do we make a copy?
  // return textStyles;
  // }

  // public String getTextStyleValue(String attribute) {
  // return null or empty string?
  // return this.textStyles.get(attribute);
  // }

  // public void setTextStyles(HashMap<String, String> textStyles) {
  // add clean new, make a copy?
  // this.textStyles = textStyles;
  // }
}
