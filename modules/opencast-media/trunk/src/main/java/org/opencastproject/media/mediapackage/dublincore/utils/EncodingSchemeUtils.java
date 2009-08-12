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

package org.opencastproject.media.mediapackage.dublincore.utils;

import org.opencastproject.media.mediapackage.dublincore.DublinCore;
import org.opencastproject.media.mediapackage.dublincore.DublinCoreValue;
import org.opencastproject.util.Assert;
import org.opencastproject.util.MapBuilder;

import org.joda.time.format.ISODateTimeFormat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to facilitate the work with DCMI encoding schemes.
 * 
 * @author Christoph E. Driessen <ced@neopoly.de>
 */
public class EncodingSchemeUtils {

  private static final Map<Precision, String> formats = new MapBuilder<Precision, String>().put(Precision.Year, "yyyy")
          .put(Precision.Month, "yyyy-MM").put(Precision.Day, "yyyy-MM-dd").put(Precision.Minute,
                  "yyyy-MM-dd'T'HH:mm'Z'").put(Precision.Second, "yyyy-MM-dd'T'HH:mm:ss'Z'").put(Precision.Fraction,
                  "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").toMap();

  /**
   * Encode a date with the given precision into a Dublin Core string value, using the recommended W3C-DTF scheme and
   * the UTC timezone.
   * <p/>
   * See <a href="http://www.w3.org/TR/NOTE-datetime">http://www.w3.org/TR/NOTE- datetime</a> for more information about
   * W3C-DTF.
   * 
   * @param date
   *          the date to encode
   * @param precision
   *          the precision to use
   */
  public static DublinCoreValue encodeDate(Date date, Precision precision) {
    Assert.notNull(date, "The date must not be null");
    Assert.notNull(precision, "The precision must not be null");

    SimpleDateFormat f = new SimpleDateFormat(formats.get(precision));
    f.setTimeZone(TimeZone.getTimeZone("UTC"));
    return new DublinCoreValue(f.format(date), DublinCore.LANGUAGE_UNDEFINED, DublinCore.ENC_SCHEME_W3CDTF);
  }

  /**
   * Encode a period with the given precision into a Dublin Core string value using the recommended DCMI Period scheme
   * and the UTC timezone. One of the dates may be null to create an open interval.
   * <p/>
   * See <a href="http://dublincore.org/documents/dcmi-period/">http://dublincore .org/documents/dcmi-period/</a> for
   * more information about DCMI Period.
   * 
   * @param period
   *          the period
   * @param precision
   *          the precision
   */
  public static DublinCoreValue encodePeriod(DCMIPeriod period, Precision precision) {
    Assert.notNull(period, "The period must not be null");
    Assert.notNull(precision, "The precision must not be null");

    SimpleDateFormat f = new SimpleDateFormat(formats.get(precision));
    f.setTimeZone(TimeZone.getTimeZone("UTC"));
    StringBuilder b = new StringBuilder();
    if (period.hasStart()) {
      b.append("start=").append(f.format(period.getStart())).append(";");
    }
    if (period.hasEnd()) {
      if (b.length() > 0)
        b.append(" ");
      b.append("end=").append(f.format(period.getEnd())).append(";");
    }
    if (period.hasName()) {
      b.append(" ").append("name=").append(period.getName().replace(";", "")).append(";");
    }
    b.append(" ").append("scheme=W3C-DTF;");
    return new DublinCoreValue(b.toString(), DublinCore.LANGUAGE_UNDEFINED, DublinCore.ENC_SCHEME_PERIOD);
  }

  /**
   * Tries to decode the given value as a W3C-DTF encoded date. If decoding fails, null is returned.
   * 
   * @return the date or null if decoding fails
   */
  public static Date decodeDate(DublinCoreValue value) {
    if (value.getEncodingScheme() == null || value.getEncodingScheme().equals(DublinCore.ENC_SCHEME_W3CDTF)) {
      try {
        return parseW3CDTF(value.getValue());
      } catch (IllegalArgumentException ignore) {
      }
    }
    return null;
  }

  /**
   * Like {@link #decodeDate(org.opencastproject.media.mediapackage.dublincore.DublinCoreValue)} , but throws an
   * {@link IllegalArgumentException} if the value cannot be decoded.
   * 
   * @return the date
   * @throws IllegalArgumentException
   *           if the value cannot be decoded
   */
  public static Date decodeMandatoryDate(DublinCoreValue value) {
    Date date = decodeDate(value);
    if (date == null)
      throw new IllegalArgumentException("Cannot decode to Date: " + value);
    return date;
  }

  private static final Pattern DCMI_PERIOD = Pattern.compile("(start|end|name)\\s*=\\s*(.*?)(?:;|\\s*$)");
  private static final Pattern DCMI_PERIOD_SCHEME = Pattern.compile("scheme\\s*=\\s*(.*?)(?:;|\\s*$)");

  /**
   * Tries to decode a string in the DCMI period format, using W3C-DTF for the encoding of the individual dates. If
   * parsing fails at any point, null will be returned.
   * 
   * @return the period or null if decoding fails
   */
  public static DCMIPeriod decodePeriod(DublinCoreValue value) {
    // Parse value
    Matcher schemeMatcher = DCMI_PERIOD_SCHEME.matcher(value.getValue());
    boolean mayBeW3CDTFEncoded = true;
    if (schemeMatcher.find()) {
      String schemeString = schemeMatcher.group(1);
      if (!schemeString.equalsIgnoreCase("W3C-DTF") && !schemeString.equalsIgnoreCase("W3CDTF")) {
        mayBeW3CDTFEncoded = false;
      }
    }
    try {
      if (mayBeW3CDTFEncoded) {
        // Declare fields
        Date start = null;
        Date end = null;
        String name = null;
        // Parse
        Matcher m = DCMI_PERIOD.matcher(value.getValue());
        while (m.find()) {
          String field = m.group(1);
          String fieldValue = m.group(2);
          if ("start".equals(field)) {
            if (start != null)
              return null;
            start = parseW3CDTF(fieldValue);
          } else if ("end".equals(field)) {
            if (end != null)
              return null;
            end = parseW3CDTF(fieldValue);
          } else if ("name".equals(field)) {
            if (name != null)
              return null;
            name = fieldValue;
          }
        }
        return new DCMIPeriod(start, end, name);
      }
    } catch (IllegalArgumentException ignore) {
      // Parse error
    }
    return null;
  }

  /**
   * Like {@link #decodePeriod(org.opencastproject.media.mediapackage.dublincore.DublinCoreValue)} , but throws an
   * {@link IllegalArgumentException} if the value cannot be decoded.
   * 
   * @return the period
   * @throws IllegalArgumentException
   *           if the value cannot be decoded
   */
  public static DCMIPeriod decodeMandatoryPeriod(DublinCoreValue value) {
    DCMIPeriod period = decodePeriod(value);
    if (period == null)
      throw new IllegalArgumentException("Cannot decode to DCMIPeriod: " + value);

    return period;
  }

  /**
   * Tries to decode the value to a temporal object. For now, supported types are {@link java.util.Date} and
   * {@link DCMIPeriod}
   * 
   * @param value
   *          the value to decode
   * @return a temporal object of the said types or null if decoding fails
   */
  public static Object decodeTemporal(DublinCoreValue value) {
    // First try Date
    Object temporal = decodeDate(value);
    if (temporal == null) {
      // ... then try DCMIPeriod
      temporal = decodePeriod(value);
    }
    return temporal;
  }

  /**
   * Like {@link #decodeTemporal(org.opencastproject.media.mediapackage.dublincore.DublinCoreValue)} , but throws an
   * {@link IllegalArgumentException} if the value cannot be decoded.
   * 
   * @return the temporal object of type {@link java.util.Date} or {@link DCMIPeriod}
   * @throws IllegalArgumentException
   *           if the value cannot be decoded
   */
  public static Object decodeMandatoryTemporal(DublinCoreValue value) {
    Object temporal = decodeTemporal(value);
    if (value == null)
      throw new IllegalArgumentException("Cannot decode to either Date or DCMIPeriod: " + value);

    return temporal;
  }

  /**
   * @throws IllegalArgumentException
   *           if the value cannot be parsed
   */
  private static final Date parseW3CDTF(String value) {
    return ISODateTimeFormat.dateTimeParser().parseDateTime(value).toDate();
  }
}
