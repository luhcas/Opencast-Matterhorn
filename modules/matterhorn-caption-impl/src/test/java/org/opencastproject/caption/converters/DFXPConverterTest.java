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
import org.opencastproject.caption.converters.DFXPCaptionConverter;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

/**
 * 
 * Test class for DFXP converter.
 * 
 */
public class DFXPConverterTest {

  // converter
  private DFXPCaptionConverter DFXPConverter;
  // sample
  private String dfxpSample;

  @Before
  public void setup() throws IOException {
    DFXPConverter = new DFXPCaptionConverter();
    InputStream input = DFXPConverterTest.class.getResourceAsStream("/sample.dfxp.xml");
    // load sample as string
    dfxpSample = parseInputStream(input);
  }

  @Test
  public void testDFXPConversion() {
    try {
      // verify pattern matching
      Assert.assertTrue(Pattern.compile(DFXPConverter.getIdPattern()).matcher(dfxpSample).find());
      // verify conversion parsing and exporting without exception
      CaptionCollection collection = DFXPConverter.importCaption(dfxpSample);
      String srt = DFXPConverter.exportCaption(collection);
      Assert.assertTrue(srt.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
    } catch (Exception e) {
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
