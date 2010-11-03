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
package org.opencastproject.capture.impl.jobs;

import org.opencastproject.capture.admin.api.RecordingState;
import org.opencastproject.capture.api.AgentRecording;
import org.opencastproject.capture.api.CaptureParameters;
import org.opencastproject.capture.impl.RecordingImpl;
import org.opencastproject.util.XProperties;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Vector;


/**
 * Tests for the CleanCaptureJob class
 */
public class CleanCaptureJobTest {

  private static final Logger logger = LoggerFactory.getLogger(CleanCaptureJobTest.class);

  XProperties props = null;
  CleanCaptureJob theJob = null;
  Vector<AgentRecording> theRecordings = null;

  int numberOfRecordings = 5;

  File baseDir; 

  @Before
  public void setUp() throws URISyntaxException {
    // Define particular instances for the CleanCaptureJob required arguments
    props = new XProperties();
    props.setProperty(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL, new File(System.getProperty("java.io.tmpdir"), "clean-capture-test").getAbsolutePath());
    theJob = new CleanCaptureJob();
    theRecordings = new Vector<AgentRecording>();

    baseDir = new File(getClass().getClassLoader().getResource(".").toURI().getPath(), "cleanTest");
    baseDir.mkdir();
    if (!baseDir.exists()) {
      Assert.fail();
    }

    try {
      RecordingImpl rec = new RecordingImpl(null, props);
      rec.setState(RecordingState.UPLOAD_FINISHED);
      theRecordings.add(rec);

      rec = new RecordingImpl(null, props);
      rec.setState(RecordingState.UPLOAD_FINISHED);
      theRecordings.add(rec);
      
      rec = new RecordingImpl(null, props);
      rec.setState(RecordingState.CAPTURING);
      theRecordings.add(rec);

      rec = new RecordingImpl(null, props);
      rec.setState(RecordingState.MANIFEST);
      theRecordings.add(rec);

      rec = new RecordingImpl(null, props);
      rec.setState(RecordingState.UPLOADING);
      theRecordings.add(rec);
    } catch (IOException e) {
      logger.error("Unexpected I/O Exception when creating test recordings: {}", e.getMessage());
      return;
    } 
  }

  @After
  public void tearDown() {
    FileUtils.deleteQuietly(new File(props.getProperty(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL)));
    props = null;
    theRecordings = null;
  }

  /**
   * The minimum space allowed in disk is set to zero and the maximum archival time is zero
   */
  @Test
  public void diskZeroArchivalZero() {
    // Insert properties accordingly for this test
    props.setProperty(CaptureParameters.CAPTURE_CLEANER_MAX_ARCHIVAL_DAYS, "0");
    props.setProperty(CaptureParameters.CAPTURE_CLEANER_MIN_DISK_SPACE, "0");

    // Should clean the recordings in the indexes 0, 1 and 2
    theJob.doCleaning(props,theRecordings);

    // Check the cleaning was OK
    for (int i = 0; i < numberOfRecordings; i++) 
      if (i < numberOfRecordings/2) {
        Assert.assertFalse("Recording " + i + " exists when it should not.", theRecordings.get(i).getBaseDir().exists());
      }
      else {
        Assert.assertTrue(theRecordings.get(i).getBaseDir().exists());
      }

  }

  /**
   * The minimum free space in disk is zero and the maximum archival time is "infinity"
   */
  @Test
  public void diskZeroArchivalLots() {
    // Insert properties for this test
    props.setProperty(CaptureParameters.CAPTURE_CLEANER_MAX_ARCHIVAL_DAYS, String.valueOf(Long.MAX_VALUE));
    props.setProperty(CaptureParameters.CAPTURE_CLEANER_MIN_DISK_SPACE, "0");

    // Should leave all the recordings untouched
    theJob.doCleaning(props, theRecordings);

    //Check the cleaning was OK
    for (AgentRecording aRec : theRecordings)
      Assert.assertTrue(aRec.getBaseDir().exists());
  }

  /**
   * The minimum free space in disk is a lot and the maximum archival time is "infinity". All recordings have been ingested
   */
  @Test
  public void diskLotsArchivalLotsAllIngested() {
    // Insert properties for this test
    props.setProperty(CaptureParameters.CAPTURE_CLEANER_MAX_ARCHIVAL_DAYS, String.valueOf(Long.MAX_VALUE));
    props.setProperty(CaptureParameters.CAPTURE_CLEANER_MIN_DISK_SPACE, String.valueOf(Long.MAX_VALUE));
    // Create a capture.ingested file for the recordings that don't have it
    for (AgentRecording aRec : theRecordings) {
      aRec.setState(RecordingState.UPLOAD_FINISHED);
    }       
    
    // Should clean all the recordings *because of the disk space*
    theJob.doCleaning(props,theRecordings);

    // Check the cleaning was OK
    for (AgentRecording aRec : theRecordings)
      Assert.assertFalse(aRec.getBaseDir().exists());
  }

  /**
   * The minimum free space in disk is a lot and the maximum archival time is "infinity". There is non-ingested recordings
   */
  @Test
  public void diskLotsArchivalLots() {
    // Insert properties for this test
    props.setProperty(CaptureParameters.CAPTURE_CLEANER_MAX_ARCHIVAL_DAYS, String.valueOf(Long.MAX_VALUE));
    props.setProperty(CaptureParameters.CAPTURE_CLEANER_MIN_DISK_SPACE, String.valueOf(Long.MAX_VALUE));
        
    // Should clean all the recordings *because of the disk space*
    theJob.doCleaning(props,theRecordings);

    // Check the cleaning was OK
    for (int i = 0; i < numberOfRecordings; i++) 
      if (i < numberOfRecordings/2)
        Assert.assertFalse(theRecordings.get(i).getBaseDir().exists());
      else
        Assert.assertTrue(theRecordings.get(i).getBaseDir().exists());
  }
}
