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
package org.opencastproject.workflow.api;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Provides a mechanism to un/marshall workflow instances and definitions to/from xml.
 */
public final class WorkflowParser {

  private static final JAXBContext jaxbContext;

  static {
    StringBuilder sb = new StringBuilder();
    sb.append("org.opencastproject.mediapackage");
    sb.append(":org.opencastproject.workflow.api");
    try {
      jaxbContext = JAXBContext.newInstance(sb.toString(), WorkflowParser.class.getClassLoader());
    } catch (JAXBException e) {
      throw new IllegalStateException(e);
    }
  }

  /** Disallow instantiating this class */
  private WorkflowParser() {
  }

  /**
   * Loads workflow definitions from the given input stream.
   * 
   * @param in
   * @return the list of workflow definitions
   */
  public static List<WorkflowDefinition> parseWorkflowDefinitions(InputStream in) throws Exception {
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
    WorkflowDefinitionImpl[] impls = unmarshaller.unmarshal(
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in), WorkflowDefinitionImpl[].class)
            .getValue();
    List<WorkflowDefinition> list = new ArrayList<WorkflowDefinition>();
    for (WorkflowDefinitionImpl impl : impls) {
      list.add(impl);
    }
    return list;
  }

  /**
   * Loads a workflow definition from the given input stream.
   * 
   * @param in
   *          the input stream
   * @return the workflow definition
   * @throws Exception
   *           if creating the workflow definition fails
   */
  public static WorkflowDefinition parseWorkflowDefinition(InputStream in) throws Exception {
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
    return unmarshaller.unmarshal(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in),
            WorkflowDefinitionImpl.class).getValue();
  }

  /**
   * Loads a workflow definition from the xml stream.
   * 
   * @param in
   *          xml stream of the workflow definition
   * @return the workflow definition
   * @throws Exception
   *           if creating the workflow definition fails
   */
  public static WorkflowDefinition parseWorkflowDefinition(String in) throws Exception {
    return parseWorkflowDefinition(IOUtils.toInputStream(in, "UTF8"));
  }

  /**
   * Loads a workflow instance from the given input stream.
   * 
   * @param in
   *          the input stream
   * @return the workflow instance
   * @throws Exception
   *           if creating the workflow instance fails
   */
  public static WorkflowInstance parseWorkflowInstance(InputStream in) throws Exception {
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
    WorkflowInstanceImpl workflow = unmarshaller.unmarshal(
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in), WorkflowInstanceImpl.class).getValue();
    workflow.init();
    return workflow;
  }

  /**
   * Loads a workflow instance from the xml stream.
   * 
   * @param in
   *          xml stream of the workflow instance
   * @return the workflow instance
   * @throws Exception
   *           if creating the workflow instance fails
   */
  public static WorkflowInstance parseWorkflowInstance(String in) throws Exception {
    return parseWorkflowInstance(IOUtils.toInputStream(in, "UTF8"));
  }

  /**
   * Loads workflow statistics from the given input stream.
   * 
   * @param in
   *          the input stream
   * @return the workflow statistics
   * @throws Exception
   *           if creating the workflow statistics fails
   */
  public static WorkflowStatistics parseWorkflowStatistics(InputStream in) throws Exception {
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
    WorkflowStatistics stats = unmarshaller.unmarshal(
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in), WorkflowStatistics.class).getValue();
    return stats;
  }

  /**
   * Loads workflow statistics from the given xml string.
   * 
   * @param xml
   *          the xml serialized representation of the workflow statistics
   * @return the workflow statistics
   * @throws Exception
   *           if creating the workflow statistics fails
   */
  public static WorkflowStatistics parseWorkflowStatistics(String xml) throws Exception {
    return parseWorkflowStatistics(IOUtils.toInputStream(xml, "UTF8"));
  }

  /**
   * Loads a set of workflow instances from the given input stream.
   * 
   * @param in
   *          the input stream
   * @return the set of workflow instances
   * @throws Exception
   *           if creating the workflow instance set fails
   */
  public static WorkflowSet parseWorkflowSet(InputStream in) throws Exception {
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
    WorkflowSetImpl workflowSet = unmarshaller.unmarshal(
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in), WorkflowSetImpl.class).getValue();
    return workflowSet;
  }

  /**
   * Loads a set of workflow instances from the xml string.
   * 
   * @param in
   *          xml string of the workflow instance set
   * @return the workflow set
   * @throws Exception
   *           if creating the workflow instance set fails
   */
  public static WorkflowSet parseWorkflowSet(String in) throws Exception {
    return parseWorkflowSet(IOUtils.toInputStream(in, "UTF8"));
  }

  public static String toXml(WorkflowInstance workflowInstance) throws Exception {
    Marshaller marshaller = jaxbContext.createMarshaller();
    Writer writer = new StringWriter();
    marshaller.marshal(workflowInstance, writer);
    return writer.toString();
  }

  public static String toXml(WorkflowDefinition workflowDefinition) throws Exception {
    Marshaller marshaller = jaxbContext.createMarshaller();
    Writer writer = new StringWriter();
    marshaller.marshal(workflowDefinition, writer);
    return writer.toString();
  }

  public static String toXml(List<WorkflowDefinition> list) throws Exception {
    Marshaller marshaller = jaxbContext.createMarshaller();
    Writer writer = new StringWriter();
    marshaller.marshal(new WorkflowDefinitionSet(list), writer);
    return writer.toString();
  }

  public static String toXml(WorkflowSet set) throws Exception {
    Marshaller marshaller = jaxbContext.createMarshaller();
    Writer writer = new StringWriter();
    marshaller.marshal(set, writer);
    return writer.toString();
  }

  public static WorkflowOperationResult buildWorkflowOperationResult(MediaPackage mediaPackage, Action action,
          long timeInQueue) {
    return buildWorkflowOperationResult(mediaPackage, null, action, timeInQueue);
  }

  public static WorkflowOperationResult buildWorkflowOperationResult(MediaPackage mediaPackage, Action action) {
    return buildWorkflowOperationResult(mediaPackage, null, action, 0);
  }

  public static WorkflowOperationResult buildWorkflowOperationResult(Action action, long timeInQueue) {
    return buildWorkflowOperationResult(null, null, action, timeInQueue);
  }

  public static WorkflowOperationResult buildWorkflowOperationResult(Action action) {
    return buildWorkflowOperationResult(null, null, action, 0);
  }

  public static WorkflowOperationResult buildWorkflowOperationResult(MediaPackage mediaPackage,
          Map<String, String> properties, Action action, long timeInQueue) {
    return new WorkflowOperationResultImpl(mediaPackage, properties, action, timeInQueue);
  }

  public static String toXml(WorkflowStatistics stats) throws Exception {
    Marshaller marshaller = jaxbContext.createMarshaller();
    Writer writer = new StringWriter();
    marshaller.marshal(stats, writer);
    return writer.toString();
  }

}
