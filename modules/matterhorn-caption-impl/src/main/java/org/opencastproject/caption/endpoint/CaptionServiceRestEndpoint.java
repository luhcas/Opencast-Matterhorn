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
package org.opencastproject.caption.endpoint;

import org.opencastproject.caption.api.CaptionService;
import org.opencastproject.util.DocUtil;
import org.opencastproject.util.doc.DocRestData;
import org.opencastproject.util.doc.Format;
import org.opencastproject.util.doc.Param;
import org.opencastproject.util.doc.RestEndpoint;
import org.opencastproject.util.doc.RestTestForm;
import org.opencastproject.util.doc.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Rest endpoint for {@link CaptionService}.
 * 
 */
@Path("/")
public class CaptionServiceRestEndpoint {

  protected CaptionService service;
  protected String docs;

  private static final Logger logger = LoggerFactory.getLogger(CaptionServiceRestEndpoint.class);

  public void setCaptionService(CaptionService service) {
    this.service = service;
  }

  public void unsetCaptionService(CaptionService service) {
    this.service = null;
  }

  @POST
  @Path("convert")
  @Produces(MediaType.TEXT_PLAIN)
  public Response convert(@FormParam("input") String inputType, @FormParam("output") String outputType,
          @FormParam("captions") String input) {
    try {
      String output;
      if (inputType == null || inputType.equals("")) {
        output = service.convert(input, outputType);
      } else {
        output = service.convert(input, inputType, outputType);
      }
      return Response.ok().entity(output).build();
    } catch (Exception e) {
      logger.error(e.getMessage());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Generates docs.
   * 
   * @return Doc string
   */
  protected String generateDocs() {
    DocRestData data = new DocRestData("Caption", "Caption Service", "/caption/rest", null);
    data.setAbstract("This service enables conversion from one caption format to another.");

    RestEndpoint convertEndpoint = new RestEndpoint("convert", RestEndpoint.Method.POST, "/convert",
            "Convert captions from one format to another");
    convertEndpoint.addFormat(new Format("plaintext", null, null));
    convertEndpoint.addStatus(Status.OK("Conversion successfully completed."));
    convertEndpoint.addRequiredParam(new Param("captions", Param.Type.TEXT, null, "Captions to be converted."));
    convertEndpoint.addOptionalParam(new Param("input", Param.Type.STRING, null, "Caption input format (for example: DFXP, SubRip,...)."));
    convertEndpoint.addRequiredParam(new Param("output", Param.Type.STRING, null, "Caption output format (for example: DFXP, SubRip,...)."));
    convertEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, convertEndpoint);
    return DocUtil.generate(data);
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocumentation() {
    if (docs == null) {
      docs = generateDocs();
    }
    return docs;
  }
}
