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
package org.opencastproject.util;

import org.apache.commons.lang.StringUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utility class for the solr database.
 */
public final class SolrUtils {

  /** Disallow construction of this utility class */
  private SolrUtils() {
  }

  /** The solr date format string tag. */
  public static final String SOLR_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

  /** The solr supported date format. **/
  protected static DateFormat dateFormat = new SimpleDateFormat(SOLR_DATE_FORMAT);

  /** The solr supported date format for days **/
  protected static DateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd");

  /** The regular filter expression for single characters */
  private static final String charCleanerRegex = "([\\+\\-\\!\\(\\)\\{\\}\\[\\]\\\\^\"\\~\\*\\?\\:])";

  /**
   * Clean up the user query input string to avoid invalid input parameters.
   * 
   * @param q
   *          The input String.
   * @return The cleaned string.
   */
  public static String clean(String q) {
    q = q.replaceAll(charCleanerRegex, "\\\\$1");
    q = q.replaceAll("\\&\\&", "\\\\&\\\\&");
    q = q.replaceAll("\\|\\|", "\\\\|\\\\|");
    return q;
  }

  /**
   * Returns a serialized version of the date or <code>null</code> if <code>null</code> was passed in for the date.
   * 
   * @param date
   *          the date
   * @return the serialized date
   */
  public static String serializeDate(Date date) {
    if (date == null)
      return null;
    return dateFormat.format(date);
  }

  /**
   * Returns the date or <code>null</code> if <code>null</code> was passed in for the date.
   * 
   * @param date
   *          the serialized date
   * @return the date
   * @throws ParseException
   *           if parsing the date fails
   */
  public static Date parseDate(String date) throws ParseException {
    if (StringUtils.isBlank(date))
      return null;
    return dateFormat.parse(date);
  }

  /**
   * Returns an expression to search for any date that lies in between <code>startDate</date> and <code>endDate</date>.
   * 
   * @param startDate
   *          the start date
   * @param endDate
   *          the end date
   * @return the serialized search expression
   */
  public static String serializeDateRange(Date startDate, Date endDate) {
    if (startDate == null)
      throw new IllegalArgumentException("Start date cannot be null");
    if (endDate == null)
      throw new IllegalArgumentException("End date cannot be null");
    StringBuffer buf = new StringBuffer("[");
    buf.append(dateFormat.format(startDate));
    buf.append(" TO ");
    buf.append(dateFormat.format(endDate));
    buf.append("]");
    return buf.toString();
  }

  /**
   * Returns an expression to search for the given day.
   * 
   * @param date
   *          the date
   * @return the serialized search expression
   */
  public static String selectDay(Date date) {
    if (date == null)
      return null;
    StringBuffer buf = new StringBuffer("[");
    buf.append(dayFormat.format(date)).append("T00:00:00Z");
    buf.append(" TO ");
    buf.append(dayFormat.format(date)).append("T23:59:59Z");
    buf.append("]");
    return buf.toString();
  }

}
