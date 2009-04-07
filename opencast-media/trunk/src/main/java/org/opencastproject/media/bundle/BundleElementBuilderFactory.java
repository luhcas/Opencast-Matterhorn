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
 * Factory to retreive instances of a bundle element builder. Use the static
 * method {@link #newInstance()} to obtain a reference to a concrete
 * implementation of a <code>BundleElementBuilderFactory</code>. This instance
 * can then be used to create or load bundle elements.
 * <p>
 * The factory can be configured by specifying the concrete implementation class
 * through the system property <code>opencast.BundleElementBuilderFactory</code>
 * .
 * </p>
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id: BundleElementBuilderFactory.java 99 2009-04-03 19:53:39Z wunden
 *          $
 */
public final class BundleElementBuilderFactory {

  /** Class name for the default bundler element builder */
  private static final String BUILDER_CLASS = "org.opencastproject.media.bundle.BundleElementBuilderImpl";

  /** Name of the system property */
  public static final String PROPERTY_NAME = "opencast.bundleelementbuilder";

  /** The implementation class name */
  private static String builderClassName = BUILDER_CLASS;

  /** The singleton instance of this factory */
  private static final BundleElementBuilderFactory factory = new BundleElementBuilderFactory();

  /** The default builder implementation */
  private BundleElementBuilder builder = null;

  /**
   * Private method to create a new element builder factory.
   */
  private BundleElementBuilderFactory() {
    String className = System.getProperty(PROPERTY_NAME);
    if (className != null) {
      builderClassName = className;
    }
  }

  /**
   * Returns an instance of a BundleElementBuilderFactory.
   * 
   * @return the bundle element builder factory
   * @throws ConfigurationException
   *           if the factory cannot be instantiated
   */
  public static BundleElementBuilderFactory newInstance()
      throws ConfigurationException {
    return factory;
  }

  /**
   * Factory method that returns an instance of a bundle element builder.
   * <p>
   * It uses the following ordered lookup procedure to determine which
   * implementation of the {@link BundleElementBuilder} interface to use:
   * <ul>
   * <li>Implementation specified using the
   * <code>opencast.bundleelementbuilder</code> system property</li>
   * <li>Platform default implementation</li>
   * </ul>
   * 
   * @return the bundle element builder
   * @throws ConfigurationException
   *           If the builder cannot be instantiated
   */
  public BundleElementBuilder newElementBuilder() throws ConfigurationException {
    if (builder == null) {
      try {
        Class<?> builderClass = Class.forName(builderClassName);
        builder = (BundleElementBuilder) builderClass.newInstance();
      } catch (ClassNotFoundException e) {
        throw new ConfigurationException(
            "Class not found while creating element builder: " + e.getMessage(),
            e);
      } catch (InstantiationException e) {
        throw new ConfigurationException(
            "Instantiation exception while creating element builder: "
                + e.getMessage(), e);
      } catch (IllegalAccessException e) {
        throw new ConfigurationException(
            "Access exception while creating element builder: "
                + e.getMessage(), e);
      } catch (Exception e) {
        throw new ConfigurationException(
            "Exception while creating element builder: " + e.getMessage(), e);
      }
    }
    return builder;
  }

}