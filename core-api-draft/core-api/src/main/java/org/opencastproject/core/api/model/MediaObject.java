package org.opencastproject.core.api.model;

import java.io.Serializable;

/**
 * A collection of media content and its associated metadata.  What these mean
 * in real terms is still to be determined.
 */
public interface MediaObject {
	/**
	 * The media itself
	 * @return
	 */
	public Serializable getMedia();
	
	/**
	 * The media's metadata
	 * @return
	 */
	public Serializable getMetadata();
}
