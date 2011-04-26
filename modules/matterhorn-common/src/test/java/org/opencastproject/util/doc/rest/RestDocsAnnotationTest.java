package org.opencastproject.util.doc.rest;

import org.junit.Assert;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * This test class tests the functionality of annotations used for documenting REST endpoints.
 */
public class RestDocsAnnotationTest {

  /**
   * This tests the functionality of @RestService annotation type.
   */
  @Test
  public void testRestServiceDocs() {
    RestService restServiceAnnotation = TestServletSample.class.getAnnotation(RestService.class);

    // name, title and abstract text
    Assert.assertEquals("ingestservice", restServiceAnnotation.name());
    Assert.assertEquals("Ingest Service", restServiceAnnotation.title());
    Assert.assertEquals(
            "This service creates and augments Matterhorn media packages that include media tracks, metadata catalogs and attachments.",
            restServiceAnnotation.abstractText());

    // notes
    Assert.assertEquals(2, restServiceAnnotation.notes().length);
    Assert.assertEquals(
            "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
            restServiceAnnotation.notes()[0]);
    Assert.assertEquals(
            "If the service is down or not working it will return a status 503, this means the the underlying service is not working and is either restarting or has failed",
            restServiceAnnotation.notes()[1]);

  }

  /**
   * This tests the functionality of @RestQuery, @RestParameter, @RestResponse, @Path, @Produces, @Consumes annotation
   * type.
   */
  @Test
  public void testRestQueryDocs() {
    Method testMethod;
    try {
      testMethod = TestServletSample.class.getMethod("methodA");
      if (testMethod != null) {
        RestQuery restQueryAnnotation = (RestQuery) testMethod.getAnnotation(RestQuery.class);
        Path pathAnnotation = (Path) testMethod.getAnnotation(Path.class);
        Produces producesAnnotation = (Produces) testMethod.getAnnotation(Produces.class);
        Consumes consumesAnnotation = (Consumes) testMethod.getAnnotation(Consumes.class);

        Assert.assertEquals(1, producesAnnotation.value().length);
        Assert.assertEquals(MediaType.TEXT_XML, producesAnnotation.value()[0]);

        Assert.assertEquals(1, consumesAnnotation.value().length);
        Assert.assertEquals(MediaType.MULTIPART_FORM_DATA, consumesAnnotation.value()[0]);

        Assert.assertEquals("addTrack", pathAnnotation.value());

        // we cannot hard code the GET.class or POST.class because we don't know which one is used.
        for (Annotation a : testMethod.getAnnotations()) {
          HttpMethod method = (HttpMethod) a.annotationType().getAnnotation(HttpMethod.class);
          if (method != null) {
            Assert.assertEquals("POST", a.annotationType().getSimpleName());
            Assert.assertEquals("POST", method.value());
          }
        }

        // name, description and return description
        Assert.assertEquals("addTrackInputStream", restQueryAnnotation.name());
        Assert.assertEquals("Add a media track to a given media package using an input stream",
                restQueryAnnotation.description());
        Assert.assertEquals("augmented media package", restQueryAnnotation.returnDescription());

        // path parameter
        Assert.assertTrue(restQueryAnnotation.pathParameters().length == 1);
        Assert.assertEquals("wdID", restQueryAnnotation.pathParameters()[0].name());
        Assert.assertEquals("Workflow definition id", restQueryAnnotation.pathParameters()[0].description());
        Assert.assertTrue(restQueryAnnotation.pathParameters()[0].isRequired());
        Assert.assertEquals("", restQueryAnnotation.pathParameters()[0].defaultValue());
        Assert.assertEquals(RestParameter.Type.STRING, restQueryAnnotation.pathParameters()[0].type());

        // query parameters
        Assert.assertTrue(restQueryAnnotation.restParameters().length == 2);
        // #1
        Assert.assertEquals("flavor", restQueryAnnotation.restParameters()[0].name());
        Assert.assertEquals("The kind of media track", restQueryAnnotation.restParameters()[0].description());
        Assert.assertTrue(restQueryAnnotation.restParameters()[0].isRequired());
        Assert.assertEquals("Default", restQueryAnnotation.restParameters()[0].defaultValue());
        Assert.assertEquals(RestParameter.Type.STRING, restQueryAnnotation.restParameters()[0].type());
        // #2
        Assert.assertEquals("mediaPackage", restQueryAnnotation.restParameters()[1].name());
        Assert.assertEquals("The media package as XML", restQueryAnnotation.restParameters()[1].description());
        Assert.assertFalse(restQueryAnnotation.restParameters()[1].isRequired());
        Assert.assertEquals("", restQueryAnnotation.restParameters()[1].defaultValue());
        Assert.assertEquals(RestParameter.Type.TEXT, restQueryAnnotation.restParameters()[1].type());

        // body parameter
        Assert.assertEquals("BODY", restQueryAnnotation.bodyParameter().name());
        Assert.assertEquals("The media track file", restQueryAnnotation.bodyParameter().description());
        Assert.assertTrue(restQueryAnnotation.bodyParameter().isRequired());
        Assert.assertEquals("", restQueryAnnotation.bodyParameter().defaultValue());
        Assert.assertEquals(RestParameter.Type.FILE, restQueryAnnotation.bodyParameter().type());

        // responses
        Assert.assertTrue(restQueryAnnotation.reponses().length == 3);

        Assert.assertEquals(HttpServletResponse.SC_OK, restQueryAnnotation.reponses()[0].responseCode());
        Assert.assertEquals("Returns augmented media package", restQueryAnnotation.reponses()[0].description());

        Assert.assertEquals(HttpServletResponse.SC_BAD_REQUEST, restQueryAnnotation.reponses()[1].responseCode());
        Assert.assertEquals("", restQueryAnnotation.reponses()[1].description());

        Assert.assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                restQueryAnnotation.reponses()[2].responseCode());
        Assert.assertEquals("", restQueryAnnotation.reponses()[2].description());

      }
    } catch (SecurityException e) {
      Assert.fail();
    } catch (NoSuchMethodException e) {
      Assert.fail();
    }
  }

  /**
   * This tests the functionality of @RestQuery, @RestParameter, @RestResponse, @Path, @Produces, @Consumes annotation
   * type using a different technique.
   */
  @Test
  public void testRestQueryDocs2() {
    Method testMethod;
    try {
      testMethod = TestServletSample.class.getMethod("methodA");
      if (testMethod != null) {
        RestDocData restDocData = new RestDocData("NAME", "TITLE", "URL", null, new TestServletSample(),
                new HashMap<String, String>());

        RestQuery restQueryAnnotation = (RestQuery) testMethod.getAnnotation(RestQuery.class);
        Path pathAnnotation = (Path) testMethod.getAnnotation(Path.class);
        Produces producesAnnotation = (Produces) testMethod.getAnnotation(Produces.class);

        String httpMethodString = null;

        // we cannot hard code the GET.class or POST.class because we don't know which one is used.
        for (Annotation a : testMethod.getAnnotations()) {
          HttpMethod method = (HttpMethod) a.annotationType().getAnnotation(HttpMethod.class);
          if (method != null) {
            httpMethodString = method.value();
          }
        }

        RestEndpointData endpoint = new RestEndpointData(testMethod.getReturnType(),
                restDocData.processMacro(restQueryAnnotation.name()), httpMethodString, "/" + pathAnnotation.value(),
                restDocData.processMacro(restQueryAnnotation.description()));
        if (!restQueryAnnotation.returnDescription().isEmpty()) {
          endpoint.addNote("Return value description: "
                  + restDocData.processMacro(restQueryAnnotation.returnDescription()));
        }

        // name, description and return description
        Assert.assertEquals("addTrackInputStream", endpoint.getName());
        Assert.assertEquals("Add a media track to a given media package using an input stream",
                endpoint.getDescription());
        Assert.assertEquals(1, endpoint.getNotes().size());
        Assert.assertEquals("Return value description: augmented media package", endpoint.getNotes().get(0));

        // HTTP method
        Assert.assertEquals("POST", endpoint.getMethod());
        Assert.assertEquals("/addTrack", endpoint.getPath());

        // @Produces
        if (producesAnnotation != null) {
          for (String format : producesAnnotation.value()) {
            endpoint.addFormat(new RestFormatData(format));
          }
        }
        Assert.assertEquals(1, endpoint.getFormats().size());
        Assert.assertEquals(MediaType.TEXT_XML, endpoint.getFormats().get(0).getName());

        // responses
        for (RestResponse restResp : restQueryAnnotation.reponses()) {
          endpoint.addStatus(restResp, restDocData);
        }
        Assert.assertEquals(3, endpoint.getStatuses().size());

        Assert.assertEquals(HttpServletResponse.SC_OK, endpoint.getStatuses().get(0).getCode());
        Assert.assertEquals("Returns augmented media package", endpoint.getStatuses().get(0).getDescription());

        Assert.assertEquals(HttpServletResponse.SC_BAD_REQUEST, endpoint.getStatuses().get(1).getCode());
        Assert.assertEquals(null, endpoint.getStatuses().get(1).getDescription());

        Assert.assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, endpoint.getStatuses().get(2).getCode());
        Assert.assertEquals(null, endpoint.getStatuses().get(2).getDescription());

        // body parameter
        if (restQueryAnnotation.bodyParameter().type() != RestParameter.Type.NO_PARAMETER) {
          endpoint.addBodyParam(restQueryAnnotation.bodyParameter(), restDocData);
        }
        Assert.assertEquals("BODY", endpoint.getBodyParam().getName());
        Assert.assertEquals("The media track file", endpoint.getBodyParam().getDescription());
        Assert.assertTrue(endpoint.getBodyParam().isRequired());
        Assert.assertEquals(null, endpoint.getBodyParam().getDefaultValue());
        Assert.assertTrue("FILE".equalsIgnoreCase(endpoint.getBodyParam().getType()));

        // path parameter
        for (RestParameter restParam : restQueryAnnotation.pathParameters()) {
          endpoint.addPathParam(new RestParamData(restParam, restDocData));
        }
        Assert.assertEquals(1, endpoint.getPathParams().size());
        Assert.assertEquals("wdID", endpoint.getPathParams().get(0).getName());
        Assert.assertEquals("Workflow definition id", endpoint.getPathParams().get(0).getDescription());
        Assert.assertTrue(endpoint.getPathParams().get(0).isRequired());
        Assert.assertTrue(endpoint.getPathParams().get(0).isPath());
        Assert.assertEquals(null, endpoint.getPathParams().get(0).getDefaultValue());
        Assert.assertTrue("STRING".equalsIgnoreCase(endpoint.getPathParams().get(0).getType()));

        // query parameters
        for (RestParameter restParam : restQueryAnnotation.restParameters()) {
          if (restParam.isRequired()) {
            endpoint.addRequiredParam(new RestParamData(restParam, restDocData));
          } else {
            endpoint.addOptionalParam(new RestParamData(restParam, restDocData));
          }
        }
        // #1
        Assert.assertEquals(1, endpoint.getRequiredParams().size());
        Assert.assertEquals("flavor", endpoint.getRequiredParams().get(0).getName());
        Assert.assertEquals("The kind of media track", endpoint.getRequiredParams().get(0).getDescription());
        Assert.assertTrue(endpoint.getRequiredParams().get(0).isRequired());
        Assert.assertEquals("Default", endpoint.getRequiredParams().get(0).getDefaultValue());
        Assert.assertTrue("STRING".equalsIgnoreCase(endpoint.getRequiredParams().get(0).getType()));

        // #2
        Assert.assertEquals(1, endpoint.getOptionalParams().size());
        Assert.assertEquals("mediaPackage", endpoint.getOptionalParams().get(0).getName());
        Assert.assertEquals("The media package as XML", endpoint.getOptionalParams().get(0).getDescription());
        Assert.assertFalse(endpoint.getOptionalParams().get(0).isRequired());
        Assert.assertEquals(null, endpoint.getOptionalParams().get(0).getDefaultValue());
        Assert.assertTrue("TEXT".equalsIgnoreCase(endpoint.getOptionalParams().get(0).getType()));

      }
    } catch (SecurityException e) {
      Assert.fail();
    } catch (NoSuchMethodException e) {
      Assert.fail();
    }
  }

  @Test
  public void testRestQueryDocsMacros() {
    Method testMethod = null;
    Map<String, String> map = new HashMap<String, String>();
    map.put("somethingElse", "the value of something else");
    map.put("anotherthing", "the value of another thing");
    try {
      testMethod = TestServletSample.class.getMethod("methodB");
      if (testMethod != null) {
        RestQuery restQueryAnnotation = (RestQuery) testMethod.getAnnotation(RestQuery.class);
        RestDocData restDocData = new RestDocData("NAME", "TITLE", "URL", null, new TestServletSample(), map);

        Assert.assertEquals(restDocData.processMacro(restQueryAnnotation.restParameters()[1].defaultValue()),
                "ADCD THIS IS SCHEMA XUHZSUFH the value of something else UGGUH the value of another thing AIHID");
      }
    } catch (SecurityException e) {
      Assert.fail();
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
      Assert.fail();
    }
  }

  /**
   * This sample class simulates a annotated REST service class.
   */
  @RestService(name = "ingestservice", title = "Ingest Service", notes = {
          "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
          "If the service is down or not working it will return a status 503, this means the the underlying service is not working and is either restarting or has failed" }, abstractText = "This service creates and augments Matterhorn media packages that include media tracks, metadata catalogs and attachments.")
  public class TestServletSample {

    @POST
    @Produces(MediaType.TEXT_XML)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("addTrack")
    @RestQuery(name = "addTrackInputStream", description = "Add a media track to a given media package using an input stream", pathParameters = { @RestParameter(defaultValue = "", description = "Workflow definition id", isRequired = true, name = "wdID", type = RestParameter.Type.STRING) }, restParameters = {
            @RestParameter(defaultValue = "Default", description = "The kind of media track", isRequired = true, name = "flavor", type = RestParameter.Type.STRING),
            @RestParameter(defaultValue = "", description = "The media package as XML", isRequired = false, name = "mediaPackage", type = RestParameter.Type.TEXT) }, bodyParameter = @RestParameter(defaultValue = "", description = "The media track file", isRequired = true, name = "BODY", type = RestParameter.Type.FILE), reponses = {
            @RestResponse(description = "Returns augmented media package", responseCode = HttpServletResponse.SC_OK),
            @RestResponse(description = "", responseCode = HttpServletResponse.SC_BAD_REQUEST),
            @RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "augmented media package")
    public int methodA() {
      return 0;
    }

    @POST
    @Produces(MediaType.TEXT_XML)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("addTrack")
    @RestQuery(name = "addTrackInputStream", description = "Add a media track to a given media package using an input stream", pathParameters = { @RestParameter(defaultValue = "", description = "Workflow definition id", isRequired = true, name = "wdID", type = RestParameter.Type.STRING) }, restParameters = {
            @RestParameter(defaultValue = "Default", description = "The kind of media track", isRequired = true, name = "flavor", type = RestParameter.Type.STRING),
            @RestParameter(defaultValue = "ADCD ${this.schema} XUHZSUFH ${somethingElse} UGGUH ${anotherthing} AIHID", description = "The media package as XML", isRequired = false, name = "mediaPackage", type = RestParameter.Type.TEXT) }, bodyParameter = @RestParameter(defaultValue = "", description = "The media track file", isRequired = true, name = "BODY", type = RestParameter.Type.FILE), reponses = {
            @RestResponse(description = "Returns augmented media package", responseCode = HttpServletResponse.SC_OK),
            @RestResponse(description = "", responseCode = HttpServletResponse.SC_BAD_REQUEST),
            @RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "augmented media package")
    public int methodB() {
      return 0;
    }

    public String getSchema() {
      return "THIS IS SCHEMA";
    }
  }

}
