/**
 *  Copyright 2009 Opencast Project (http://www.opencastproject.org)
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

package org.opencastproject.media.bundle.dublincore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.opencastproject.media.bundle.dublincore.utils.DCMIPeriod;
import org.opencastproject.media.bundle.dublincore.utils.EncodingSchemeUtils;
import org.opencastproject.media.bundle.dublincore.utils.Precision;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

/**
 * Test cases for {@link org.opencastproject.media.bundle.dublincore.utils.EncodingSchemeUtils}.
 *
 * @author Christoph E. Driessen <ced@neopoly.de>
 */
public class EncodingSchemeUtilsTest {

    @Test
    public void testEncodeDate() {
        Date now = new Date();
        assertEquals(4, EncodingSchemeUtils.encodeDate(now, Precision.Year).getValue().length());
        assertEquals(3, EncodingSchemeUtils.encodeDate(now, Precision.Day).getValue().split("-").length);
        assertEquals("2009-01-01T00:00:00Z".length(),
                EncodingSchemeUtils.encodeDate(now, Precision.Second).getValue().length());
        assertEquals(DublinCore.ENC_SCHEME_W3CDTF,
                EncodingSchemeUtils.encodeDate(now, Precision.Year).getEncodingScheme());
    }

    @Test
    public void testEncodeFraction() {
        Date a = new Date(1);
        Date b = new Date(125);
        Date c = new Date(100);
        assertEquals("1970-01-01T00:00:00.001Z",
                EncodingSchemeUtils.encodeDate(a, Precision.Fraction).getValue());
        assertEquals("1970-01-01T00:00:00.125Z",
                EncodingSchemeUtils.encodeDate(b, Precision.Fraction).getValue());
        assertEquals("1970-01-01T00:00:00.100Z",
                EncodingSchemeUtils.encodeDate(c, Precision.Fraction).getValue());
    }

    @Test
    public void testEncodePeriod() {
        DublinCoreValue a = EncodingSchemeUtils.encodePeriod(
                new DCMIPeriod(createDate(2007, 2, 10, 12, 0, 0), createDate(2009, 12, 24, 10, 0, 0), "long time"),
                Precision.Day);
        assertEquals("start=2007-02-10; end=2009-12-24; name=long time; scheme=W3C-DTF;", a.getValue());
        assertEquals(DublinCore.ENC_SCHEME_PERIOD, a.getEncodingScheme());
        DublinCoreValue b = EncodingSchemeUtils.encodePeriod(
                new DCMIPeriod(createDate(2007, 2, 10, 12, 0, 0), null),
                Precision.Day);
        assertEquals("start=2007-02-10; scheme=W3C-DTF;", b.getValue());
    }

    @Test
    public void testDecodeDate() {
        assertEquals(createDate(2008, 10, 1, 0, 0, 0),
                EncodingSchemeUtils.decodeDate(new DublinCoreValue("2008-10-01")));
    }

    @Test
    public void testDecodePeriod() {
        DCMIPeriod a = EncodingSchemeUtils.decodePeriod(new DublinCoreValue("start=2008-10-01; end=2009-01-01;"));
        assertEquals(createDate(2008, 10, 1, 0, 0, 0), a.getStart());
        assertEquals(createDate(2009, 1, 1, 0, 0, 0), a.getEnd());
        DCMIPeriod b = EncodingSchemeUtils.decodePeriod(new DublinCoreValue("start=2008-10-01; end=2009-01-01"));
        assertEquals(createDate(2008, 10, 1, 0, 0, 0), b.getStart());
        assertEquals(createDate(2009, 1, 1, 0, 0, 0), b.getEnd());
        DCMIPeriod c = EncodingSchemeUtils.decodePeriod(new DublinCoreValue("start=2008-10-01"));
        assertEquals(createDate(2008, 10, 1, 0, 0, 0), c.getStart());
        assertNull(c.getEnd());
        DCMIPeriod d = EncodingSchemeUtils.decodePeriod(new DublinCoreValue("start=2008-10-01T10:20Z; end=2009-01-01; scheme=UNKNOWN"));
        assertNull(d);
        DCMIPeriod e = EncodingSchemeUtils.decodePeriod(new DublinCoreValue("start=2008-10-01T10:20Z; end=2009-01-01; scheme=W3C-DTF"));
        assertNotNull(e);
        DCMIPeriod f = EncodingSchemeUtils.decodePeriod(new DublinCoreValue("start=2008-10-01ERR; end=2009-01-01; scheme=W3C-DTF"));
        assertNull(f);
    }

    @Test
    public void testDecodeTemporal() {
        assertTrue(EncodingSchemeUtils.decodeTemporal(new DublinCoreValue("start=2008-10-01; end=2009-01-01;"))
                instanceof DCMIPeriod);
        assertTrue(EncodingSchemeUtils.decodeTemporal(new DublinCoreValue("2008-10-01"))
                instanceof Date);
        assertTrue(EncodingSchemeUtils.decodeTemporal(new DublinCoreValue("2008-10-01T10:30:05Z"))
                instanceof Date);
        assertTrue(EncodingSchemeUtils.decodeTemporal(new DublinCoreValue("start=2008-10-01T10:20Z; end=2009-01-01; scheme=W3C-DTF"))
                instanceof DCMIPeriod);
    }

    public static Date createDate(int year, int month, int day, int hour, int minute, int second) {
        Calendar c = Calendar.getInstance();
        c.set(year, month - 1, day, hour, minute, second);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }
}
