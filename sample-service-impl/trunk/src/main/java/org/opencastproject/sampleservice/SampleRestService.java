package org.opencastproject.sampleservice;

import org.opencastproject.rest.OpencastRestService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("/samplerest")
public class SampleRestService implements OpencastRestService {
  @GET
  @Produces(MediaType.TEXT_HTML)
  public String getSomething(@QueryParam("path") String path) {
    return "sample";
  }
}
