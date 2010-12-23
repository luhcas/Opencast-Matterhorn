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

package org.opencastproject.metadata.dublincore;

import java.util.Date;

/**
 * A time interval, representing a DCMI period. They may be open.
 * <p/>
 * For further information on DCMI periods please refer to <a
 * href="http://dublincore.org/documents/dcmi-period/">http://dublincore.org/documents/dcmi-period/</a>.
 */
public class DCMIPeriod {

  private Date start;
  private Date end;
  private String name;

  /**
   * Create a new period. To create an open interval you may set one of the boundaries null.
   */
  public DCMIPeriod(Date start, Date end) {
    if (start == null && end == null)
      throw new IllegalStateException("A period must be bounded at least at one end");
    if (start != null && end != null && end.before(start))
      throw new IllegalStateException("The end date is before the start date");

    this.start = start;
    this.end = end;
  }

  /**
   * Create a new period with an optional name. To create an open interval you may set one of the bounbaries null.
   */
  public DCMIPeriod(Date start, Date end, String name) {
    this(start, end);

    this.name = name;
  }

  /**
   * Returns the start date of the period or null, if it has only an upper bound.
   */
  public Date getStart() {
    return start;
  }

  /**
   * Returns the end date of the period or null, if it has only a lower bound.
   */
  public Date getEnd() {
    return end;
  }

  /**
   * Returns the optional name of the period.
   * 
   * @return the name of the period or null
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the name of the period.
   * 
   * @param name
   *          the name or null, to delete it
   */
  void setName(String name) {
    this.name = name;
  }

  /**
   * Checks if the interval is closed.
   */
  public boolean isClosed() {
    return start != null && end != null;
  }

  /**
   * Checks if the interval has a start boundary.
   */
  public boolean hasStart() {
    return start != null;
  }

  /**
   * Checks if the interval has an end boundary.
   */
  public boolean hasEnd() {
    return end != null;
  }

  /**
   * Checks if the interval has a name.
   */
  public boolean hasName() {
    return name != null;
  }

  @Override
  public String toString() {
    return "DCMIPeriod{" + "start=" + (start != null ? start : "]") + ", end=" + (end != null ? end : "[")
            + (name != null ? ", name='" + name + '\'' : "") + '}';
  }

}
