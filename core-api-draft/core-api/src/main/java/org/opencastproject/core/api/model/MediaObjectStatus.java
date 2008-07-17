package org.opencastproject.core.api.model;

/**
 * The status describing a particular MediaObject as it moves through a
 * {@link MediaProcessingWorkflow}.
 */
public interface MediaObjectStatus {
	
	/**
	 * The description of the status
	 * @return
	 */
	public String getDescription();
}
