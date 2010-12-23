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
package org.opencastproject.capture.endpoint;

import org.opencastproject.capture.admin.api.RecordingStateUpdate;
import org.opencastproject.capture.api.AgentRecording;
import org.opencastproject.capture.api.StateService;
import org.opencastproject.rest.RestPublisher;
import org.opencastproject.util.DocUtil;
import org.opencastproject.util.doc.DocRestData;
import org.opencastproject.util.doc.Format;
import org.opencastproject.util.doc.RestEndpoint;
import org.opencastproject.util.doc.RestTestForm;
import org.opencastproject.util.doc.Status;

import org.osgi.service.component.ComponentContext;

import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * The REST endpoint for the state service on the capture device
 */
@Path("/")
public class StateRestService {

  private StateService service;

  /**
   * Callback from OSGi that is called when this service is activated.
   * 
   * @param cc
   *          OSGi component context
   */
  public void activate(ComponentContext cc) {
    String serviceUrl = (String) cc.getProperties().get(RestPublisher.SERVICE_PATH_PROPERTY);
    docs = generateDocs(serviceUrl);
  }

  /**
   * Set {@link org.opencastproject.capture.api.StateService} service.
   * 
   * @param service
   *          Service implemented {@link org.opencastproject.capture.api.StateService}
   */
  public void setService(StateService service) {
    this.service = service;
  }

  /**
   * Set {@link org.opencastproject.capture.api.StateService} service.
   * 
   * @param service
   *          Service implemented {@link org.opencastproject.capture.api.StateService}
   */
  public void unsetService(StateService service) {
    this.service = null;
  }

  /**
   * Gets the state of the agent from the state service
   * 
   * @return String The state of the of the agent. Will be defined in AgentState
   * @see org.opencastproject.capture.admin.api.AgentState
   */
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("state")
  public String getState() {
    if (service != null) {
      return this.service.getAgentState();
    } else {
      return "Server Error";
    }
  }

  /**
   * Gets the list of recording that the capture agent knows about.
   * 
   * @return Response The method outputs the list to the response's output stream
   */
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("recordings")
  public LinkedList<RecordingStateUpdate> getRecordings() {
    if (service == null) {
      Response r = Response.status(Response.Status.SERVICE_UNAVAILABLE)
              .entity("State Service is unavailable, please wait...").build();
      throw new WebApplicationException(r);
    }

    LinkedList<RecordingStateUpdate> update = new LinkedList<RecordingStateUpdate>();
    Map<String, AgentRecording> data = service.getKnownRecordings();
    // Run through and build a map of updates (rather than states)
    for (Entry<String, AgentRecording> e : data.entrySet()) {
      update.add(new RecordingStateUpdate(e.getValue()));
    }

    return update;
  }

  /**
   * Gets the documentation for the service.
   * 
   * @return Response The method outputs the list to the response's output stream
   */
  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public Response getDocumentation() {
    return Response.ok(docs).build();
  }

  protected String docs;
  private String[] notes = {
          "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
          "If the service is down or not working it will return a status 503, this means the the underlying service"
                  + " is not working and is either restarting or has failed",
          "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated."
                  + " In other words, there is a bug! You should file an error report with your server logs from the time when"
                  + " the error occurred: <a href=\"https://issues.opencastproject.org\">Opencast Issue Tracker</a>", };

  private String generateDocs(String serviceUrl) {
    DocRestData data = new DocRestData("stateservice", "State Service", serviceUrl, notes);

    // getState
    RestEndpoint endpoint = new RestEndpoint("state", RestEndpoint.Method.GET, "/state",
            "Return the state of the capture agent");
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addStatus(Status.OK(null));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, endpoint);

    // getRecordings
    endpoint = new RestEndpoint("recordings", RestEndpoint.Method.GET, "/recordings",
            "Return a list of the capture agent's recordings");
    endpoint.addFormat(new Format("XML", null, null));
    endpoint.addStatus(Status.OK(null));
    endpoint.addStatus(Status.SERVICE_UNAVAILABLE("State Service is unavailable"));
    endpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, endpoint);

    return DocUtil.generate(data);
  }

  public StateRestService() {
  }

}
