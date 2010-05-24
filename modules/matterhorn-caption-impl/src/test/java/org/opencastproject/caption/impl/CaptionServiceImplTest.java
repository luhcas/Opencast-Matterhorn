/**
 *  Copyright 2010 The Regents of the University of California
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

import org.opencastproject.caption.api.CaptionConverter;
import org.opencastproject.caption.api.CaptionService;
import org.opencastproject.caption.api.IllegalCaptionFormatException;
import org.opencastproject.caption.api.UnsupportedCaptionFormatException;
import org.opencastproject.caption.converters.DFXPCaptionConverter;
import org.opencastproject.caption.converters.SubRipCaptionConverter;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

/**
 * Test for {@link CaptionServiceImpl}.
 * 
 */
public class CaptionServiceImplTest {

  // caption service
  private CaptionService service;
  // DFXP captions sample
  private String dfxpSample;

  @Before
  public void setup() throws IOException {
    // setting caption formats
    final HashMap<String, CaptionConverter> captionConverters = new HashMap<String, CaptionConverter>();
    captionConverters.put("SubRip", new SubRipCaptionConverter());
    captionConverters.put("DFXP", new DFXPCaptionConverter());

    // creating service
    service = new CaptionServiceImpl() {
      @Override
      protected HashMap<String, CaptionConverter> getAvailableCaptionConverters() {
        // override the method for getting caption formats registered on the system
        return captionConverters;
      }

      @Override
      protected CaptionConverter getCaptionConverter(String formatName) {
        return captionConverters.get(formatName);
      }      
    };

    // loading sample
    dfxpSample = parseInputStream(CaptionServiceImplTest.class.getResourceAsStream("/sample.dfxp.xml"));  
  }

  @Test
  public void testCoversionWithSpecifiedInputParameter() {
    try {
      String result = service.convert(dfxpSample, "DFXP", "SubRip");
      Assert.assertTrue(result.startsWith("1"));
    } catch (UnsupportedCaptionFormatException e) {
      Assert.fail(e.getMessage());
    } catch (IllegalCaptionFormatException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testConversionWithAutoFind() {
    try {
      String result = service.convert(dfxpSample, "SubRip");
      Assert.assertTrue(result.startsWith("1"));
    } catch (UnsupportedCaptionFormatException e) {
      Assert.fail(e.getMessage());
    } catch (IllegalCaptionFormatException e) {
      Assert.fail(e.getMessage());
    }
  }
  
  /**
   * Loading sample from {@link InputStream}.
   * @param is
   * @return
   * @throws IOException
   */
  private String parseInputStream(InputStream is) throws IOException {
    if (is != null) {
      // initialize StringBuffer
      StringBuffer buffer = new StringBuffer();
      String line;
      BufferedReader reader = new BufferedReader(new InputStreamReader(is));
      try {
        while ((line = reader.readLine()) != null) {
          buffer.append(line).append(System.getProperty("line.separator"));
        }
        reader.close();
      } catch (IOException e) {
        throw e;
      } finally {
        reader.close();
        is.close();
      }
      return buffer.toString();
    } else {
      return "";
    }
  }
}
