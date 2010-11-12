package org.opencastproject.capture.pipeline.bins.sources;

import java.util.Properties;

import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Pad;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;

public class AudioTestSrcBin extends AudioSrcBin {

  Element audiotestsrc;
  
  /** Used to create an audio test src great for testing the capture agent without needing any devices
   * installed but still gives that authentic capturing experience. 
   * @param captureDevice - The details for this capture device.
   * @param properties - The confidence monitoring details for this device. 
   * @throws Exception - If anything fails to link, in this case nothing, it throws an exception with the details.
   */
  public AudioTestSrcBin(CaptureDevice captureDevice, Properties properties) throws Exception {
    super(captureDevice, properties);
  }

  /** Create an audiotestsrc Element (see gst-inspect for details) **/
  @Override
  protected void createElements(){
    audiotestsrc = ElementFactory.make("audiotestsrc", null);
  }
  
  /** Add the audiotestsrc to the bin we will return as a source. **/
  @Override
  protected void addElementsToBin() {
    bin.add(audiotestsrc);
  }

  /** No element properties to set, you could set this to a different test 
   * source if you don't like the white noise. **/
  @Override
  protected void setElementProperties() {
    
  }

  /** The sink for this source is the audiotestsrc itself that will be used to
   * create the ghost pads for this bin. **/
  @Override
  public Pad getSrcPad() {
    return audiotestsrc.getStaticPad("src");
  }

  /** Since we only have one Element in this Bin we don't need to link anything. **/
  @Override
  protected void linkElements() throws Exception {
    
  }
  
  /** This isn't a video test source so always return false **/
  @Override
  public boolean isVideoDevice(){
    return false;
  }
  
  /** This is an audio test source so return true on being a audio source **/
  @Override
  public boolean isAudioDevice(){
    return true;
  }
}