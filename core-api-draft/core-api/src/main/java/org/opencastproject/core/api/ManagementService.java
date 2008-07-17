package org.opencastproject.core.api;

import java.util.Set;

import org.opencastproject.core.api.model.CoreCapability;
import org.opencastproject.core.api.model.MediaObject;
import org.opencastproject.core.api.model.MediaObjectStatus;
import org.opencastproject.core.api.model.MediaProcessingWorkflow;

/**
 * This service provides a means to query (and manage?) the state of
 * {@link MediaObject}s as the move through {@link MediaProcessingWorkflow}s
 * in the core, and for querying and managing the Core itself.
 */
public interface ManagementService {

	/**
	 * Gets the {@link MediaObjectStatus} for a given {@link MediaObject}'s ID
	 * @param mediaObjectId
	 * @return
	 */
	public MediaObjectStatus getStatus(String mediaObjectId);

	/**
	 * Gets the {@link CoreCapability}s provided by this core implementation.
	 * @return
	 */
	public Set<CoreCapability> getCoreCapabilities();
}
