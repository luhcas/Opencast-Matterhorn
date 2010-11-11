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
package org.opencastproject.workflow.remote;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.serviceregistry.api.RemoteBase;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.WorkflowBuilder;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowQuery;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workflow.api.WorkflowSet;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * An implementation of the workflow service that communicates with a remote workflow service via HTTP.
 */
public class WorkflowServiceRemoteImpl extends RemoteBase implements WorkflowService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(WorkflowServiceRemoteImpl.class);

  public WorkflowServiceRemoteImpl() {
    super(JOB_TYPE);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#getWorkflowDefinitionById(java.lang.String)
   */
  @Override
  public WorkflowDefinition getWorkflowDefinitionById(String id) throws WorkflowDatabaseException, NotFoundException {
    HttpGet get = new HttpGet("/definition/" + id + ".xml");
    HttpResponse response = getResponse(get, HttpStatus.SC_NOT_FOUND, HttpStatus.SC_OK);
    if (HttpStatus.SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
      throw new NotFoundException("Workflow definition " + id + " does not exist.");
    } else {
      try {
        return WorkflowBuilder.getInstance().parseWorkflowDefinition(response.getEntity().getContent());
      } catch (Exception e) {
        throw new WorkflowDatabaseException(e);
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#getWorkflowById(java.lang.String)
   */
  @Override
  public WorkflowInstance getWorkflowById(String id) throws WorkflowDatabaseException, NotFoundException {
    HttpGet get = new HttpGet("/instance/" + id + ".xml");
    HttpResponse response = getResponse(get, HttpStatus.SC_NOT_FOUND, HttpStatus.SC_OK);
    if (HttpStatus.SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
      throw new NotFoundException("Workflow instance " + id + " does not exist.");
    } else {
      try {
        return WorkflowBuilder.getInstance().parseWorkflowInstance(response.getEntity().getContent());
      } catch (Exception e) {
        throw new WorkflowDatabaseException(e);
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService
   *      #getWorkflowInstances(org.opencastproject.workflow.api.WorkflowQuery)
   */
  @Override
  public WorkflowSet getWorkflowInstances(WorkflowQuery query) throws WorkflowDatabaseException {
    List<NameValuePair> queryStringParams = new ArrayList<NameValuePair>();
    if (query.getText() != null) {
      queryStringParams.add(new BasicNameValuePair("q", query.getText()));
    }
    if (query.getState() != null) {
      queryStringParams.add(new BasicNameValuePair("state", query.getState().toString()));
    }
    if (query.getState() != null) {
      queryStringParams.add(new BasicNameValuePair("series", query.getSeries()));
    }
    if (query.getMediaPackage() != null) {
      queryStringParams.add(new BasicNameValuePair("mp", query.getMediaPackage()));
    }
    if (query.getCurrentOperation() != null) {
      queryStringParams.add(new BasicNameValuePair("op", query.getCurrentOperation()));
    }
    if (query.getStartPage() > 0) {
      queryStringParams.add(new BasicNameValuePair("startPage", Long.toString(query.getStartPage())));
    }
    if (query.getCount() > 0) {
      queryStringParams.add(new BasicNameValuePair("count", Long.toString(query.getCount())));
    }

    StringBuilder url = new StringBuilder();
    url.append("/instances.xml?");
    url.append(URLEncodedUtils.format(queryStringParams, "UTF-8"));

    HttpGet get = new HttpGet(url.toString());
    HttpResponse response = getResponse(get);
    if (response == null) {
      throw new WorkflowDatabaseException("Workflow instances can not be loaded from a remote workflow service");
    } else {
      try {
        return WorkflowBuilder.getInstance().parseWorkflowSet(response.getEntity().getContent());
      } catch (Exception e) {
        throw new WorkflowDatabaseException(e);
      }
    }

  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#start(org.opencastproject.workflow.api.WorkflowDefinition,
   *      org.opencastproject.mediapackage.MediaPackage, java.util.Map)
   */
  @Override
  public WorkflowInstance start(WorkflowDefinition workflowDefinition, MediaPackage mediaPackage,
          Map<String, String> properties) throws WorkflowDatabaseException {
    try {
      return start(workflowDefinition, mediaPackage, null, properties);
    } catch (NotFoundException e) {
      throw new IllegalStateException("A null parent workflow id should never result in a not found exception ", e);
    }
  }

  /**
   * Converts a Map<String, String> to s key=value\n string, suitable for the properties form parameter expected by the
   * workflow rest endpoint.
   * 
   * @param props
   *          The map of strings
   * @return the string representation
   */
  private String mapToString(Map<String, String> props) {
    StringBuilder sb = new StringBuilder();
    for (Entry<String, String> entry : props.entrySet()) {
      sb.append(entry.getKey());
      sb.append("=");
      sb.append(entry.getValue());
      sb.append("\n");
    }
    return sb.toString();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#start(org.opencastproject.workflow.api.WorkflowDefinition,
   *      org.opencastproject.mediapackage.MediaPackage, java.lang.String, java.util.Map)
   */
  @Override
  public WorkflowInstance start(WorkflowDefinition workflowDefinition, MediaPackage mediaPackage,
          String parentWorkflowId, Map<String, String> properties) throws WorkflowDatabaseException, NotFoundException {
    String url = "/start";
    HttpPost post = new HttpPost(url);
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      if (workflowDefinition != null) {
        params.add(new BasicNameValuePair("definition", WorkflowBuilder.getInstance().toXml(workflowDefinition)));
      }
      params.add(new BasicNameValuePair("mediapackage", mediaPackage.toXml()));
      if (parentWorkflowId != null) {
        params.add(new BasicNameValuePair("parent", parentWorkflowId));
      }
      if (properties != null) {
        params.add(new BasicNameValuePair("properties", mapToString(properties)));
      }
      UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);
      post.setEntity(entity);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to assemble a remote workflow request", e);
    }
    HttpResponse response = getResponse(post, HttpStatus.SC_NOT_FOUND, HttpStatus.SC_OK);
    if (response == null) {
      throw new WorkflowDatabaseException("Unable to start a remote workflow. The http response code was unexpected.");
    } else {
      String xml = null;
      WorkflowInstance instance = null;
      try {
        xml = EntityUtils.toString(response.getEntity());
        instance = WorkflowBuilder.getInstance().parseWorkflowInstance(xml);
        return instance;
      } catch (Exception e) {
        throw new WorkflowDatabaseException("Unable to build a workflow from xml: " + xml);
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#start(org.opencastproject.workflow.api.WorkflowDefinition,
   *      org.opencastproject.mediapackage.MediaPackage)
   */
  @Override
  public WorkflowInstance start(WorkflowDefinition workflowDefinition, MediaPackage mediaPackage)
          throws WorkflowDatabaseException {
    try {
      return start(workflowDefinition, mediaPackage, null, null);
    } catch (NotFoundException e) {
      throw new IllegalStateException("A null parent workflow id should never result in a not found exception ", e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#start(org.opencastproject.mediapackage.MediaPackage,
   *      java.util.Map)
   */
  @Override
  public WorkflowInstance start(MediaPackage mediaPackage, Map<String, String> properties)
          throws WorkflowDatabaseException {
    try {
      return start(null, mediaPackage, null, properties);
    } catch (NotFoundException e) {
      throw new IllegalStateException("A null parent workflow id should never result in a not found exception ", e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#start(org.opencastproject.mediapackage.MediaPackage)
   */
  @Override
  public WorkflowInstance start(MediaPackage mediaPackage) throws WorkflowDatabaseException {
    try {
      return start(null, mediaPackage, null, null);
    } catch (NotFoundException e) {
      throw new IllegalStateException("A null parent workflow id should never result in a not found exception ", e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#countWorkflowInstances()
   */
  @Override
  public long countWorkflowInstances() throws WorkflowDatabaseException {
    return countWorkflowInstances(null, null);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#countWorkflowInstances(org.opencastproject.workflow.api.WorkflowInstance.WorkflowState,
   *      java.lang.String)
   */
  @Override
  public long countWorkflowInstances(WorkflowState state, String operation) throws WorkflowDatabaseException {
    HttpGet get = new HttpGet("/count");
    HttpParams params = get.getParams();
    if (state != null)
      params.setParameter("state", state.toString());
    if (operation != null)
      params.setParameter("operation", operation);
    HttpResponse response = getResponse(get);
    if (response == null) {
      throw new WorkflowDatabaseException("Unable to count workflow instances");
    } else {
      String body;
      try {
        body = EntityUtils.toString(response.getEntity());
      } catch (ParseException e) {
        throw new WorkflowDatabaseException("Unable to parse the response body");
      } catch (IOException e) {
        throw new WorkflowDatabaseException("Unable to parse the response body");
      }
      try {
        return Long.parseLong(body);
      } catch (NumberFormatException e) {
        throw new WorkflowDatabaseException("Unable to parse the response body as a long: " + body);
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#stop(java.lang.String)
   */
  @Override
  public void stop(String workflowInstanceId) throws WorkflowDatabaseException, NotFoundException {
    HttpPost post = new HttpPost("/stop");
    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    params.add(new BasicNameValuePair("id", workflowInstanceId));
    try {
      post.setEntity(new UrlEncodedFormEntity(params));
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("Unable to assemble a remote workflow service request", e);
    }
    HttpResponse response = getResponse(post, HttpStatus.SC_NO_CONTENT, HttpStatus.SC_NOT_FOUND);
    if (response == null) {
      throw new WorkflowDatabaseException("Unexpected HTTP response code");
    } else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
      throw new NotFoundException("Workflow instance with id='" + workflowInstanceId + "' not found");
    } else {
      logger.info("Workflow '{}' stopped", workflowInstanceId);
      return;
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#suspend(java.lang.String)
   */
  @Override
  public void suspend(String workflowInstanceId) throws WorkflowDatabaseException, NotFoundException {
    HttpPost post = new HttpPost("/suspend");
    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    params.add(new BasicNameValuePair("id", workflowInstanceId));
    try {
      post.setEntity(new UrlEncodedFormEntity(params));
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("Unable to assemble a remote workflow service request", e);
    }
    HttpResponse response = getResponse(post, HttpStatus.SC_NO_CONTENT, HttpStatus.SC_NOT_FOUND);
    if (response == null) {
      throw new WorkflowDatabaseException("Unexpected HTTP response code");
    } else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
      throw new NotFoundException("Workflow instance with id='" + workflowInstanceId + "' not found");
    } else {
      logger.info("Workflow '{}' suspended", workflowInstanceId);
      return;
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#resume(java.lang.String)
   */
  @Override
  public void resume(String workflowInstanceId) throws NotFoundException, WorkflowDatabaseException {
    resume(workflowInstanceId, null);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#resume(java.lang.String, java.util.Map)
   */
  @Override
  public void resume(String workflowInstanceId, Map<String, String> properties) throws NotFoundException,
          WorkflowDatabaseException {
    HttpPost post = new HttpPost("/resume");
    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    params.add(new BasicNameValuePair("id", workflowInstanceId));
    params.add(new BasicNameValuePair("properties", mapToString(properties)));
    try {
      post.setEntity(new UrlEncodedFormEntity(params));
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("Unable to assemble a remote workflow service request", e);
    }
    HttpResponse response = getResponse(post, HttpStatus.SC_NO_CONTENT, HttpStatus.SC_NOT_FOUND);
    if (response == null) {
      throw new WorkflowDatabaseException("Unexpected HTTP response code");
    } else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
      throw new NotFoundException("Workflow instance with id='" + workflowInstanceId + "' not found");
    } else {
      logger.info("Workflow '{}' resumed", workflowInstanceId);
      return;
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#update(org.opencastproject.workflow.api.WorkflowInstance)
   */
  @Override
  public void update(WorkflowInstance workflowInstance) throws WorkflowDatabaseException {
    String xml;
    try {
      xml = WorkflowBuilder.getInstance().toXml(workflowInstance);
    } catch (Exception e) {
      throw new IllegalStateException("unable to serialize workflow instance to xml");
    }
    HttpPost post = new HttpPost("/update");
    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    params.add(new BasicNameValuePair("workflow", xml));
    try {
      post.setEntity(new UrlEncodedFormEntity(params));
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("Unable to assemble a remote workflow service request", e);
    }
    HttpResponse response = getResponse(post, HttpStatus.SC_NO_CONTENT);
    if (response == null) {
      throw new WorkflowDatabaseException("Unexpected HTTP response code");
    } // otherwise, our work is done
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowService#listAvailableWorkflowDefinitions()
   */
  @Override
  public List<WorkflowDefinition> listAvailableWorkflowDefinitions() throws WorkflowDatabaseException {
    HttpGet get = new HttpGet("/definitions.xml");
    HttpResponse response = getResponse(get);
    if (response == null) {
      throw new WorkflowDatabaseException(
              "Unable to read the registered workflow definitions from the remote workflow service");
    } else {
      try {
        return WorkflowBuilder.getInstance().parseWorkflowDefinitions(response.getEntity().getContent());
      } catch (Exception e) {
        throw new IllegalStateException("Unable to parse workflow definitions");
      }
    }
  }

}
