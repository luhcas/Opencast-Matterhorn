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

import org.opencastproject.util.FileSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Utility class used for media package handling.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id: MediaPackageSupport.java 1883 2009-01-21 17:54:57Z wunden $
 */
public class MediaPackageSupport {

  /**
   * Mode used when merging media packages.
   * <p>
   * <ul>
   * <li><code>Merge</code> assigns a new identifier in case of conflicts</li>
   * <li><code>Replace</code> replaces elements in the target media package with matching identifier</li>
   * <li><code>Skip</code> skips elements from the source media package with matching identifer</li>
   * <li><code>Fail</code> fail in case of conflicting identifier</li>
   * </ul>
   * 
   * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
   */
  public enum MergeMode {
    Merge, Replace, Skip, Fail
  };

  /** the logging facility provided by log4j */
  private final static Logger log_ = LoggerFactory.getLogger(MediaPackageSupport.class.getName());

  /**
   * Returns <code>true</code> if a lockfile exists in the directory <code>dir</code>.
   * 
   * @param dir
   *          the media package directory
   */
  public static boolean isLocked(File dir) {
    File lock = new File(dir, MediaPackage.LOCKFILE);
    return lock.exists();
  }

  /**
   * Locks the media package located in <code>dir</code> to make sure no other ingest process will try to process this
   * media package. A <code>MediaPackageException</code> will be thrown if the media package is already locked.
   * 
   * @param dir
   *          the media package directory
   * @throws MediaPackageException
   *           if the lock already exists or cannot be created
   */
  public static boolean lockMediaPackage(File dir) throws MediaPackageException {
    return lockMediaPackage(dir, false);
  }

  /**
   * Locks the media package located in <code>dir</code> to make sure no other ingest process will try to process this
   * media package.
   * <p>
   * If <code>force</code> is <code>true</code>, this method will not throw and exception if the lock alreday exists.
   * </p>
   * 
   * @param dir
   *          the media package directory
   * @param force
   *          <code>true</code> to relock an already locked media package
   * @throws MediaPackageException
   *           if the lock already exists or cannot be created
   */
  public static boolean lockMediaPackage(File dir, boolean force) throws MediaPackageException {
    File lock = new File(dir, MediaPackage.LOCKFILE);
    if (lock.exists()) {
      if (force)
        log_.debug("Overwriting existing media package lock");
      else {
        log_.debug("Media package at " + dir + " is already locked");
        return false;
      }
    }
    try {
      lock.createNewFile();
    } catch (IOException e) {
      throw new MediaPackageException("Error creating media package lock at " + dir + ": " + e.getMessage());
    }
    return true;
  }

  /**
   * Removes the lockfile from the media package directory.
   * 
   * @param dir
   *          the media package directory
   * @throws MediaPackageException
   *           if the lock file cannot be removed
   */
  public static void unlockMediaPackage(File dir) throws MediaPackageException {
    File lock = new File(dir, MediaPackage.LOCKFILE);
    if (!lock.exists())
      return;
    try {
      lock.delete();
    } catch (Exception e) {
      throw new MediaPackageException("Error removing media package lock at " + dir + ": " + e.getMessage(), e);
    }
  }

  /**
   * Merges the contents of media package located at <code>sourceDir</code> into the media package located at
   * <code>targetDir</code>.
   * <p>
   * When choosing to move the media package element into the new place instead of copying them, the souce media package
   * folder will be removed afterwards.
   * </p>
   * 
   * @param targetDir
   *          the target media package directory
   * @param sourceDir
   *          the source media package directory
   * @param move
   *          <code>true</code> to move the media package contents, <code>false</code> to make a copy
   * @param mode
   *          conflict resolution strategy in case of identical element identifier
   * @throws MediaPackageException
   *           if an error occurs either accessing one of the two media packages or merging them
   */
  public static MediaPackage merge(File targetDir, File sourceDir, boolean move, MergeMode mode)
          throws MediaPackageException {
    MediaPackage src = null;
    MediaPackage dest = null;
    boolean srcIsLocked = false;
    boolean targetIsLocked = false;

    // Get hold of a media package builder
    MediaPackageBuilderFactory builderFactory = MediaPackageBuilderFactory.newInstance();
    MediaPackageBuilder builder = builderFactory.newMediaPackageBuilder();

    try {
      // Load source media package
      try {
        src = builder.loadFromDirectory(sourceDir);
        srcIsLocked = isLocked(sourceDir);
        lockMediaPackage(targetDir, true);
      } catch (Throwable e) {
        throw new MediaPackageException("Error loading source media package: " + e.getMessage());
      }

      // Load target media package
      try {
        dest = builder.loadFromDirectory(targetDir);
        targetIsLocked = isLocked(targetDir);
        lockMediaPackage(targetDir, true);
      } catch (Throwable e) {
        throw new MediaPackageException("Error loading target media package: " + e.getMessage());
      }

      // Add media package elements from source media package
      try {
        for (MediaPackageElement e : src.elements()) {
          if (dest.getElementById(e.getIdentifier()) == null)
            dest.add(e, move);
          else {
            if (MergeMode.Replace == mode) {
              log_.debug("Replacing element " + e.getIdentifier() + " while merging " + dest + " with " + src);
              dest.remove(dest.getElementById(e.getIdentifier()));
              dest.add(e, move);
            } else if (MergeMode.Skip == mode) {
              log_.debug("Skipping element " + e.getIdentifier() + " while merging " + dest + " with " + src);
              continue;
            } else if (MergeMode.Merge == mode) {
              log_.debug("Renaming element " + e.getIdentifier() + " while merging " + dest + " with " + src);
              e.setIdentifier(null);
              dest.add(e, move);
            } else if (MergeMode.Fail == mode) {
              throw new MediaPackageException("Target media package " + dest + " already contains element with id "
                      + e.getIdentifier());
            }
          }
        }
        dest.save();
      } catch (UnsupportedElementException e) {
        throw new MediaPackageException(e);
      }

      // Cleanup after moving
      if (move) {
        log_.debug("Removing empty source media package folder");
        FileSupport.delete(sourceDir, true);
      }

    } finally {
      if (srcIsLocked)
        unlockMediaPackage(sourceDir);
      if (targetIsLocked)
        unlockMediaPackage(targetDir);
    }

    // Return the target media package
    return dest;
  }

}