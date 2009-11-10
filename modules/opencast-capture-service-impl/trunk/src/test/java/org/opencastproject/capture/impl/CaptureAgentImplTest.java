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
package org.opencastproject.capture.impl;

import org.opencastproject.media.mediapackage.MediaPackage;
import org.opencastproject.media.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageException;
import org.opencastproject.media.mediapackage.UnsupportedElementException;
import org.opencastproject.media.mediapackage.track.TrackImpl;
import org.opencastproject.util.ConfigurationException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import java.io.File;
import java.io.StringWriter;
import java.net.MalformedURLException;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class CaptureAgentImplTest {
  private static final Logger logger = LoggerFactory.getLogger(CaptureAgentImplTest.class);

  private static final String captureDir = CaptureAgentImpl.tmpPath;
  
  private CaptureAgentImpl service = null;
  private final File[] outFiles = {new File(captureDir+File.separator+"professor.mpg"),
                                   new File(captureDir+File.separator+"screen.mpg"),
                                   new File(captureDir+File.separator+"microphone.mp2"),
                                   new File(captureDir+File.separator+"capture.stopped")};
  private final long msecs = 10000;

  @Before
  public void setup() {
    service = new CaptureAgentImpl();

    // Checks output files don't exist
    for (File checkFile : outFiles) {
      if (checkFile.exists())
        checkFile.delete();
    }
  }

  @After
  public void teardown() {
    service = null;
  }

  @Test
  public void testCapture() {
    
    logger.info("Starting capture");
    service.startCapture();
    
    logger.info("Starting timing...");
    try {
      Thread.sleep(msecs);
    } catch (InterruptedException e) {
      logger.error("Unexpected exception while sleeping: "+e.getMessage());
      Assert.fail("Unexpected exception while sleeping: "+e.getMessage());
    }

    logger.info("End of timing. Stopping...");
    
    String result = service.stopCapture();

    // Checks correct return value
    Assert.assertTrue(result.equals("Capture OK"));
    
    // Checks for the existence of the expected files
    //for (File item : outFiles)
    //  Assert.assertTrue(item.exists());
    
    // Generates the manifest
    try {
      MediaPackage pkg = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
      
      
      for(File item : outFiles) {
        if (item.exists() && !(item.getName().equals("capture.stopped"))) {
          System.out.println(item.getName()); 
          pkg.add(TrackImpl.fromURL(item.toURL()));
        }
      }
      
      Document doc = pkg.toXml();
      
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");

      //initialize StreamResult with File object to save to file
      StreamResult result2 = new StreamResult(new StringWriter());
      DOMSource source = new DOMSource(doc);
      transformer.transform(source, result2);

      String xmlString = result2.getWriter().toString();
      System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n"+xmlString+"\n\n\n\n\n\n\n\n\n\n\n\n\n\n");

      
    } catch (ConfigurationException e) {
      logger.error("MediaPackage configuration exception: "+e.getMessage());
      Assert.fail("MediaPackage configuration exception: "+e.getMessage());
    } catch (MediaPackageException e) {
      logger.error("MediaPackage Exception: "+e.getMessage());
      Assert.fail("MediaPackage Exception: "+e.getMessage());
    } catch (MalformedURLException e) {
      logger.error("Malformed URL Exception: "+e.getMessage());
      Assert.fail("Malformed URL Exception: "+e.getMessage());
    } catch (UnsupportedElementException e) {
      logger.error("Unsupported Element Exception: "+e.getMessage());
      Assert.fail("Unsupported Element Exception: "+e.getMessage());
    } catch (TransformerException e) {
      logger.error("Transformer Exception: "+e.getMessage());
      Assert.fail("Transformer Exception: "+e.getMessage());
    }
    
    // Checks for the existence of the .zip file
    // TODO
    
    // Checks files inside the .zip file are those expected
    // TODO

  }

}



