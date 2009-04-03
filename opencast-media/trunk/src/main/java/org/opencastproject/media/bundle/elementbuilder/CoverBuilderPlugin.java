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
import org.opencastproject.media.bundle.BundleElementBuilderPlugin;
import org.opencastproject.media.bundle.BundleException;
import org.opencastproject.media.bundle.Cover;
import org.opencastproject.media.bundle.attachment.CoverImpl;

/**
 * This implementation of the {@link BundleElementBuilderPlugin} recognizes 
 * attachments in the Portable Document Format (pdf) and creates bundle element
 * representations for them.
 * <p>
 * The test depends solely on the mimetype.
 * </p>
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id$
 */
public class CoverBuilderPlugin extends AbstractAttachmentBuilderPlugin implements BundleElementBuilderPlugin {
	
	/**
	 * Creates a new attachment builder that will accept attachments of type
	 * {@link Cover}.
	 */
	public CoverBuilderPlugin() {
		super(Cover.FLAVOR);
	}

	/**
	 * @see org.opencastproject.media.bundle.elementbuilder.AbstractAttachmentBuilderPlugin#specializeAttachment(org.opencastproject.media.bundle.Attachment)
	 */
	@Override
	protected Attachment specializeAttachment(Attachment attachment) throws BundleException {
		try {
			return new CoverImpl(attachment);
		} catch (Exception e) {
			throw new BundleException("Failed to specialize cover " + attachment + ": " + e.getMessage());
		}
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Cover Builder Plugin";
	}
	
}