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
package org.opencastproject.analysis.text;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.opencastproject.analysis.text.ocropus.OcropusTextAnalyzer;
import org.opencastproject.analysis.text.ocropus.OcropusTextFrame;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;

/**
 * Test case for class {@link OcropusTextAnalyzer}.
 */
public class OcropusTextAnalyzerTest {

  /** Path to the test image */
  protected String testPath = "/image.jpg";

  /** Test image */
  protected File testFile = null;

  /** Path to the ocropus binary */
  protected static String ocropusbinary = OcropusTextAnalyzer.OCROPUS_BINARY_DEFAULT;

  /** The ocropus text analyzer */
  protected OcropusTextAnalyzer analyzer = null;
  
  /** The text without punctuation */
  protected String text = "Land and Vegetation Key players on the";

  /** True to run the tests */
  private static boolean ocropusInstalled = true;
  
  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(OcropusTextAnalyzerTest.class);

  @BeforeClass
  public static void testOcropus() {
    try {
      Process p = new ProcessBuilder(ocropusbinary).start();
      if (p.waitFor() != 0)
        throw new IllegalStateException();
    } catch (Throwable t) {
      logger.warn("Skipping text analysis tests due to unsatisifed ocropus installation");
      ocropusInstalled = false;
    }
  }

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    URL imageUrl = this.getClass().getResource(testPath);
    testFile = File.createTempFile("ocrtest", ".jpg");
    FileUtils.copyURLToFile(imageUrl, testFile);
    analyzer = new OcropusTextAnalyzer(ocropusbinary);
  }

  /**
   * @throws java.io.File.IOException
   */
  @After
  public void tearDown() throws Exception {
    FileUtils.deleteQuietly(testFile);
  }

  /**
   * Test method for {@link org.opencastproject.analysis.text.ocropus.OcropusTextAnalyzer#getBinary()}.
   */
  @Test
  public void testGetBinary() {
    assertEquals(ocropusbinary, analyzer.getBinary());
  }

  /**
   * Test method for {@link org.opencastproject.analysis.text.ocropus.OcropusTextAnalyzer#analyze(java.io.File)}.
   */
  @Test
  public void testAnalyze() {
    if (!ocropusInstalled)
      return;
    
    if (!new File(ocropusbinary).exists())
      return;
    OcropusTextFrame frame = analyzer.analyze(testFile);
    assertTrue(frame.hasText());
    assertEquals(text, frame.getLines()[0].getText());
  }

}
