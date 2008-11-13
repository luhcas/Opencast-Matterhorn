package org.opencastproject.api;

import javax.jcr.Repository;

/**
 * Provides access to the Opencast JCR Repository
 */
public interface OpencastJcrServer {

	public Repository getRepository();

}