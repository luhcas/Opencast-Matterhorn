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
package org.opencastproject.composer.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.opencastproject.composer.api.EncoderEngine;
import org.opencastproject.composer.impl.episode.EpisodeEncoderEngine;
import org.opencastproject.composer.impl.ffmpeg.FFmpegEncoderEngine;
import org.opencastproject.util.ConfigurationException;

import org.junit.Test;

/**
 * Test cases to test the creation of encoder engines based on the encoding profile.
 */
public class EncoderEngineFactoryTest {

  /**
   * Test method for {@link org.opencastproject.composer.impl.EncoderEngineFactoryImpl#newEngineByProfile(java.lang.String)}.
   * By giving a random (non existing) profile name, the default engine should be created.
   */
  @Test
  public void testNewEngineByProfile() {
    EncoderEngineFactory factory = EncoderEngineFactory.newInstance();
    EncoderEngine engine = null;
    try {
      engine = factory.newEngineByProfile("xyz");
      fail("Test to create encoder engine for unknown profile 'xyz' should have failed");
    } catch (ConfigurationException e) {
      // That's expected
    }
    engine = factory.newEngineByProfile("ui-cover.http");
    assertNotNull(engine);
    assertEquals(FFmpegEncoderEngine.class, engine.getClass());
  }

  /**
   * Test creation of an ffmpeg encoder.
   */
  @Test
  public void testNewEngineByFFmpegProfile() {
    EncoderEngineFactory factory = EncoderEngineFactory.newInstance();
    EncoderEngine ffmpegEncoderEngine = factory.newEngineByProfile("ffmpeg-format.generic");
    assertNotNull(ffmpegEncoderEngine);
    assertEquals(FFmpegEncoderEngine.class, ffmpegEncoderEngine.getClass());
  }
  
  /**
   * Test creation of a telestream episode encoder.
   */
  @Test
  public void testNewEngineByEpisodeProfile() {
    EncoderEngineFactory factory = EncoderEngineFactory.newInstance();
    EncoderEngine episodeEncoderEngine = factory.newEngineByProfile("episode-format.generic");
    assertNotNull(episodeEncoderEngine);
    assertEquals(EpisodeEncoderEngine.class, episodeEncoderEngine.getClass());
  }
  
  /**
   * Test method for {@link org.opencastproject.composer.impl.EncoderEngineFactory#newInstance()}.
   */
  @Test
  public void testNewInstance() {
    EncoderEngineFactory factory = EncoderEngineFactory.newInstance();
    assertNotNull(factory);
    assertEquals(EncoderEngineFactory.FACTORY_CLASS, factory.getClass().getCanonicalName());
  }

}