package org.opencastproject.kernel.rest.docs;

import org.opencastproject.kernel.rest.docs.RestParameter;
import org.opencastproject.kernel.rest.docs.RestQuery;
import org.opencastproject.kernel.rest.docs.RestResponse;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;

/**
 * This test class tests the functionality of @RestQuery annotation type.
 */
public class RestDocsAnnotationTest {
  @Test
  public void testRestQueryDocs() {
    Method testMethod;
    try {
      testMethod = TestServletSample.class.getMethod("methodA");
      if (testMethod != null)
      {
        RestQuery annotation = (RestQuery) testMethod.getAnnotation(RestQuery.class);
        
        Assert.assertEquals("Starts a capture using the default devices as appropriate.", annotation.description());      
        Assert.assertEquals("A list of capture agent things", annotation.returnDescription());

        Assert.assertTrue(annotation.pathParameters().length == 1);
        Assert.assertEquals("location", annotation.pathParameters()[0].name());
        Assert.assertEquals("The room of the capture agent", annotation.pathParameters()[0].description());
        Assert.assertFalse(annotation.pathParameters()[0].isRequired());
        
        Assert.assertTrue(annotation.queryParameters().length == 1);
        Assert.assertEquals("id", annotation.queryParameters()[0].name());
        Assert.assertEquals("The ID of the capture to start", annotation.queryParameters()[0].description());
        Assert.assertTrue(annotation.queryParameters()[0].isRequired());        
        
        Assert.assertTrue(annotation.reponses().length == 2);

        Assert.assertEquals(200, annotation.reponses()[0].responseCode());
        Assert.assertEquals("When the capture started correctly", annotation.reponses()[0].description());
        
        Assert.assertEquals(400, annotation.reponses()[1].responseCode());
        Assert.assertEquals("When there are no media devices", annotation.reponses()[1].description());
      }
    } catch (SecurityException e) {
      Assert.fail();
    } catch (NoSuchMethodException e) {
      Assert.fail();
    }
  }

  /**
   * This sample class simulates a annotated REST service class.
   */
  private class TestServletSample{
   
    @SuppressWarnings("unused")
    @RestQuery(
            description = "Starts a capture using the default devices as appropriate.",
            returnDescription = "A list of capture agent things",
            pathParameters = { @RestParameter(name = "location", description = "The room of the capture agent", isRequired = false) }, 
            queryParameters = { @RestParameter(name = "id", description = "The ID of the capture to start", isRequired = true) }, 
            reponses = { @RestResponse(responseCode = 200, description = "When the capture started correctly"),
                         @RestResponse(responseCode = 400, description = "When there are no media devices") }
            )            
    public int methodA()
    {
      return 0;
    }

  }
}