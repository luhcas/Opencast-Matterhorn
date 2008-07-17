package org.opencastproject.core.api;

import java.io.Serializable;
import java.util.Set;

import org.opencastproject.core.api.model.MediaObject;
import org.opencastproject.core.api.model.MediaProcessingWorkflow;

/**
 * This service provides a means to query and manage the
 * {@link MediaProcessingWorkflow}s registered with the core, and to submit new
 * {@link MediaObject}s to the core using one of the registered
 * {@link MediaProcessingWorkflow}s.
 *
 */
public interface MediaInputService {
	/**
	 * Gets all currently registered {@link MediaProcessingWorkflow}s.
	 * @return
	 */
	public Set<MediaProcessingWorkflow> getWorkflows();

	/**
	 * Adds a new {@link MediaProcessingWorkflow}
	 * @param description The description for the new {@link MediaProcessingWorkflow}
	 * @return The ID of the newly registered {@link MediaProcessingWorkflow}
	 */
	public String addWorkflow(String description);

	/**
	 * Removes the registration for an existing {@link MediaProcessingWorkflow}
	 * @param workflowId The ID for the {@link MediaProcessingWorkflow}
	 * @return Whether the workflow existed, and was therefore removed, or not
	 */
	public boolean removeWorkflow(String workflowId);
	
	/**
	 * Adds a {@link MediaObject} to the core for processing via a workflow.
	 * @param media
	 * @param metadata
	 * @param workflow
	 * @return The unique ID of the mediaObject, as defined by the core implementation.
	 */
	public String submitMedia(Serializable media, Serializable metadata, String workflowId);
}
