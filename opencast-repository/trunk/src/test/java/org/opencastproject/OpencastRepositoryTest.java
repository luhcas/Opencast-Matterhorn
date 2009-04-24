package org.opencastproject;

import org.opencastproject.repository.api.OpencastRepository;
import org.opencastproject.repository.impl.OpencastRepositoryImpl;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.core.TransientRepository;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

public class OpencastRepositoryTest {

  protected static final String CONTENT1 = "Some content to store in the repository";
  protected static final String CONTENT2 = "Different content";
  protected static final String PATH = "/a/path/to/some/content";
  
  protected static TransientRepository jcrRepo;
  protected static OpencastRepository repo;

  @BeforeClass
  public static void setupOnce() throws Exception {
    jcrRepo = new TransientRepository(
        "./src/test/resources/simple-repository.xml", "./target/simple_repo/");
    repo = new OpencastRepositoryImpl(jcrRepo);
  }
  
  @Before
  public void init() throws Exception {
    // Put the content of the string into the repository as "data" (rather than metadata)
    ByteArrayInputStream in = new ByteArrayInputStream(CONTENT1.getBytes("UTF8"));
    repo.putObject(in, PATH);
  }
  
  @After
  public void destroy() throws Exception {
    // Clean up the repository
    Session session = jcrRepo.login(new SimpleCredentials("admin", "admin".toCharArray()));
    if(session.itemExists("/a")) {
      Node top = session.getRootNode().getNode("a");
      top.remove();
      session.save();
    }
  }
  
  @AfterClass
  public static void destroyOnce() throws Exception {
    // Shutdown the repository
    jcrRepo.shutdown();
  }

  @Test
  public void testReadDataInRepository() throws Exception {
    // Read the data from the repository
    InputStream streamFromRepo = repo.getObject(InputStream.class, PATH);
    String stringFromRepo = IOUtils.toString(streamFromRepo, "UTF8");

    // Ensure the repository returns the same content
    Assert.assertEquals(CONTENT1, stringFromRepo);
  }

  @Test
  public void testPutDataToExistingPath() throws Exception {
    // Put different content into the repository
    ByteArrayInputStream in = new ByteArrayInputStream(CONTENT2.getBytes("UTF8"));
    repo.putObject(in, PATH);
    
    // Read the new data from the repository
    InputStream streamFromRepo = repo.getObject(InputStream.class, PATH);
    String stringFromRepo = IOUtils.toString(streamFromRepo, "UTF8");

    // Ensure the repository returns the same content
    Assert.assertEquals(CONTENT2, stringFromRepo);
  }
  
  @Test
  public void testMetadata() throws Exception {
    repo.putMetadata("foo", "bar", PATH);
    repo.putMetadata("biz", "baz", PATH);
    
    Map<String, String> metadata = repo.getMetadata(PATH);
    Map<String, String> expected = new HashMap<String, String>();
    expected.put("bar", "foo");
    expected.put("baz", "biz");

    Assert.assertEquals(expected, metadata);
  }

}
