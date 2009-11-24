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

import java.io.File;
import java.util.Properties;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CaptureAgentImplTest {
  private static final Logger logger = LoggerFactory.getLogger(CaptureAgentImplTest.class);

  private static final String captureDir = CaptureAgentImpl.tmpPath;
  //private static final String ingestURL = "http://nightly.opencastproject.org/ingest/rest/addZippedMediaPackage";
  private static final String ingestURL = "http://rubenrua.uvigo.es:8080";
  
  private CaptureAgentImpl service = null;
  private final File[] outFiles = {new File(captureDir+File.separator+"professor.mpg"),
                                   new File(captureDir+File.separator+"screen.mpg"),
                                   new File(captureDir+File.separator+"microphone.mp2"),
                                   new File(captureDir+File.separator+"capture.stopped")};
  private final File manifest = new File(captureDir+File.separator+"manifest.xml");
  private final long msecs = 10000;
  

  @Before
  public void setup() {
    service = new CaptureAgentImpl();

    // Checks output files don't exist
    for (File checkFile : outFiles) {
      if (checkFile.exists())
        checkFile.delete();
    }
    
    if (manifest.exists())
      manifest.delete();
  }

  @After
  public void teardown() {
    service = null;
  }

  @Test
  public void testCapture() {
     
    try {
      /* setup some default hard-coded properties */
      Properties props = new Properties();
      props.setProperty(CaptureParameters.CAPTURE_DEVICE_NAMES, "SCREEN,PRESENTER,AUDIO");
      props.setProperty("capture.device.SCREEN.src", "/dev/video1");
      props.setProperty("capture.device.SCREEN.outputfile", outFiles[1].getAbsolutePath());
      props.setProperty("capture.device.PRESENTER.src", "/dev/video0");
      props.setProperty("capture.device.PRESENTER.outputfile", outFiles[0].getAbsolutePath());
      props.setProperty("capture.device.AUDIO.src", "hw:0");
      props.setProperty("capture.device.AUDIO.outputfile", outFiles[2].getAbsolutePath());
      
      logger.info("Starting capture.");
      
      service.startCapture(props);
      
      logger.info("Starting timing.");
      try {
        Thread.sleep(msecs);
      } catch (InterruptedException e) {
        logger.error("Unexpected exception while sleeping: {}.", e.getMessage());
        Assert.fail("Unexpected exception while sleeping: " + e.getMessage());
      }
  
      logger.info("End of timing. Stopping...");
      
      String result = service.stopCapture();
  
      // Checks correct return value
      Assert.assertTrue(result.equals("Capture OK"));
      
      System.out.println("After AssertTrue()");

    } catch (UnsatisfiedLinkError e) {
      logger.error("Could not properly test capture agent: {}.", e.getMessage());
    }
    
    // Checks for the existence of the expected files
    for (File item : outFiles)
      Assert.assertTrue(item.exists());

    logger.info("Existence checked.");

    //TODO:  Bring this back, or kill it.  If it comes back, let greg_logan know so he can fix the logging style here.
    // Generates the manifest
    /*    try {
      MediaPackage pkg = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
        
      // Inserts the files in the MediaPackage
      for(java.io.File item : outFiles) {
        if (item.exists()) 
        if (item.getName().substring(item.getName().lastIndexOf('.')).equals(".xml"))
          pkg.add(item.toURL());
        else
          pkg.add(TrackImpl.fromURL(item.toURL()));
      }

            
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
    } catch (IOException e) {
      logger.error("I/O Exception: "+e.getMessage());
      Assert.fail("I/O Exception: "+e.getMessage());
    }
*/
      // Zips files
      File zip = new File(captureDir+File.separator+"output.zip");
      service.zipFiles(zip.getAbsolutePath());
      
      logger.info("Files zipped.");

      // Checks for the existence of the .zip file
      Assert.assertTrue(zip.exists());
      
      // Checks that files inside the .zip file are those expected
      // TODO  Necessary?      
      
      logger.info("Let's injest.");

      // Ingests the file
      logger.info("Ingest result:\n==============\n{}", service.doIngest(ingestURL, zip));
    
  }

}



