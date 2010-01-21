/**
 *  Copyright 2009 The Regents of the University of California
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

package org.opencastproject.inspection.impl;

import org.opencastproject.util.ConfigurationException;

/**
 * Factory to create a {@link MediaAnalyzer} for media files. Use the static method {@link #newInstance()} to obtain a
 * reference to a concrete implementation of a <code>MediaAnalyzerFactory</code>. This instance can then be used to
 * create a media analzer.
 * 
 * <p>
 * The factory can be configured by specifying the concrete implementation class through the system property
 * <code>opencast.mediaanalyzer</code>.
 * </p>
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @version $Id: MediaAnalyzerFactory.java 355 2009-08-12 17:57:32Z twunden $
 */
public class MediaAnalyzerFactory {

  /** Class name for the default media package builder */
  private static final String BUILDER_CLASS = "org.opencastproject.inspection.impl.MediaInfoAnalyzer";

  /** Name of the system property */
  public static final String PROPERTY_NAME = "opencast.mediaanalyzer";

  /** The implementation class name */
  private static String analyzerClassName = BUILDER_CLASS;

  /** The singleton instance of this factory */
  private static final MediaAnalyzerFactory factory = new MediaAnalyzerFactory();

  /**
   * Private method to create a new media analyzer factory.
   */
  private MediaAnalyzerFactory() {
    String className = System.getProperty(PROPERTY_NAME);
    if (className != null) {
      analyzerClassName = className;
    }
  }

  /**
   * Returns an instance of a MediaAnalyzerFactory.
   * 
   * @return the media analyzer factory
   * @throws ConfigurationException
   *           if the factory cannot be instantiated
   */
  public static MediaAnalyzerFactory newInstance() throws ConfigurationException {
    return factory;
  }

  /**
   * Factory method that returns a new instance of the default media analyzer implementation.
   * <p>
   * It uses the following ordered lookup procedure to determine which implementation of the {@link MediaAnalyzer}
   * interface to use:
   * <ul>
   * <li>Implementation specified using the <code>opencast.mediaanalyzer</code> system property</li>
   * <li>Platform default implementation</li>
   * </ul>
   * 
   * @return the media analyzer
   * @throws ConfigurationException
   *           If the analyzer cannot be instantiated
   */
  public MediaAnalyzer newMediaAnalyzer() throws ConfigurationException {
    try {
      Class<?> builderClass = Class.forName(analyzerClassName);
      return (MediaAnalyzer) builderClass.newInstance();
    } catch (ClassNotFoundException e) {
      throw new ConfigurationException("Class not found while creating media analyzer: " + e.getMessage(), e);
    } catch (InstantiationException e) {
      throw new ConfigurationException("Instantiation exception while creating media analyzer: " + e.getMessage(), e);
    } catch (IllegalAccessException e) {
      throw new ConfigurationException("Access exception while creating media analyzer: " + e.getMessage(), e);
    }
  }

}
