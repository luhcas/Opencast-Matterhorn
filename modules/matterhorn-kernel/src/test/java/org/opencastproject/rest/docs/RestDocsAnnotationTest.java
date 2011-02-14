package org.opencastproject.rest.docs;

import org.opencastproject.kernel.rest.docs.RestQuery;
import org.opencastproject.kernel.rest.docs.RestMethods;
import org.opencastproject.kernel.rest.docs.RestFormats;

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
        RestFormats[] expectedReturnFormats = {RestFormats.XML, RestFormats.JSON};
        RestMethods[] expectedMethods = {RestMethods.GET, RestMethods.POST};
        
        RestQuery annotation = (RestQuery) testMethod.getAnnotation(RestQuery.class);
        
        Assert.assertEquals("Starts a capture using the default devices as appropriate.", annotation.description());      
        Assert.assertArrayEquals(expectedReturnFormats, annotation.returnFormats());
        Assert.assertEquals("A list of capture agent things", annotation.returnDescription());
        Assert.assertArrayEquals(expectedMethods, annotation.methods());
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
   
    @RestQuery(
            description = "Starts a capture using the default devices as appropriate.",
            returnFormats = {RestFormats.XML, RestFormats.JSON},
            returnDescription = "A list of capture agent things",
            methods = {RestMethods.GET, RestMethods.POST})
    public int methodA()
    {
      return 0;
    }

  }
}