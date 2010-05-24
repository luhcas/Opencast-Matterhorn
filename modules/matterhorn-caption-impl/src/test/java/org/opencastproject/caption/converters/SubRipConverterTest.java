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
package org.opencastproject.caption.converters;

import org.opencastproject.caption.api.CaptionCollection;
import org.opencastproject.caption.api.IllegalCaptionFormatException;
import org.opencastproject.caption.converters.SubRipCaptionConverter;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Pattern;


/**
 * Test class for SubRip format.
 *
 */
public class SubRipConverterTest {
  
  // SubRip converter
  private SubRipCaptionConverter format;
  // srt sample
  private String srtSample;
  
  @Before
  public void setup() throws IOException{
    format = new SubRipCaptionConverter();
    InputStream input = SubRipConverterTest.class.getResourceAsStream("/sample.srt");
    // loading sample as string
    srtSample = parseInputStream(input);
  }
  
  @Test
  public void testImport(){
    // verify pattern matching
    Assert.assertTrue(Pattern.compile(format.getIdPattern()).matcher(srtSample).find());
    try {
      // verify parsing and exporting without exceptions
      CaptionCollection collection = format.importCaption(srtSample);
      String srt = format.exportCaption(collection);
      Assert.assertTrue(srt.startsWith("1"));
    } catch (IllegalCaptionFormatException e) {
      Assert.fail(e.getMessage());
    }
  }
  
  /**
   * Loading sample from {@link InputStream} as String.
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
