package org.opencastproject.core.api.model;

import java.io.Serializable;

/**
 * A workflow to ingest and optionally process and distribute media.
 */
public interface MediaProcessingWorkflow {
	/**
	 * The unique identifier for this {@link MediaProcessingWorkflow}
	 * @return
	 */
	public String getId();

	/**
	 * The description for this {@link MediaProcessingWorkflow}
	 * @return
	 */
	public String getDescription();

	/**
	 * The actual workflow definition for this {@link MediaProcessingWorkflow}.
	 * The format of this definition may vary among implementations, and may be
	 * a Podcast Producer plist, an ant script, a bpel script, or something else
	 * entirely.
	 * @return
	 */
	public Serializable getWorkflowDefinition();
}
