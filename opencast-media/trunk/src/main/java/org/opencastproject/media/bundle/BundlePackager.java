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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Interface describing a bundle packager, that will, for example, pack a
 * bundle structure into a jar file.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id$
 */
public interface BundlePackager {

	/**
	 * Reads a bundle from the given input stream, unpacked into the
	 * <code>bundleRoot</code> directory.
	 * @param in 
	 * 		the archive input stream
	 * 
	 * @return the bundle
	 * @throws IOException
	 * 		if accessing the package fails
	 * @throws BundleException
	 * 		if unpacking the jar file fails
	 */
	Bundle unpack(InputStream in) throws IOException, BundleException;
	
	/**
	 * Saves a bundle to the given output stream.
	 * 
	 * @param bundle the bundle
	 * @param out the output stream
	 * @throws IOException
	 * 		if accessing a bundle file fails
	 * @throws BundleException
	 * 		if creating the jar file fails
	 */
	void pack(Bundle bundle, OutputStream out) throws IOException, BundleException;
	
}