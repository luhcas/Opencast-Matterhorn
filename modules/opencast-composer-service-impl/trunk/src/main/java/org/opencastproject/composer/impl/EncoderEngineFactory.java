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

package org.opencastproject.composer.impl;

import org.opencastproject.composer.api.EncoderEngine;
import org.opencastproject.util.ConfigurationException;

/**
 * Factory used to obtain an encoder engine based on the node configuration and an optional encoding profile passed.
 */
public class EncoderEngineFactory {

  /** Class name for the default engine factory */
  public static final String FACTORY_CLASS = "org.opencastproject.composer.impl.EncoderEngineFactoryImpl";

  /** Class name for the default engine */
  public static final String DEFAULT_ENGINE_CLASS = "org.opencastproject.composer.impl.ffmpeg.FFmpegEncoderEngine";

  /** Name of the system property */
  public static final String PROPERTY_NAME = "org.opencastproject.encoderfactory";

  /** The implementation class name */
  private static String factoryClassName = FACTORY_CLASS;

  /** The implementation class name */
  private static String defaultEngineClassName = DEFAULT_ENGINE_CLASS;

  /** The singleton instance of this factory */
  private static EncoderEngineFactory factory_ = null;

  /** The default encoder engine */
  private EncoderEngine ffmpegEncoder_ = null;

  /**
   * Private method to create a new engine factory.
   */
  protected EncoderEngineFactory() {
    String className = System.getProperty(PROPERTY_NAME);
    if (className != null) {
      factoryClassName = className;
    }
  }

  /**
   * Factory method that returns an instance of an encoding engine factory.
   * <p>
   * It uses the following ordered lookup procedure to determine which implementation of the
   * {@link EncoderEngineFactory} to use:
   * <ul>
   * <li>Implementation specified using the <code>org.opencastproject.encoderfactory</code> system property</li>
   * <li>Platform default implementation</li>
   * </ul>
   * 
   * @return the encoder engine factory
   * @throws ConfigurationException
   *           if the factory cannot be instantiated
   */
  public static EncoderEngineFactory newInstance() throws ConfigurationException {
    if (factory_ == null) {
      try {
        Class<?> builderClass = Class.forName(factoryClassName);
        factory_ = (EncoderEngineFactory) builderClass.newInstance();
      } catch (ClassNotFoundException e) {
        throw new ConfigurationException("Class not found while creating engine factory: " + e.getMessage(), e);
      } catch (InstantiationException e) {
        throw new ConfigurationException("Instantiation exception while creating engine factory: " + e.getMessage(), e);
      } catch (IllegalAccessException e) {
        throw new ConfigurationException("Access exception while creating engine factory: " + e.getMessage(), e);
      }
    }
    return factory_;
  }

  /**
   * Factory method that returns an instance of an encoding engine.
   * <p>
   * This implementation always returns the ffmpeg encoding engine, regarless of the profile. Implement your own factory
   * to change this behaviour.
   * </p>
   * 
   * @param profile
   *          the encoding profile
   * @return the encoder engine
   * @throws ConfigurationException
   *           If the engine cannot be instantiated
   */
  public EncoderEngine newEngineByProfile(String profile) throws ConfigurationException {
    if (ffmpegEncoder_ == null) {
      try {
        Class<?> builderClass = Class.forName(defaultEngineClassName);
        ffmpegEncoder_ = (EncoderEngine) builderClass.newInstance();
      } catch (ClassNotFoundException e) {
        throw new ConfigurationException("Class not found while creating default (ffmpeg) encoder: " + e.getMessage(), e);
      } catch (InstantiationException e) {
        throw new ConfigurationException("Instantiation exception while creating default (ffmpeg) encoder: "
                + e.getMessage(), e);
      } catch (IllegalAccessException e) {
        throw new ConfigurationException("Access exception while creating default (ffmpeg) encoder: " + e.getMessage(), e);
      }
    }
    return ffmpegEncoder_;
  }

}