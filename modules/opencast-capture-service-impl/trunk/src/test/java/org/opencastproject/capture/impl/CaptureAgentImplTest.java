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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Properties;

public class CaptureAgentImplTest {
    private static final Logger logger = LoggerFactory.getLogger(CaptureAgentImplTest.class);
    
    
    private CaptureAgentImpl service = null;
    /** Defines the default paths for the files captured
        TODO: Should be better to use the ConfigurationManager to be more faithful to the way in which this is actually done?
        TODO: At least use File.separator to be more system-independent */
    
    private final File outDir = new File(this.getClass().getResource("/.").getFile(), "captures");
    private final String[] outFiles = {new String("professor.mpg"),
                                       new String("screen.mpg"),
                                       new String("microphone.mp2"),
                                       new String("capture.stopped")};
    private final long msecs = 10000;
    Properties props = null;
    
    @Before
        public void setup() {
        service = new CaptureAgentImpl();
        // TODO: Is it right to call this method to force the initialization as if the bundle was activated?
        service.activate(null);

        // Creates the Properties for the test
        props = new Properties();
        // TODO: Changing the test capture directory to one under resources directory
        props.setProperty(CaptureParameters.RECORDING_ID, "CaptureTest-" + System.currentTimeMillis());
        props.setProperty(CaptureParameters.RECORDING_ROOT_URL, outDir.getAbsolutePath());
        props.setProperty(CaptureParameters.CAPTURE_DEVICE_NAMES, "SCREEN,PRESENTER,AUDIO");
        props.setProperty("capture.device.PRESENTER.src", "/dev/video0");
        props.setProperty("capture.device.PRESENTER.outputfile", outFiles[0]);
        props.setProperty("capture.device.SCREEN.src", "/dev/video1");
        props.setProperty("capture.device.SCREEN.outputfile", outFiles[1]);
        props.setProperty("capture.device.AUDIO.src", "hw:0");
        props.setProperty("capture.device.AUDIO.outputfile", outFiles[2]);

        // Checks that output files don't exist
        for (String checkFile : outFiles) {
            File auxFile = new File(outDir, checkFile);
            if (auxFile.exists())
                auxFile.delete();
        }
    }
    
    @After
        public void teardown() {
        service = null;
    }
    
    @Test
        public void testCapture() {
        
        try {
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
            
            boolean result = service.stopCapture();
            
            // Checks correct return value
            Assert.assertTrue(result);
      
        } catch (UnsatisfiedLinkError e) {
            logger.error("Could not properly test capture agent: {}.", e.getMessage());
        }
    
        // Checks for the existence of the expected files, including "capture.stopped"
        for (String item : outFiles) {
            File auxFile = new File(outDir, item);
            Assert.assertTrue(auxFile.exists());
        }

        logger.info("Checked files exist");
       
    }
    
}



