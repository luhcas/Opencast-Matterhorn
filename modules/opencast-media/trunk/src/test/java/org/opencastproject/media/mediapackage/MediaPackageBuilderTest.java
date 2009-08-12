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

package org.opencastproject.media.mediapackage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.opencastproject.media.mediapackage.handle.HandleException;
import org.opencastproject.util.FileSupport;

import org.junit.Test;

import java.io.File;
import java.io.IOException;

/**
 * Test case used to make sure the media package builder works as expected.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id: MediaPackageBuilderTest.java 2905 2009-07-15 16:16:05Z ced $
 */
public class MediaPackageBuilderTest extends AbstractMediaPackageTest {

  /**
   * Test method for
   * {@link org.opencastproject.media.mediapackage.MediaPackageBuilderImpl#createNew(org.opencastproject.media.mediapackage.handle.Handle)}
   * .
   */
  @Test
  public void testCreateNew() {
    MediaPackage mediaPackage = null;
    try {
      mediaPackage = mediaPackageBuilder.createNew(identifier);
      assertEquals(identifier, mediaPackage.getIdentifier());
    } catch (MediaPackageException e) {
      fail("Error creating new media package: " + e.getMessage());
    } finally {
      FileSupport.delete(mediaPackage.getRoot().getParentFile(), true);
    }
  }

  /**
   * Test method for {@link org.opencastproject.media.mediapackage.MediaPackageBuilderImpl#loadFromManifest(File)}.
   */
  @Test
  public void testLoadFromManifest() {
    try {
      MediaPackage mediaPackage = mediaPackageBuilder.loadFromManifest(manifestFile);
      assertEquals(mediaPackage.getCatalogs().length, 3);
      assertNotNull(mediaPackage.getCatalogs(MediaPackageElements.DUBLINCORE_CATALOG));
    } catch (MediaPackageException e) {
      fail("Media package excpetion while reading media package from manifest: " + e.getMessage());
    } catch (ConfigurationException e) {
      fail("Configuration exception while reading media package from manifest: " + e.getMessage());
    }
  }

  /**
   * Test method for
   * {@link org.opencastproject.media.mediapackage.MediaPackageBuilderImpl#loadFromDirectory(java.io.File)}.
   */
  @Test
  public void testLoadFromDirectory() {
    try {
      MediaPackage mediaPackage = mediaPackageBuilder.loadFromDirectory(packageDir);
      assertEquals(mediaPackage.getCatalogs().length, 3);
      assertNotNull(mediaPackage.getCatalogs(MediaPackageElements.DUBLINCORE_CATALOG));
    } catch (MediaPackageException e) {
      fail("Media package excpetion while reading media package from directory: " + e.getMessage());
    } catch (ConfigurationException e) {
      fail("Configuration exception while reading media package from directory: " + e.getMessage());
    }
  }

  /**
   * Test method for
   * {@link org.opencastproject.media.mediapackage.MediaPackageBuilderImpl#createFromElements(java.io.File,boolean)}.
   * 
   * @throws IOException
   */
  @Test
  public void testCreateFromElements() throws IOException {
    try {
      MediaPackage mediaPackage = mediaPackageBuilder.createFromElements(packageDir, false);
      assertEquals(mediaPackage.getCatalogs().length, 3);
      assertNotNull(mediaPackage.getCatalogs(MediaPackageElements.DUBLINCORE_CATALOG));
      assertEquals(mediaPackage.getCatalogs(MediaPackageElements.DUBLINCORE_CATALOG).length, 2);
    } catch (MediaPackageException e) {
      fail("Media package exception while reading media package from elements: " + e.getMessage());
    } catch (UnsupportedElementException e) {
      fail("Unsupported media package element found while reading media package from elements: " + e.getMessage());
    }

    // Test unknown elements
    File showstopper = new File(packageDir, "showstopper.zzz");
    showstopper.createNewFile();
    try {
      try {
        mediaPackageBuilder.createFromElements(packageDir, false);
        fail("Media package builder ignored an unknown file where it shouldn't");
      } catch (MediaPackageException e) {
        fail("Media package exception while reading media package from elements: " + e.getMessage());
      } catch (UnsupportedElementException e) {
        // This was expected
      }
      try {
        mediaPackageBuilder.createFromElements(packageDir, true);
      } catch (MediaPackageException e) {
        fail("Media package exception while reading media package from elements: " + e.getMessage());
      } catch (UnsupportedElementException e) {
        fail("A UnsupportedMediaPackageElementException exception was raised although ignoreUnknown was true");
      }
    } finally {
      showstopper.delete();
    }

    // Try again without manifest
    manifestFile.delete();
    try {
      MediaPackage mediaPackage = mediaPackageBuilder.createFromElements(packageDir, false);
      assertEquals(mediaPackage.getCatalogs().length, 3);
      assertNotNull(mediaPackage.getCatalogs(MediaPackageElements.DUBLINCORE_CATALOG));
    } catch (MediaPackageException e) {
      fail("Media package exception while reading media package from elements: " + e.getMessage());
    } catch (UnsupportedElementException e) {
      fail("Unsupported media package element found while reading media package from elements: " + e.getMessage());
    }

  }

  @Test
  public void testMediaAnalyzer() {
    String audioVideoPath = MediaPackageBuilderTest.class.getResource("/av.mov").getPath();
    String videoOnlyPath = MediaPackageBuilderTest.class.getResource("/vonly.mov").getPath();
    String audioOnlyPath = MediaPackageBuilderTest.class.getResource("/aonly.mov").getPath();
    MediaPackage mediaPackage = null;
    MediaPackageElement audiovisualElement = null;
    MediaPackageElement videoElement = null;
    MediaPackageElement audioElement = null;
    try {
      try {
        mediaPackage = mediaPackageBuilder.createNew(handleBuilder.createNew());
        audiovisualElement = mediaPackage.add(new File(audioVideoPath), false);
        videoElement = mediaPackage.add(new File(videoOnlyPath), false);
        audioElement = mediaPackage.add(new File(audioOnlyPath), false);
      } catch (MediaPackageException e) {
        fail("Media package exception while reading media package from elements: " + e.getMessage());
      } catch (UnsupportedElementException e) {
        fail("Unsupported media package element found while reading media package from elements: " + e.getMessage());
      } catch (HandleException e) {
        fail("Error creating handle: " + e.getMessage());
      }

      // Test element types
      // todo
      // if (!(audiovisualElement instanceof AudioVisualTrack))
      // fail("Audiovisual track has not been recognized by the media package builder");
      // AudioVisualTrack audiovisualTrack = (AudioVisualTrack)audiovisualElement;
      //
      // if (!(videoElement instanceof VideoTrack))
      // fail("Video track has not been recognized by the media package builder");
      // VideoTrack videoTrack = (VideoTrack)videoElement;
      //
      // if (!(audioElement instanceof AudioTrack))
      // fail("Audio track has not been recognized by the media package builder");
      // AudioTrack audioTrack = (AudioTrack)audioElement;

      // Test track durations
      // Todo media package is not requested to take tracks with equal length for now so this test is disabled.
      // Todo See ManifestImpl.add(MediaPackageElement)
      // assertEquals(audiovisualTrack.getDuration(), mediaPackage.getDuration());
      // assertEquals(videoTrack.getDuration(), mediaPackage.getDuration());
      // assertEquals(audioTrack.getDuration(), mediaPackage.getDuration());

      // Test adding of track with different duration

      // Test Audio/Video settings
      /*
       * todo assertTrue(audiovisualTrack.hasAudio() && audiovisualTrack.hasVideo()); assertTrue(!videoTrack.hasAudio()
       * && videoTrack.hasVideo()); assertTrue(audioTrack.hasAudio() && !audioTrack.hasVideo());
       */

    } finally {
      if (mediaPackage != null)
        FileSupport.delete(mediaPackage.getRoot());
    }
  }

  /**
   * Test method for
   * {@link org.opencastproject.media.mediapackage.MediaPackageBuilderImpl#loadFromPackage(org.opencastproject.media.mediapackage.MediaPackagePackager, java.io.InputStream)}
   * .
   */
  @Test
  public void testLoadFromPackage() {
    System.out.println("Not yet implemented");
  }

}