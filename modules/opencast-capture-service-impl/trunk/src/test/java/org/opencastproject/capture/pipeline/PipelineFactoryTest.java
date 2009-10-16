package org.opencastproject.capture.pipeline;

import java.util.Properties;
import org.gstreamer.*;
import org.gstreamer.event.EOSEvent;

import org.junit.Test;

/**
 * TODO: Clarify how gstreamer testing should be done.
 */
public class PipelineFactoryTest {
  @Test
  public void stub() {
  }
  /*
   * public static void main(String[] args) { if (args.length != 1) {
   * System.out.println("Usage: java PipelineFactoryTest [timeout]"); System.exit(1); } int duration =
   * Integer.parseInt(args[0]); Properties props = new Properties(); props.setProperty("/dev/video0",
   * "/home/kta719/gst-testing/professor.mpg"); props.setProperty("/dev/video1", "/home/kta719/gst-testing/screen.mpg");
   * props.setProperty("hw:0", "/home/kta719/gst-testing/microphone.mp2");
   * 
   * final Pipeline pipe = PipelineFactory.create(props); Bus bus = pipe.getBus(); bus.connect(new Bus.EOS() {
   * 
   * @Override public void endOfStream(GstObject arg0) { System.out.println(pipe.getName() + " received EOS.");
   * pipe.setState(State.NULL); Gst.quit(); }
   * 
   * }); bus.connect(new Bus.ERROR() {
   * 
   * @Override public void errorMessage(GstObject arg0, int arg1, String arg2) { System.err.println(arg0.getName() +
   * ": " + arg2); Gst.quit(); }
   * 
   * });
   * 
   * pipe.play(); while (pipe.getState() != State.PLAYING); System.out.println(pipe.getName() + " started."); new
   * Thread(new SendEOS(duration*1000, pipe)).start(); Gst.main(); Gst.deinit(); }
   */
}

class SendEOS implements Runnable {

  private long timeout;
  private Pipeline pipeline;

  @Override
  public void run() {
    try {
      Thread.sleep(timeout);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    pipeline.sendEvent(new EOSEvent());
  }

  public SendEOS(long milli, Pipeline p) {
    timeout = milli;
    pipeline = p;
  }

}
