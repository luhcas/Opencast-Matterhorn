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

package org.opencastproject.media.mediapackage.dublincore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opencastproject.media.mediapackage.dublincore.utils.EncodingSchemeUtils.decodeDate;
import static org.opencastproject.media.mediapackage.dublincore.utils.EncodingSchemeUtils.decodeDuration;
import static org.opencastproject.media.mediapackage.dublincore.utils.EncodingSchemeUtils.decodePeriod;
import static org.opencastproject.media.mediapackage.dublincore.utils.EncodingSchemeUtils.decodeTemporal;
import static org.opencastproject.media.mediapackage.dublincore.utils.EncodingSchemeUtils.encodeDate;
import static org.opencastproject.media.mediapackage.dublincore.utils.EncodingSchemeUtils.encodeDuration;
import static org.opencastproject.media.mediapackage.dublincore.utils.EncodingSchemeUtils.encodePeriod;

import org.opencastproject.media.mediapackage.dublincore.utils.DCMIPeriod;
import org.opencastproject.media.mediapackage.dublincore.utils.Precision;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Test cases for {@link org.opencastproject.media.mediapackage.dublincore.utils.EncodingSchemeUtils} .
 * 
 * @author Christoph E. Driessen <ced@neopoly.de>
 */
public class EncodingSchemeUtilsTest {

  @Test
  public void testEncodeDate() {
      Date now = new Date();
      assertEquals(4, encodeDate(now, Precision.Year).getValue().length());
      assertEquals(3, encodeDate(now, Precision.Day).getValue().split("-").length);
      assertEquals("2009-01-01T00:00:00Z".length(),
              encodeDate(now, Precision.Second).getValue().length());
      assertEquals(DublinCore.ENC_SCHEME_W3CDTF,
              encodeDate(now, Precision.Year).getEncodingScheme());
      // Test symmetry
      assertEquals(decodeDate(encodeDate(now, Precision.Second)), precisionSecond(now));
      assertEquals(decodeDate(encodeDate(now, Precision.Day)), precisionDay(now));
      //
      assertEquals("1724-04-22", encodeDate(createDate(1724, 4, 22, 18, 30, 0), Precision.Day).getValue());
      assertEquals("1724-04-22T18:30:00Z", encodeDate(createDate(1724, 4, 22, 18, 30, 0, "UTC"), Precision.Second).getValue());
      assertEquals("1724-04-22T17:30:10Z", encodeDate(createDate(1724, 4, 22, 18, 30, 10, "GMT+1"), Precision.Second).getValue());
      assertEquals("1724-04-22T17:30Z", encodeDate(createDate(1724, 4, 22, 18, 30, 25, "GMT+1"), Precision.Minute).getValue());
      assertEquals("1999-03-21", encodeDate(createDate(1999, 3, 21, 18, 30, 25, "GMT+1"), Precision.Day).getValue());
      //
      System.out.println(encodeDate(now, Precision.Day).getValue());
      System.out.println(encodeDate(now, Precision.Second).getValue());
  }

  @Test
  public void testEncodeFraction() {
      Date a = new Date(1);
      Date b = new Date(125);
      Date c = new Date(100);
      assertEquals("1970-01-01T00:00:00.001Z",
              encodeDate(a, Precision.Fraction).getValue());
      assertEquals("1970-01-01T00:00:00.125Z",
              encodeDate(b, Precision.Fraction).getValue());
      assertEquals("1970-01-01T00:00:00.100Z",
              encodeDate(c, Precision.Fraction).getValue());
  }

  @Test
  public void testEncodePeriod() {
      DublinCoreValue a = encodePeriod(
              new DCMIPeriod(createDate(2007, 2, 10, 12, 0, 0), createDate(2009, 12, 24, 10, 0, 0), "long time"),
              Precision.Day);
      assertEquals("start=2007-02-10; end=2009-12-24; name=long time; scheme=W3C-DTF;", a.getValue());
      assertEquals(DublinCore.ENC_SCHEME_PERIOD, a.getEncodingScheme());
      DublinCoreValue b = encodePeriod(
              new DCMIPeriod(createDate(2007, 2, 10, 12, 0, 0), null),
              Precision.Day);
      assertEquals("start=2007-02-10; scheme=W3C-DTF;", b.getValue());
  }

  @Test
  public void testDecodeDate() {
      assertEquals(createDate(2008, 10, 1, 0, 0, 0), decodeDate(new DublinCoreValue("2008-10-01")));
      assertEquals(createDate(1999, 3, 21, 14, 30, 0, "UTC"), decodeDate(new DublinCoreValue("1999-03-21T14:30Z")));
      assertEquals(createDate(1999, 3, 21, 14, 30, 0, "UTC"), decodeDate(new DublinCoreValue("1999-03-21T14:30:00Z")));
      assertEquals(createDate(1999, 3, 21, 14, 30, 15, "UTC"), decodeDate(new DublinCoreValue("1999-03-21T14:30:15Z")));
      assertEquals(createDate(1999, 3, 21, 14, 30, 15, "GMT+1"), decodeDate(new DublinCoreValue("1999-03-21T13:30:15Z")));
      assertEquals(createDate(2001, 9, 11, 0, 0, 0), decodeDate(new DublinCoreValue("2001-09-11")));
      System.out.println(decodeDate(new DublinCoreValue("2009-03-31")));
      System.out.println(decodeDate(new DublinCoreValue("2009-09-11")));
      System.out.println(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(decodeDate(new DublinCoreValue("2009-03-31"))));
  }

  @Test
  public void testDecodePeriod() {
      DCMIPeriod a = decodePeriod(new DublinCoreValue("start=2008-10-01; end=2009-01-01;"));
      assertEquals(createDate(2008, 10, 1, 0, 0, 0), a.getStart());
      assertEquals(createDate(2009, 1, 1, 0, 0, 0), a.getEnd());
      DCMIPeriod b = decodePeriod(new DublinCoreValue("start=2008-10-01; end=2009-01-01"));
      assertEquals(createDate(2008, 10, 1, 0, 0, 0), b.getStart());
      assertEquals(createDate(2009, 1, 1, 0, 0, 0), b.getEnd());
      DCMIPeriod c = decodePeriod(new DublinCoreValue("start=2008-10-01"));
      assertEquals(createDate(2008, 10, 1, 0, 0, 0), c.getStart());
      assertNull(c.getEnd());
      DCMIPeriod d = decodePeriod(new DublinCoreValue("start=2008-10-01T10:20Z; end=2009-01-01; scheme=UNKNOWN"));
      assertNull(d);
      DCMIPeriod e = decodePeriod(new DublinCoreValue("start=2008-10-01T10:20Z; end=2009-01-01; scheme=W3C-DTF"));
      assertNotNull(e);
      DCMIPeriod f = decodePeriod(new DublinCoreValue("start=2008-10-01ERR; end=2009-01-01; scheme=W3C-DTF"));
      assertNull(f);
  }

  @Test
  public void testDecodeTemporal() {
      assertTrue(decodeTemporal(new DublinCoreValue("start=2008-10-01; end=2009-01-01;"))
              instanceof DCMIPeriod);
      assertTrue(decodeTemporal(new DublinCoreValue("2008-10-01"))
              instanceof Date);
      assertTrue(decodeTemporal(new DublinCoreValue("2008-10-01T10:30:05Z"))
              instanceof Date);
      assertTrue(decodeTemporal(new DublinCoreValue("start=2008-10-01T10:20Z; end=2009-01-01; scheme=W3C-DTF"))
              instanceof DCMIPeriod);
  }

  @Test
  public void testEncodeDuration() {
      Long d1 = 2743414L;
      assertEquals(d1, decodeDuration(encodeDuration(d1).getValue()));
      Long d2 = 78534795325L;
      assertEquals(d2, decodeDuration(encodeDuration(d2).getValue()));
      Long d3 = 234L;
      assertEquals(d3, decodeDuration(encodeDuration(d3).getValue()));

      assertEquals(DublinCore.ENC_SCHEME_ISO8601, encodeDuration(d3).getEncodingScheme());

      // Try to decode 01:10:05
      try {
        assertEquals(new Long(1 * 1000 * 60 * 60 + 10 * 1000 * 60 + 5 * 1000), decodeDuration("01:10:05"));
        fail("Decoding of duration in ms passed although it should have failed");
      } catch (IllegalArgumentException e) {
        // this is expected
      }

      try {
        assertNull(decodeDuration(new DublinCoreValue("asdsad")));
        fail("Decoding of nonsense passed although it should have failed");
      } catch (IllegalArgumentException e) {
        // this is expected
      }

      assertNull(decodeDuration(new DublinCoreValue(encodeDuration(d1).getValue(), DublinCore.LANGUAGE_UNDEFINED, DublinCore.ENC_SCHEME_BOX)));
  }

  public static Date createDate(int year, int month, int day, int hour, int minute, int second) {
      Calendar c = Calendar.getInstance();
      c.set(year, month - 1, day, hour, minute, second);
      c.set(Calendar.MILLISECOND, 0);
      return c.getTime();
  }

  public static Date createDate(int year, int month, int day, int hour, int minute, int second, String tz) {
      Calendar c = Calendar.getInstance(TimeZone.getTimeZone(tz));
      c.set(year, month - 1, day, hour, minute, second);
      c.set(Calendar.MILLISECOND, 0);
      return c.getTime();
  }

  public static Date setTenOClock(Date date) {
      Calendar c = Calendar.getInstance();
      c.setTime(date);
      c.set(Calendar.HOUR_OF_DAY, 10);
      c.set(Calendar.MINUTE, 0);
      c.set(Calendar.SECOND, 0);
      c.set(Calendar.MILLISECOND, 0);
      return c.getTime();
  }

  public static Date precisionSecond(Date date) {
      Calendar c = Calendar.getInstance();
      c.setTime(date);
      c.set(Calendar.MILLISECOND, 0);
      return c.getTime();
  }

  public static Date precisionDay(Date date) {
      Calendar c = Calendar.getInstance();
      c.setTime(date);
      c.set(Calendar.HOUR_OF_DAY, 0);
      c.set(Calendar.MINUTE, 0);
      c.set(Calendar.SECOND, 0);
      c.set(Calendar.MILLISECOND, 0);
      return c.getTime();
  }

}