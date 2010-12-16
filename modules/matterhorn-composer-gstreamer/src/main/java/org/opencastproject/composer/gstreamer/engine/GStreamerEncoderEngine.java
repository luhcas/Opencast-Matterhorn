/**
 *  Copyright 2009, 2010 The Regents of the University of California
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
package org.opencastproject.composer.gstreamer.engine;

import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.composer.gstreamer.AbstractGSEncoderEngine;

import org.gstreamer.Bus;
import org.gstreamer.GstObject;
import org.gstreamer.Pipeline;
import org.gstreamer.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Encoder engine that uses GStreamer for encoding.
 */
public class GStreamerEncoderEngine extends AbstractGSEncoderEngine {

  /** Suffix for gstreamer pipeline template */
  private static final String GS_SUFFIX = "gstreamer.pipeline";

  /** Logger utility */
  private static final Logger logger = LoggerFactory.getLogger(GStreamerEncoderEngine.class);

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.opencastproject.composer.gstreamer.AbstractGSEncoderEngine#createAndLaunchPipeline(org.opencastproject.composer
   * .api.EncodingProfile, java.util.Map)
   */
  @Override
  protected void createAndLaunchPipeline(EncodingProfile profile, Map<String, String> properties)
          throws EncoderException {

    logger.info("Creating pipeline definition from: {}", profile.getName());
    String pipelineDefinition = buildGStreamerPipelineDefinition(profile, properties);
    logger.info("Creating pipeline from: {}", pipelineDefinition);
    GSPipeline pipeline = createPipeline(pipelineDefinition);
    logger.info("Executing pipeline built from: {}", pipelineDefinition);
    launchPipeline(pipeline);
    logger.info("Execution successful");
  }

  /**
   * Builds string representation of gstreamer pipeline by substituting templates from pipeline template with actual
   * values from properties. Template format is #{property.name}. All unmatched properties are removed.
   * 
   * @param profile
   *          EncodingProfile used for this encoding job
   * @param properties
   *          Map that contains substitutions for templates
   * @return String representation of gstreamer pipeline
   * @throws EncoderException
   *           if profile does not contain gstreamer template
   */
  private String buildGStreamerPipelineDefinition(EncodingProfile profile, Map<String, String> properties)
          throws EncoderException {

    String pipelineTemplate = profile.getExtension(GS_SUFFIX);
    if (pipelineTemplate == null) {
      logger.warn("Profile {} does not contain gstreamer pipeline template.", profile.getName());
      throw new EncoderException("Profile " + profile.getName() + " does not contain gstreamer pipeline template");
    }

    // substitute templates for actual values
    String pipelineDefinition = substituteTemplateValues(pipelineTemplate, properties, true);

    return pipelineDefinition;
  }

  /**
   * Creates GSPipeline that contains gstreamer Pipeline with its MonitorObject from string representation of pipeline.
   * Syntax is equivalent to the gstreamer command line syntax.
   * 
   * @param pipelineDefinition
   *          String representation of gstreamer pipeline
   * @return GSPipeline that contains built Pipeline and MonitorObject
   * @throws EncoderException
   *           if Pipeline cannot be constructed from pipeline definition
   */
  private GSPipeline createPipeline(String pipelineDefinition) throws EncoderException {

    if (pipelineDefinition == null || pipelineDefinition.equals("")) {
      logger.warn("No pipeline definition specified.");
      throw new EncoderException("Pipeline definition is null");
    }

    Pipeline pipeline;
    try {
      pipeline = Pipeline.launch(pipelineDefinition);
    } catch (Throwable t) {
      logger.warn("Could not create pipeline from definition \"{}\": {}", pipelineDefinition, t.getMessage());
      throw new EncoderException("Unable to create pipeline from: " + pipelineDefinition, t);
    }
    if (pipeline == null) {
      logger.warn("No pipeline was created from \"{}\"", pipelineDefinition);
      throw new EncoderException("No pipeline was created from: " + pipelineDefinition);
    }

    MonitorObject monitorObject = createNewMonitorObject();
    installListeners(pipeline, monitorObject);

    return createNewGSPipeline(pipeline, monitorObject);
  }

  /**
   * Executes GSPipeline. Blocks until either exception occures in processing pipeline or EOS is reached.
   * 
   * @param gspipeline
   *          GSPipeline used for execution
   * @throws EncoderException
   *           if current thread is interrupted or exception occurred in processing pipeline
   */
  private void launchPipeline(GSPipeline gspipeline) throws EncoderException {

    if (Thread.interrupted()) {
      logger.warn("Failed to start processing pipeline: Thread interrupted");
      throw new EncoderException("Failed to start processing pipeline: Thread interrupted");
    }

    gspipeline.getPipeline().play();
    synchronized (gspipeline.getMonitorObject().getMonitorLock()) {
      try {
        while (!gspipeline.getMonitorObject().getEOSReached())
          gspipeline.getMonitorObject().getMonitorLock().wait();
      } catch (InterruptedException e) {
        logger.warn("Thread interrupted while processing");
        throw new EncoderException("Could not finish processing", e);
      } finally {
        gspipeline.getPipeline().stop();
      }
    }

    String errorMessage = gspipeline.getMonitorObject().getFirstErrorMessage();
    if (errorMessage != null) {
      logger.warn("Errors in processing pipeline");
      throw new EncoderException("Error occurred in processing pipeline: " + errorMessage);
    }
  }

  /**
   * Substitutes template values from template with actual values from properties.
   * 
   * @param template
   *          String that represents template
   * @param properties
   *          Map that contains substitution for template values in template
   * @param cleanup
   *          if template values that were not matched should be removed
   * @return String built from template
   */
  private String substituteTemplateValues(String template, Map<String, String> properties, boolean cleanup) {

    StringBuffer buffer = new StringBuffer();
    Pattern pattern = Pattern.compile("#\\{\\S+?\\}");
    Matcher matcher = pattern.matcher(template);
    while (matcher.find()) {
      String match = template.substring(matcher.start() + 2, matcher.end() - 1);
      if (properties.containsKey(match)) {
        matcher.appendReplacement(buffer, properties.get(match));
      }
    }
    matcher.appendTail(buffer);

    String processedTemplate = buffer.toString();

    if (cleanup) {
      // remove all property matches
      buffer = new StringBuffer();
      Pattern ppattern = Pattern.compile("\\S+?=#\\{\\S+?\\}");
      matcher = ppattern.matcher(processedTemplate);
      while (matcher.find()) {
        matcher.appendReplacement(buffer, "");
      }
      matcher.appendTail(buffer);
      processedTemplate = buffer.toString();

      // remove all other templates
      buffer = new StringBuffer();
      matcher = pattern.matcher(processedTemplate);
      while (matcher.find()) {
        matcher.appendReplacement(buffer, "");
      }
      matcher.appendTail(buffer);
      processedTemplate = buffer.toString();
    }

    return processedTemplate;
  }

  /**
   * Install various listeners to Pipeline, such as: ERROR, WARNING, INFO, STATE_CHANGED and EOS.
   * 
   * @param pipeline
   *          Pipeline to which listeners will be installed
   * @param monitorObject
   *          MonitorObject used for monitoring state of pipeline: errors and EOS
   */
  private void installListeners(Pipeline pipeline, final MonitorObject monitorObject) {
    pipeline.getBus().connect(new Bus.ERROR() {
      @Override
      public void errorMessage(GstObject source, int code, String message) {
        String errorMessage = source.getName() + ": " + code + " - " + message;
        monitorObject.addErrorMessage(errorMessage);
        logger.error("Error in processing pipeline: {}", errorMessage);
        // terminate pipeline immediately
        monitorObject.setStopPipeline(true);
        synchronized (monitorObject.getMonitorLock()) {
          monitorObject.getMonitorLock().notifyAll();
        }
      }
    });
    pipeline.getBus().connect(new Bus.WARNING() {
      @Override
      public void warningMessage(GstObject source, int code, String message) {
        logger.warn("Warning in processing pipeline: {}: {} - {}",
                new String[] { source.getName(), Integer.toString(code), message });
      }
    });
    pipeline.getBus().connect(new Bus.INFO() {
      @Override
      public void infoMessage(GstObject source, int code, String message) {
        logger.info("{}: {} - {}", new String[] { source.getName(), Integer.toString(code), message });
      }
    });
    pipeline.getBus().connect(new Bus.STATE_CHANGED() {
      @Override
      public void stateChanged(GstObject source, State old, State current, State pending) {
        logger.debug("{}: State changed from {} to {}",
                new String[] { source.getName(), old.toString(), current.toString() });
      }
    });
    pipeline.getBus().connect(new Bus.EOS() {
      @Override
      public void endOfStream(GstObject source) {
        logger.info("{}: End of stream reached.", source.getName());
        monitorObject.setStopPipeline(true);
        synchronized (monitorObject.getMonitorLock()) {
          monitorObject.getMonitorLock().notifyAll();
        }
      }
    });
  }

  /**
   * Creates new MonitorObject, which keeps information of pipelines errors, and if pipeline should be stopped.
   * 
   * @return new MonitorObject
   */
  private MonitorObject createNewMonitorObject() {
    MonitorObject monitorObject = new MonitorObject() {

      /** Lock object for notification between threads */
      private final Object lock = new Object();
      /** If pipeline should be stopped (EOS or error) */
      private AtomicBoolean stopPipeline = new AtomicBoolean(false);
      /** List of errors */
      private LinkedList<String> errors = new LinkedList<String>();

      @Override
      public void setStopPipeline(boolean stop) {
        stopPipeline.set(stop);
      }

      @Override
      public Object getMonitorLock() {
        return lock;
      }

      @Override
      public String getFirstErrorMessage() {
        return errors.isEmpty() ? null : errors.getFirst();
      }

      @Override
      public boolean getEOSReached() {
        return stopPipeline.get();
      }

      @Override
      public void addErrorMessage(String message) {
        errors.add(message);
      }
    };
    return monitorObject;
  }

  /**
   * Creates new GSPipeline object that holds Pipeline and its MonitorObject.
   * 
   * @param pipeline
   * @param monitorObject
   * @return new GSPipeline object
   */
  private GSPipeline createNewGSPipeline(final Pipeline pipeline, final MonitorObject monitorObject) {
    GSPipeline gspipeline = new GSPipeline() {
      @Override
      public Pipeline getPipeline() {
        return pipeline;
      }

      @Override
      public MonitorObject getMonitorObject() {
        return monitorObject;
      }
    };
    return gspipeline;
  }

  /**
   * Used for monitoring pipeline state.
   */
  private interface MonitorObject {
    public Object getMonitorLock();

    public boolean getEOSReached();

    public void setStopPipeline(boolean reached);

    public void addErrorMessage(String message);

    public String getFirstErrorMessage();
  }

  /**
   * Container for Pipeline and MonitorObject
   */
  private interface GSPipeline {
    public Pipeline getPipeline();

    public MonitorObject getMonitorObject();
  }
}
