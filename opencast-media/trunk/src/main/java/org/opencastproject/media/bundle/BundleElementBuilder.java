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

import org.w3c.dom.Node;

import java.io.File;
import java.io.IOException;

/**
 * A bundle element builder provides factory methods for the creation and
 * loading of bundle elements from files.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id$
 */
public interface BundleElementBuilder {

	/**
	 * Creates a bundle element from the given file that was previously accepted.
	 * <p>
	 * Since only the file is given, it is possible, that the best builder
	 * plugin cannot be uniquely identified and may require additional
	 * contraints, e. g. a matching filename. Be sure to check the documentation
	 * of the corresponding plugin for details.
	 * </p>
	 * 
	 * @param file the file to ingest
	 * @return the new bundle element
	 * @throws BundleException
	 * 			if creating the bundle element fails
	 */
	BundleElement elementFromFile(File file) throws BundleException;

	/**
	 * Creates a bundle element from the given file that was previously accepted,
	 * while <code>type</code> and <code>flavor</code> may be taken as strong
	 * hints and may both be <code>null</code>.
	 * <p>
	 * If only the file is given, it is possible, that the best suited builder
	 * plugin cannot be uniquely identified and may require additional
	 * contraints, e. g. a matching filename. Be sure to check the documentation
	 * of the corresponding builder plugin for details.
	 * </p>
	 * 
	 * @param file the file to ingest
	 * @param type the element type
	 * @param flavor the element flavor
	 * @return the new bundle element
	 * @throws BundleException
	 * 			if creating the bundle element fails
	 */
	BundleElement elementFromFile(File file, BundleElement.Type type, BundleElementFlavor flavor) throws BundleException;

	/**
	 * Creates a bundle element from the DOM element.
	 * 
	 * @param elementNode the DOM node
	 * @param bundleRoot the bundle root directory
	 * @param verify <code>true</code> to verify the element's integrity
	 * @return the bundle element
	 * @throws BundleException
	 * 		if reading the file from manifest fails
	 */
	BundleElement elementFromManifest(Node elementNode, File bundleRoot, boolean verify) throws BundleException;
	
	/**
	 * Creates a new bundle elment of the specified type.
	 * 
	 * @param type the element type
	 * @param flavor the element flavor
	 * @return the new bundle element
	 * @throws IOException
	 * 		if the bundle element's file cannot be created
	 */
	BundleElement newElement(BundleElement.Type type, BundleElementFlavor flavor) throws IOException;

}