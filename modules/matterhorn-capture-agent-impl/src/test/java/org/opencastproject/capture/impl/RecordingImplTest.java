package org.opencastproject.capture.impl;

import org.opencastproject.capture.api.CaptureParameters;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.util.ConfigurationException;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Pattern;

public class RecordingImplTest {
  RecordingImpl rec = null;
  ConfigurationManager configManager = null;

  File testDir = null;

  @Before
  public void setup() throws org.osgi.service.cm.ConfigurationException, IOException {
    //Setup the configuration manager
    configManager = new ConfigurationManager();
    Properties sourceProps = new Properties();
    InputStream is = getClass().getClassLoader().getResourceAsStream("config/capture.properties");
    if (is == null) {
      Assert.fail();
    }
    sourceProps.load(is);
    IOUtils.closeQuietly(is);

    testDir = new File(System.getProperty("java.io.tmpdir"), "recording-test");
    configManager.setItem("org.opencastproject.storage.dir", testDir.getAbsolutePath());
    configManager.setItem("org.opencastproject.server.url", "http://localhost:8080");
    configManager.updated(sourceProps);
  }

  @After
  public void teardown() {
    rec = null;
    FileUtils.deleteQuietly(testDir);
  }

  @Test
  public void testEdgeRecording() throws IllegalArgumentException, ConfigurationException, IOException, MediaPackageException {
    //Let's test some edge-casey recordings
    rec = new RecordingImpl(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew(), null);
    Assert.assertNotNull(rec);
    Assert.assertEquals(System.getProperty("java.io.tmpdir"), rec.getProperty(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL));
    Assert.assertNull(rec.setProperty("test", "foo"));
    Assert.assertEquals("foo", rec.setProperty("test", "bar"));
    FileUtils.deleteQuietly(rec.getBaseDir());
  }

  @Test
  public void testUnscheduledRecording() throws IllegalArgumentException, ConfigurationException, IOException, MediaPackageException {
    //Create the recording for an unscheduled capture
    rec = new RecordingImpl(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew(), configManager.getAllProperties());
    Assert.assertNotNull(rec);
    Assert.assertEquals(configManager.getItem(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL), rec.getProperty(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL));
    Assert.assertTrue(Pattern.matches("Unscheduled-demo_capture_agent-\\d+", rec.getID()));
    Assert.assertTrue(Pattern.matches("Unscheduled-demo_capture_agent-\\d+", rec.getProperty(CaptureParameters.RECORDING_ID)));
    Assert.assertTrue(Pattern.matches("Unscheduled-demo_capture_agent-\\d+", rec.getProperties().getProperty(CaptureParameters.RECORDING_ID)));
  }

  @Test
  public void testScheduledCaptureWithRecordingID() throws IllegalArgumentException, ConfigurationException, IOException, MediaPackageException, org.osgi.service.cm.ConfigurationException {
    configManager.setItem(CaptureParameters.RECORDING_ID, "MyTestRecording");

    //Create the recording for a scheduled capture which only has its recording id set
    rec = new RecordingImpl(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew(), configManager.getAllProperties());
    Assert.assertNotNull(rec);
    Assert.assertEquals(configManager.getItem(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL), rec.getProperty(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL));
    Assert.assertEquals("MyTestRecording", rec.getID());
    Assert.assertEquals("MyTestRecording", rec.getProperty(CaptureParameters.RECORDING_ID));
    Assert.assertEquals("MyTestRecording", rec.getProperties().getProperty(CaptureParameters.RECORDING_ID));
    configManager.setItem(CaptureParameters.RECORDING_ID, null);
  }

  @Test
  public void testScheduledCaptureWithRootURL() throws IllegalArgumentException, ConfigurationException, IOException, MediaPackageException {
    File baseDir = new File(configManager.getItem(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL));
    configManager.setItem(CaptureParameters.RECORDING_ROOT_URL, new File(baseDir, "MyTestRecording").getAbsolutePath());

    //Create the recording for a scheduled capture which only has its root url set
    rec = new RecordingImpl(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew(), configManager.getAllProperties());
    Assert.assertNotNull(rec);
    Assert.assertEquals(new File(baseDir, "MyTestRecording"), rec.getBaseDir());
    Assert.assertEquals("MyTestRecording", rec.getID());
    Assert.assertEquals("MyTestRecording", rec.getProperty(CaptureParameters.RECORDING_ID));
    Assert.assertEquals("MyTestRecording", rec.getProperties().getProperty(CaptureParameters.RECORDING_ID));
    configManager.setItem(CaptureParameters.RECORDING_ROOT_URL, null);
  }

  @Test
  public void testFullySpecCapture() throws IllegalArgumentException, ConfigurationException, IOException, MediaPackageException {
    File baseDir = new File(configManager.getItem(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL));
    String recordingID = "MyTestRecording";
    configManager.setItem(CaptureParameters.RECORDING_ID, recordingID);
    configManager.setItem(CaptureParameters.RECORDING_ROOT_URL, new File(baseDir, recordingID).getAbsolutePath());

    //Create the recording for a scheduled capture which only has its root url set
    rec = new RecordingImpl(MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew(), configManager.getAllProperties());
    Assert.assertNotNull(rec);
    Assert.assertEquals(new File(baseDir, recordingID), rec.getBaseDir());
    Assert.assertEquals(recordingID, rec.getID());
    Assert.assertEquals(recordingID, rec.getProperty(CaptureParameters.RECORDING_ID));
    Assert.assertEquals(recordingID, rec.getProperties().getProperty(CaptureParameters.RECORDING_ID));
    configManager.setItem(CaptureParameters.RECORDING_ID, null);
    configManager.setItem(CaptureParameters.RECORDING_ROOT_URL, null);
  }
}

