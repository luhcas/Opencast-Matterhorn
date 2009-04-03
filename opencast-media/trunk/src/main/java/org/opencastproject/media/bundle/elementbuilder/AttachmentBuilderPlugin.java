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

package org.opencastproject.media.bundle.elementbuilder;

import org.opencastproject.media.bundle.Attachment;
import org.opencastproject.media.bundle.BundleElement;
import org.opencastproject.media.bundle.BundleElementBuilder;
import org.opencastproject.media.bundle.BundleElementFlavor;

import org.w3c.dom.Node;

import java.io.File;
import java.io.IOException;

import javax.xml.xpath.XPathExpressionException;

/**
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id$
 */
public class AttachmentBuilderPlugin extends AbstractAttachmentBuilderPlugin implements BundleElementBuilder {

	/**
	 * @see org.opencastproject.media.bundle.elementbuilder.AbstractAttachmentBuilderPlugin#accept(java.io.File, org.opencastproject.media.bundle.BundleElement.Type, org.opencastproject.media.bundle.BundleElementFlavor)
	 */
	@Override
	public boolean accept(File file, BundleElement.Type type, BundleElementFlavor flavor) throws IOException {
		if (type != null && flavor != null) {
			if (!type.equals(BundleElement.Type.Attachment) || flavor != null)
				return false;
		} else if (type != null && !type.equals(BundleElement.Type.Attachment)) {
			return false;
		} else if (flavor != null && !flavor.equals(Attachment.FLAVOR)) {
			return false;
		}
		return super.accept(file, type, flavor);
	}
	
	/**
	 * @see org.opencastproject.media.bundle.elementbuilder.AbstractAttachmentBuilderPlugin#accept(org.w3c.dom.Node)
	 */
	@Override
	public boolean accept(Node elementNode) {
		try {
			String flavor = xpath.evaluate("@type", elementNode);
			if (flavor != null && !"".equals(flavor) && !Attachment.FLAVOR.toString().equals(flavor))
				return false;
			return super.accept(elementNode);
		} catch (XPathExpressionException e) {
			return false;
		}
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Attachment Builder Plugin";
	}

}