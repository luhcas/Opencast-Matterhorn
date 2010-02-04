package org.opencastproject.maven;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

public class GenerateServiceMojoTest {
  GenerateServiceMojo mojo = null;
  
  @Before
  public void setup() {
    mojo = new GenerateServiceMojo();
    mojo.setServiceName("my");
  }

  @After
  public void teardown() throws Exception {
    // Clean up the newly created directories
    FileUtils.deleteDirectory(new File("opencast-my-service-api"));
    FileUtils.deleteDirectory(new File("opencast-my-service-impl"));
    mojo = null;
  }
  
  @Test
  public void testServiceGeneration() throws Exception {
    mojo.execute();
    Assert.assertTrue(new File("opencast-my-service-api/src/main/java/org/opencastproject/my/api/MyService.java").exists());
    Assert.assertTrue(new File("opencast-my-service-impl/src/main/java/org/opencastproject/my/impl/MyServiceImpl.java").exists());
  }

  @Test
  public void testForInvalidApiArtifacts() throws Exception {
    checkForInvalidArtifacts(new File("opencast-my-service-api"));
    checkForInvalidArtifacts(new File("opencast-my-service-impl"));
  }

  public void checkForInvalidArtifacts(File directory) throws Exception {
    File dotClasspath = new File(directory, ".classpath");
    File dotProject = new File(directory, ".project");
    File dotSettings = new File(directory, ".settings");
    File target = new File(directory, "target");
    
    Assert.assertFalse(dotClasspath.exists());
    Assert.assertFalse(dotProject.exists());
    Assert.assertFalse(dotSettings.exists());
    Assert.assertFalse(target.exists());
  }

}
