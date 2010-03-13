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

package org.opencastproject.metadata.mpeg7;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.Formatter;

/**
 * TODO: Comment me!
 */
public class MediaRelTimePointImpl extends MediaTimePointImpl implements MediaTimePoint {

  /** Number of milliseconds per second */
  private static final long MS_PER_SECOND = 1000L;

  /** Number of milliseconds per minute */
  private static final long MS_PER_MINUTE = 60000L;

  /** Number of milliseconds per hour */
  private static final long MS_PER_HOUR = 3600000L;

  /** Number of milliseconds per day */
  private static final long MS_PER_DAY = 86400000L;

  /**
   * Creates a relative time point at <code>T00:00:00:0F1000</code>.
   */
  public MediaRelTimePointImpl() {
    super();
  }

  /**
   * @param milliseconds
   */
  public MediaRelTimePointImpl(long milliseconds) {
    fractions = (int) (milliseconds % MS_PER_SECOND);
    second = (int) ((milliseconds / MS_PER_SECOND) % 60);
    minute = (int) ((milliseconds / MS_PER_MINUTE) % 60);
    hour = (int) ((milliseconds / MS_PER_HOUR) % 24);
    day = (int) (milliseconds / MS_PER_DAY);
    hour += day * 24;
    fractionsPerSecond = 1000;
  }

  /**
   * @param hour
   *          the number of hours
   * @param minute
   *          the number of minutes
   * @param second
   *          the number of seconds
   * @param fraction
   *          the number of fractions
   * @param fractionsPerSecond
   *          the number of fractions per second
   */
  public MediaRelTimePointImpl(int hour, int minute, int second, int fraction, int fractionsPerSecond) {
    this.fractionsPerSecond = fractionsPerSecond;
    this.fractions = fraction;
    this.second = second;
    this.minute = minute;
    this.hour = hour;
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    Formatter f = new Formatter();
    String result = null;
    result = f.format("T%02d:%02d:%02d:%dF%d", hour, minute, second, fractions, fractionsPerSecond).toString();
    f.close();
    return result;
  }

  /**
   * @see org.opencastproject.media.mediapackage.XmlElement#toXml(org.w3c.dom.Document)
   */
  public Node toXml(Document document) {
    Element node = null;
    node = document.createElement("MediaRelTimePoint");
    node.setTextContent(toString());
    return node;
  }

}
