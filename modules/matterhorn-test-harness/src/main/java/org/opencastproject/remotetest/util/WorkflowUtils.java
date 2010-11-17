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
package org.opencastproject.remotetest.util;

import static org.opencastproject.remotetest.Main.BASE_URL;

import org.opencastproject.remotetest.Main;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

/**
 * Utility class that deals with workflows.
 */
public final class WorkflowUtils {

  /**
   * This utility class is not meant to be instantiated.
   */
  private WorkflowUtils() {
    // Nothing to do here
  }

  /**
   * Checks whether the given workflow is in the requested operation.
   * 
   * @param workflowId
   *          identifier of the workflow
   * @param operation
   *          the operation that the workflow is expected to be in
   * @return <code>true</code> if the workflow is in the expected state
   * @throws IllegalStateException
   *           if the specified workflow can't be found
   */
  public static boolean isWorkflowInOperation(String workflowId, String operation) throws IllegalStateException,
          Exception {
    HttpGet getWorkflowMethod = new HttpGet(BASE_URL + "/workflow/rest/instance/" + workflowId + ".xml");
    String workflow = EntityUtils.toString(Main.getClient().execute(getWorkflowMethod).getEntity());
    String currentOperation = (String) Utils.xPath(workflow, "/workflow/operations[/@state='RUNNING']/@id",
            XPathConstants.STRING);
    return operation.equalsIgnoreCase(currentOperation);
  }

  /**
   * Checks whether the given workflow is in the requested state.
   * 
   * @param workflowId
   *          identifier of the workflow
   * @param state
   *          the state that the workflow is expected to be in
   * @return <code>true</code> if the workflow is in the expected state
   * @throws IllegalStateException
   *           if the specified workflow can't be found
   */
  public static boolean isWorkflowInState(String workflowId, String state) throws IllegalStateException, Exception {
    HttpGet getWorkflowMethod = new HttpGet(BASE_URL + "/workflow/rest/instance/" + workflowId + ".xml");
    String workflow = EntityUtils.toString(Main.getClient().execute(getWorkflowMethod).getEntity());
    String currentState = (String) Utils.xPath(workflow, "/workflow/@state", XPathConstants.STRING);
    return state.equalsIgnoreCase(currentState);
  }

  /**
   * Parses the workflow instance represented by <code>xml</code> and extracts the workflow identifier.
   * 
   * @param xml
   *          the workflow instance
   * @return the workflow instance
   * @throws Exception
   *           if parsing fails
   */
  public static String getWorkflowInstanceId(String xml) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.parse(IOUtils.toInputStream(xml, "UTF-8"));
    return ((Element) XPathFactory.newInstance().newXPath().compile("/*").evaluate(doc, XPathConstants.NODE))
            .getAttribute("id");
  }

  /**
   * Parses the workflow instance represented by <code>xml</code> and extracts the workflow state.
   * 
   * @param xml
   *          the workflow instance
   * @return the workflow state
   * @throws Exception
   *           if parsing fails
   */
  public static String getWorkflowState(String xml) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.parse(IOUtils.toInputStream(xml, "UTF-8"));
    return ((Element) XPathFactory.newInstance().newXPath().compile("/*").evaluate(doc, XPathConstants.NODE))
            .getAttribute("state");
  }

}
