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

import org.opencastproject.util.ConfigurationException;

/**
 * Factory to retreive instances of a bundle builder. Use the static method
 * {@link #newInstance()} to obtain a reference to a concrete implementation of a 
 * <code>BundleBuilderFactory</code>. This instance can then be used to create
 * or load bundles.
 * <p>
 * The factory can be configured by specifying the concrete implementation class through
 * the system property <code>opencast.BundleBuilderFactory</code>.
 * </p>
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id$
 */
public final class BundleBuilderFactory {
	
	/** Class name for the default bundler builder */
	private static final String BUILDER_CLASS = "org.opencastproject.media.bundle.BundleBuilderImpl";
	
	/** Name of the system property */
	public static final String PROPERTY_NAME = "opencast.bundlebuilder";
	
	/** The implementation class name */
	private static String builderClassName = BUILDER_CLASS;

	/** The singleton instance of this factory */
	private static final BundleBuilderFactory factory = new BundleBuilderFactory();

	/** The default builder implementation */
	private BundleBuilder builder = null;

	/**
	 * Private method to create a new bundle builder factory.
	 */
	private BundleBuilderFactory() {
		String className = System.getProperty(PROPERTY_NAME);
		if (className != null) {
			builderClassName = className;
		}
	}
	
	/**
	 * Returns an instance of a BundleBuilderFactory.
	 * 
	 * @return the bundle builder factory
	 * @throws ConfigurationException
	 * 					if the factory cannot be instantiated
	 */
	public static BundleBuilderFactory newInstance() throws ConfigurationException {
		return factory;
	}

	/**
	 * Factory method that returns an instance of a bundle builder.
	 * <p>
 	 * It uses the following ordered lookup procedure to determine which
 	 * implementation of the {@link BundleBuilder} interface to use:
	 * <ul>
	 * <li>Implementation specified using the <code>opencast.bundlebuilder</code> system property</li>
	 * <li>Platform default implementation</li>
	 * </ul>
	 * 
	 * @return the bundle builder
	 * @throws ConfigurationException
	 * 				If the builder cannot be instantiated
	 */
	public BundleBuilder newBundleBuilder() throws ConfigurationException {
		if (builder == null) {
			try {
				Class<?> builderClass = Class.forName(builderClassName);
				builder = (BundleBuilder)builderClass.newInstance();
			} catch (ClassNotFoundException e) {
				throw new ConfigurationException("Class not found while creating bundle builder: " + e.getMessage(), e);
			} catch (InstantiationException e) {
				throw new ConfigurationException("Instantiation exception while creating bundle builder: " + e.getMessage(), e);
			} catch (IllegalAccessException e) {
				throw new ConfigurationException("Access exception while creating bundle builder: " + e.getMessage(), e);
			}
		}
		return builder;
	}
	
}