/**
 *  Copyright 2009 Opencast Project (http://www.opencastproject.org)
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

package org.opencastproject.media.bundle;

import org.opencastproject.util.FileSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Utility class used for bundle handling.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id$
 */
public class BundleSupport {
	
	/**
	 * Mode used when merging bundles.
	 * <p>
	 * <ul>
	 * 	<li><code>Merge</code> assigns a new identifier in case of conflicts</li>
	 * 	<li><code>Replace</code> replaces elements in the target bundle with matching identifier</li>
	 * 	<li><code>Skip</code> skips elements from the source bundle with matching identifer</li>
	 * 	<li><code>Fail</code> fail in case of conflicting identifier</li>
	 * </ul>
	 * 
	 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
	 */
	public enum MergeMode { Merge, Replace, Skip, Fail };

	/** the logging facility provided by log4j */
	private final static Logger log_ = LoggerFactory.getLogger(BundleSupport.class);

	/**
	 * Returns <code>true</code> if a lockfile exists in the directory 
	 * <code>dir</code>.
	 * 
	 * @param dir the bundle directory
	 */
	public static boolean isLocked(File dir) {
		File lock = new File(dir, Bundle.LOCKFILE);
		return lock.exists();
	}

	/**
	 * Locks the bundle located in <code>dir</code> to make sure no other ingest
	 * process will try to process this bundle. A <code>BundleException</code>
	 * will be thrown if the bundle is already locked.
	 * 
	 * @param dir the bundle directory
	 * @throws BundleException
	 * 		if the lock already exists or cannot be created
	 */
	public static boolean lockBundle(File dir) throws BundleException {
		return lockBundle(dir, false);
	}

	/**
	 * Locks the bundle located in <code>dir</code> to make sure no other ingest
	 * process will try to process this bundle.
	 * <p>
	 * If <code>force</code> is <code>true</code>, this method will not throw and
	 * exception if the lock alreday exists.
	 * </p>
	 * 
	 * @param dir the bundle directory
	 * @param force <code>true</code> to relock an already locked bundle
	 * @throws BundleException
	 * 		if the lock already exists or cannot be created
	 */
	public static boolean lockBundle(File dir, boolean force) throws BundleException {
		File lock = new File(dir, Bundle.LOCKFILE);
		if (lock.exists()) {
			if (force)
				log_.debug("Overwriting existing bundle lock");
			else {
				log_.debug("Bundle at "+ dir + " is already locked");
				return false;
			}
		}
		try {
			lock.createNewFile();
		} catch (IOException e) {
			throw new BundleException("Error creating bundle lock at " + dir + ": " + e.getMessage());
		}
		return true;
	}
	
	/**
	 * Removes the lockfile from the bundle directory.
	 * 
	 * @param dir the bundle directory
	 * @throws BundleException
	 * 		if the lock file cannot be removed
	 */
	public static void unlockBundle(File dir) throws BundleException {
		File lock = new File(dir, Bundle.LOCKFILE);
		if (!lock.exists())
			return;
		try {
			lock.delete();
		} catch (Exception e) {
			throw new BundleException("Error removing bundle lock at " + dir + ": " + e.getMessage(), e);
		}
	}

	/**
	 * Merges the contents of bundle <code>sourceBundle</code> into the bundle
	 * <code>targetBundle</code>.
	 * <p>
	 * When choosing to move the bundle element into the new place instead of
	 * copying them, the souce bundle folder will be removed afterwards.
	 * </p>
	 * 
	 * @param targetDir the target bundle directory
	 * @param sourceDir the source bundle directory
	 * @param move <code>true</code> to move the bundle contents, <code>false</code> to make a copy
	 * @param mode conflict resolution strategy in case of identical element identifier
	 * @throws BundleException
	 * 		if an error occurs either accessing one of the two bundles or merging them
	 */
	public static Bundle merge(File targetDir, File sourceDir, boolean move, MergeMode mode) throws BundleException {
		Bundle src = null;
		Bundle dest = null;
		boolean srcIsLocked = false;
		boolean targetIsLocked = false;

		// Get hold of a bundle builder
		BundleBuilderFactory builderFactory = BundleBuilderFactory.newInstance();
		BundleBuilder builder = builderFactory.newBundleBuilder();

		try {
			// Load source bundle
			try {
				src = builder.loadFromDirectory(sourceDir);
				srcIsLocked = isLocked(sourceDir);
				lockBundle(targetDir, true);
			} catch (Throwable e) {
				throw new BundleException("Error loading source bundle: " + e.getMessage());
			}
	
			// Load target bundle
			try {
				dest = builder.loadFromDirectory(targetDir);
				targetIsLocked = isLocked(targetDir);
				lockBundle(targetDir, true);
			} catch (Throwable e) {
				throw new BundleException("Error loading target bundle: " + e.getMessage());
			}
			
			// Add bundle elements from source bundle
			try {
				for (BundleElement e : src.elements()) {
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
							throw new BundleException("Target bundle " + dest + " already contains element with id " + e.getIdentifier());
						}
					}
				}
				dest.save();
			} catch (UnsupportedBundleElementException e) {
				throw new BundleException(e);
			}
			
			// Cleanup after moving
			if (move) {
				log_.debug("Removing empty source bundle folder");
				FileSupport.delete(sourceDir, true);
			}

		} finally {
			if (srcIsLocked)
				unlockBundle(sourceDir);
			if (targetIsLocked)
				unlockBundle(targetDir);
		}

		// Return the target bundle
		return dest;
	}

}