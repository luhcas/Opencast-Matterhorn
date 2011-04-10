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
package org.opencastproject.kernel.security;

import org.opencastproject.rest.docs.RestQuery;
import org.opencastproject.rest.docs.RestResponse;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.doc.rest.RestService;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Provides access to the organizations served by this Matterhorn instance.
 */
@Path("/")
@RestService(name = "organization", title = "Organizations", notes = { "" }, abstractText = "Displays the organizations served by this system")
public class OrganizationEndpoint {

  /** The organization directory */
  protected OrganizationDirectoryService orgDirectoryService = null;

  @GET
  @Path("/all.xml")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(description = "Lists the organizations as xml", returnDescription = "The list of org as xml", pathParameters = {}, queryParameters = {}, reponses = { @RestResponse(responseCode = 200, description = "Organizations returned") })
  public List<Organization> getOrganizationsAsXml() {
    return orgDirectoryService.getOrganizations();
  }

  @GET
  @Path("/all.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(description = "Lists the organizations as a json array", returnDescription = "The list of org as a json array", pathParameters = {}, queryParameters = {}, reponses = { @RestResponse(responseCode = 200, description = "Organizations returned") })
  public List<Organization> getOrganizationsAsJson() {
    return getOrganizationsAsXml();
  }

  @GET
  @Path("/{id}.xml")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(description = "Gets an organizations as xml", returnDescription = "The org as xml", pathParameters = {}, queryParameters = {}, reponses = {
          @RestResponse(responseCode = 200, description = "Organization returned"),
          @RestResponse(responseCode = 404, description = "No organization with this identifier found") })
  public Organization getOrganizationAsXml(@PathParam("id") String id) {
    try {
      return orgDirectoryService.getOrganization(id);
    } catch (NotFoundException e) {
      return null;
    }
  }

  @GET
  @Path("/{id}.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(description = "Gets an organizations as json", returnDescription = "The org as json", pathParameters = {}, queryParameters = {}, reponses = {
          @RestResponse(responseCode = 200, description = "Organization returned"),
          @RestResponse(responseCode = 404, description = "No organization with this identifier found") })
  public Organization getOrganizationAsJson(@PathParam("id") String id) {
    return getOrganizationAsXml(id);
  }

  /**
   * @param orgDirectoryService
   *          the orgDirectoryService to set
   */
  public void setOrgDirectoryService(OrganizationDirectoryService orgDirectoryService) {
    this.orgDirectoryService = orgDirectoryService;
  }
}
