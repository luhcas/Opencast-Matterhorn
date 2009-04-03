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
import java.io.Serializable;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

/**
 * General definition for metadata catalogs.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id$
 */
public interface Catalog extends BundleElement, Serializable {

	/** Bundle element type */
	BundleElement.Type TYPE = BundleElement.Type.Catalog;

    /**
     * Saves the catalog to disk.
     *
	 * @throws ParserConfigurationException
	 * 		if the xml parser environment is not correctly configured
	 * @throws TransformerException
	 * 		if serialization of the metadata document fails
	 * @throws IOException
	 * 		if an error with catalog file handling occurs
     */
    void save() throws ParserConfigurationException, TransformerException, IOException;

}