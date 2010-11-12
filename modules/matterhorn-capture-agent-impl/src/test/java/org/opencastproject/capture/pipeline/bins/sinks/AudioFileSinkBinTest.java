package org.opencastproject.capture.pipeline.bins.sinks;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Gst;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.opencastproject.capture.pipeline.SourceDeviceName;
import org.opencastproject.capture.pipeline.bins.BinTestHelpers;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.CaptureDeviceBinTest;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Ignore
public class AudioFileSinkBinTest {

  /** Capture Device Properties created for unit testing **/
  CaptureDevice captureDevice = null;
  
  /** True to run the tests */
  private static boolean gstreamerInstalled = true;
  
  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(CaptureDeviceBinTest.class);


  private String bitrateDefault;

  @BeforeClass
  public static void testGst() {
    try {
      Gst.init();
    } catch (Throwable t) {
      logger.warn("Skipping agent tests due to unsatisifed gstreamer installation");
      gstreamerInstalled = false;
    }
  }
  
  @AfterClass
  public static void tearDownGst() {
    if (gstreamerInstalled) {
      //Gst.deinit();
    }
  }
  
  @Before
  public void setup() throws ConfigurationException, IOException, URISyntaxException {
    if (!gstreamerInstalled)
      return;
    Element encoder = ElementFactory.make(AudioFileSinkBin.DEFAULT_ENCODER, null);
    bitrateDefault = encoder.getPropertyDefaultValue("bitrate").toString();
  }

  @After
  public void tearDown() {
    if (!gstreamerInstalled)
      return;
    captureDevice = null;
  }
  
  /** Salient encoder properties are codec and bitrate **/
  /** Salient muxer properties are codec and container **/
  private Properties createProperties(String codec, String bitrate, String container){
    Properties captureDeviceProperties = BinTestHelpers.createCaptureDeviceProperties(null, codec, bitrate, null, container, null, null, null, null);
    return captureDeviceProperties;
  }

  private void checkEncoderProperties(SinkBin sinkBin, String codec, String bitrate) {
    Assert.assertTrue(sinkBin.encoder.getName().contains(codec));
    Assert.assertEquals(bitrate, sinkBin.encoder.get("bitrate").toString());
  }
  
  private void checkMuxerProperties(SinkBin sinkBin, String muxer) {
    Assert.assertTrue("The muxer name " + sinkBin.muxer.getName() + " should match the muxer type " + muxer, sinkBin.muxer.getName().contains(muxer));
  }
  
  @Test
  public void nullSettingsForCodecBitrateAndContainerCreatesElementsWithDefaults() {
    if (!gstreamerInstalled) return;
    Properties captureDeviceProperties = createProperties(null, null, null);
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", SourceDeviceName.VIDEOTESTSRC, "Friendly Name", "/tmp/testpipe/test.mp2", captureDeviceProperties);
    AudioFileSinkBin audioFileSinkBin = createAudioFileSinkBinDontWantException(captureDeviceProperties);
    checkEncoderProperties(audioFileSinkBin, AudioFileSinkBin.DEFAULT_ENCODER, bitrateDefault); 
    checkMuxerProperties(audioFileSinkBin, AudioFileSinkBin.DEFAULT_MUXER);
  }
  
  @Test
  public void settingBitrateChangesCodecBitrate() {
    if (!gstreamerInstalled) return;
    Properties captureDeviceProperties = createProperties(null, "320", null);
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", SourceDeviceName.VIDEOTESTSRC, "Friendly Name", "/tmp/testpipe/test.mp2", captureDeviceProperties);
    AudioFileSinkBin audioFileSinkBin = createAudioFileSinkBinDontWantException(captureDeviceProperties);
    checkEncoderProperties(audioFileSinkBin, AudioFileSinkBin.DEFAULT_ENCODER, "320"); 
  }
  
  @Test
  public void settingCodecButNotContainerResultsInCorrectCodecAndDefaultMuxer() {
    if (!gstreamerInstalled) return;
    Properties captureDeviceProperties = createProperties("vorbisenc", null, null);
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", SourceDeviceName.VIDEOTESTSRC, "Friendly Name", "/tmp/testpipe/test.mp2", captureDeviceProperties);
    AudioFileSinkBin audioFileSinkBin = createAudioFileSinkBinDontWantException(captureDeviceProperties);
    checkEncoderProperties(audioFileSinkBin, "vorbisenc", "-1"); 
    checkMuxerProperties(audioFileSinkBin, AudioFileSinkBin.DEFAULT_MUXER);
  }
  
  @Test
  public void settingCodecToFAACButNotContainerResultsInCorrectCodecAndCorrectMuxer() {
    if (!gstreamerInstalled) return;
    Properties captureDeviceProperties = createProperties("faac", null, null);
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", SourceDeviceName.VIDEOTESTSRC, "Friendly Name", "/tmp/testpipe/test.mp2", captureDeviceProperties);
    AudioFileSinkBin audioFileSinkBin = createAudioFileSinkBinDontWantException(captureDeviceProperties);
    checkEncoderProperties(audioFileSinkBin, "faac", "128000"); 
    checkMuxerProperties(audioFileSinkBin, "mp4mux");
  }
  
  @Test
  public void settingContainerResultsInCorrectMuxer() {
    if (!gstreamerInstalled) return;
    Properties captureDeviceProperties = createProperties(null, null, "mpegtsmux");
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", SourceDeviceName.VIDEOTESTSRC, "Friendly Name", "/tmp/testpipe/test.mp2", captureDeviceProperties);
    AudioFileSinkBin audioFileSinkBin = createAudioFileSinkBinDontWantException(captureDeviceProperties);
    checkEncoderProperties(audioFileSinkBin, AudioFileSinkBin.DEFAULT_ENCODER, bitrateDefault); 
    checkMuxerProperties(audioFileSinkBin, "mpegtsmux");
  }
  
  
  
  /** Testing permutations of possible file locations for tests **/
  @Test
  public void realFileLocationExampleSetsPropertyAndDoesntThrowException(){
    if (!gstreamerInstalled) return;
    Properties captureDeviceProperties = createProperties(null, null, null);
    String location = "/tmp/testpipe/test.mp2";
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", SourceDeviceName.VIDEOTESTSRC, "Friendly Name", location, captureDeviceProperties);
    AudioFileSinkBin sinkBin = createAudioFileSinkBinDontWantException(captureDeviceProperties);
    Assert.assertEquals(location, sinkBin.filesink.get("location"));
  }
  
  @Test
  public void emptyFileLocationShouldThrowAnException(){
    if (!gstreamerInstalled) return;
    Properties captureDeviceProperties = createProperties(null, null, null);
    String location = "";
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", SourceDeviceName.VIDEOTESTSRC, "Friendly Name", location, captureDeviceProperties);
    @SuppressWarnings("unused")
    AudioFileSinkBin sinkBin = createSinkBinWantException(captureDeviceProperties);
  }
  
  @Test
  public void nullFileLocationShouldThrowAnException(){
    if (!gstreamerInstalled) return;
    Properties captureDeviceProperties = createProperties(null, null, null);
    String location = null;
    captureDevice = BinTestHelpers.createCaptureDevice("/dev/video0", SourceDeviceName.VIDEOTESTSRC, "Friendly Name", location, captureDeviceProperties);
    @SuppressWarnings("unused")
    AudioFileSinkBin sinkBin = createSinkBinWantException(captureDeviceProperties);
  }
  
  

  private AudioFileSinkBin createAudioFileSinkBinDontWantException(Properties captureDeviceProperties) {
    AudioFileSinkBin audioFileSinkBin = null;
    try {
      audioFileSinkBin = createAudioFileSinkBin(captureDeviceProperties);
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail();
    }
    return audioFileSinkBin;
  }
  
  private AudioFileSinkBin createSinkBinWantException(Properties captureDeviceProperties) {
    AudioFileSinkBin audioFileSinkBin = null;
    try {
      audioFileSinkBin = createAudioFileSinkBin(captureDeviceProperties);
      Assert.fail();
    } catch (Exception e) {
      
    }
    return audioFileSinkBin;
  }

  private AudioFileSinkBin createAudioFileSinkBin(Properties captureDeviceProperties) throws Exception {
    AudioFileSinkBin audioFileSinkBin;
    audioFileSinkBin = new AudioFileSinkBin(captureDevice, captureDeviceProperties);
    return audioFileSinkBin;
  }
}
