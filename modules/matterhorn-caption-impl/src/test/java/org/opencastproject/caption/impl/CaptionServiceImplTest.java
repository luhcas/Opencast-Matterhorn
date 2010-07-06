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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

/**
 * Test for {@link CaptionServiceImpl}.
 * 
 */
public class CaptionServiceImplTest {

  // caption service
  private CaptionService service;
  // DFXP captions sample
  private InputStream inputStream;
  // Output stream
  private ByteArrayOutputStream outputStream;
  // expected srt output
  private String expectedOutput = 
      "1\r\n"
    + "00:00:00,000 --> 00:00:05,000\r\n"
    + "Alex Cross\r\n"
    + "This is my 1st caption";

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
    inputStream = CaptionServiceImplTest.class.getResourceAsStream("/sample.dfxp.xml");
    outputStream = new ByteArrayOutputStream();
  }
  
  @Test
  public void retrieveLanguageList() {
    try {
      List<String> langList = service.getLanguageList(inputStream, "DFXP");
      Assert.assertNotNull(langList);
      Assert.assertTrue(langList.contains("en"));
      Assert.assertTrue(langList.contains("fr"));
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testCoversion() {
    try {
      service.convert(inputStream, "DFXP", outputStream, "SubRip", "en");
      Assert.assertTrue(outputStream.toString("UTF-8").startsWith(expectedOutput));
    } catch (UnsupportedCaptionFormatException e) {
      Assert.fail(e.getMessage());
    } catch (IllegalCaptionFormatException e) {
      Assert.fail(e.getMessage());
    } catch (IOException e) {
      Assert.fail(e.getMessage());
    }
  }
  
  @After
  public void tear() throws IOException {
    inputStream.close();
    outputStream.close();
  }
}
