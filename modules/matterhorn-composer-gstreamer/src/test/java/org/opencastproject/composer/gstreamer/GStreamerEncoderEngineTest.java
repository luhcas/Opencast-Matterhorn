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
package org.opencastproject.composer.gstreamer;

import org.opencastproject.composer.api.EncoderEngine;
import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.composer.api.EncodingProfile;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Test class for GStreamer Encoding Engine.
 */
public class GStreamerEncoderEngineTest {

  /** Factory for creating GStreamerEncoderEngine */
  private static GStreamerFactory factory;
  /** First file used for testing */
  private static File testFile1;
  private File resultFile1;
  /** File used for multithread testing */
  private static File testFile2;
  private File resultFile2;

  @BeforeClass
  public static void setUp() throws Exception {
    // rapid initializing and deinitializing of Gstreamer (such as after each test) may lead to unexpected behavior and
    // second test would always fail with random error. That's why GStreamer is initialized and deinitialized only once
    // in whole suit.
    factory = new GStreamerFactory();
    factory.activate(null);
    testFile1 = new File(GStreamerEncoderEngineTest.class.getResource("/audio_1.mp3").toURI());
    testFile2 = new File(GStreamerEncoderEngineTest.class.getResource("/audio_2.mp3").toURI());
  }

  @Test
  public void testSingleThread() throws Exception {

    // create properties for this test
    Map<String, String> properties = new HashMap<String, String>();
    properties
            .put("gstreamer.pipeline",
                    "filesrc location=#{in.video.path} ! decodebin ! audioconvert ! audioresample ! lame bitrate=#{out.bitrate} ! filesink location=#{out.file.path}");
    properties.put("out.bitrate", "320");

    EncoderEngine engine = factory.newEncoderEngine(null);
    EncodingProfile profile = createEncodingProfile("SingleThreadTest", ".mp3", properties);
    resultFile1 = engine.encode(testFile1, profile, properties);

    Assert.assertTrue("File does not exist!", resultFile1.exists());
  }

  @Test
  public void testMultithreading() throws Exception {

    final Map<String, String> properties1 = new HashMap<String, String>();
    properties1
            .put("gstreamer.pipeline",
                    "filesrc location=#{in.video.path} ! decodebin ! audioconvert ! audioresample ! lame bitrate=#{out.bitrate} ! filesink location=#{out.file.path}");
    properties1.put("out.bitrate", "320");

    final Map<String, String> properties2 = new HashMap<String, String>(properties1);

    final AtomicBoolean error1 = new AtomicBoolean(false);
    final AtomicBoolean error2 = new AtomicBoolean(false);

    Runnable task1 = new Runnable() {
      @Override
      public void run() {
        EncoderEngine engine = factory.newEncoderEngine(null);
        EncodingProfile profile = createEncodingProfile("Thread1Test", ".mp3", properties1);
        try {
          resultFile1 = engine.encode(testFile1, profile, properties1);
        } catch (EncoderException e) {
          error1.set(true);
        }
      }
    };
    Runnable task2 = new Runnable() {
      @Override
      public void run() {
        EncoderEngine engine = factory.newEncoderEngine(null);
        EncodingProfile profile = createEncodingProfile("Thread2Test", ".mp3", properties2);
        try {
          resultFile2 = engine.encode(testFile2, profile, properties2);
        } catch (EncoderException e) {
          error2.set(true);
        }
      }
    };
    Thread th1 = new Thread(task1);
    Thread th2 = new Thread(task2);
    th1.start();
    th2.start();
    th1.join();
    th2.join();

    Assert.assertTrue("Error in first processing pipeline", !error1.get());
    Assert.assertTrue("First file does not exist!", resultFile1 != null && resultFile1.exists());
    Assert.assertTrue("Error in second processing pipeline", !error2.get());
    Assert.assertTrue("Second file does not exist!", resultFile2 != null && resultFile2.exists());
  }

  @After
  public void tearDown() throws Exception {
    if (resultFile1 != null && resultFile1.exists()) {
      Assert.assertTrue("Could not delete first file!", resultFile1.delete());
      resultFile1 = null;
    }
    if (resultFile2 != null && resultFile2.exists()) {
      Assert.assertTrue("Could not delete second file!", resultFile2.delete());
      resultFile2 = null;
    }
  }

  @AfterClass
  public static void destroy() throws Exception {
    factory.deactivate(null);
  }

  /**
   * Creates EncodingProfile.
   * @param name
   * @param suffix
   * @param properties
   * @return
   */
  private EncodingProfile createEncodingProfile(final String name, final String suffix,
          final Map<String, String> properties) {
    EncodingProfile profile = new EncodingProfile() {
      @Override
      public boolean isApplicableTo(MediaType type) {
        return false;
      }

      @Override
      public boolean hasExtensions() {
        return false;
      }

      @Override
      public String getSuffix() {
        return suffix;
      }

      @Override
      public Object getSource() {
        return null;
      }

      @Override
      public MediaType getOutputType() {
        return null;
      }

      @Override
      public String getName() {
        return name;
      }

      @Override
      public String getMimeType() {
        return null;
      }

      @Override
      public String getIdentifier() {
        return name;
      }

      @Override
      public Map<String, String> getExtensions() {
        return properties;
      }

      @Override
      public String getExtension(String key) {
        return properties.get(key);
      }

      @Override
      public MediaType getApplicableMediaType() {
        return null;
      }
    };
    return profile;
  }
}
