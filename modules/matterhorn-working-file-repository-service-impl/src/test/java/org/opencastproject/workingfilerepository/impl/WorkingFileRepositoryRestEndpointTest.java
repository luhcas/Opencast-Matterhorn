package org.opencastproject.workingfilerepository.impl;

import org.opencastproject.workingfilerepository.api.WorkingFileRepository;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.junit.Test;

import java.io.InputStream;
import java.util.Arrays;

import javax.ws.rs.core.Response;

public class WorkingFileRepositoryRestEndpointTest {

  @Test
  public void testDocumentation() {
    WorkingFileRepositoryRestEndpoint endpoint = new WorkingFileRepositoryRestEndpoint();
    String docs = endpoint.getDocumentation();
    Assert.assertTrue(docs.indexOf("<html") > -1);
  }

  @Test
  public void testExtractImageContentType() throws Exception {
    WorkingFileRepositoryRestEndpoint endpoint = new WorkingFileRepositoryRestEndpoint();

    String mediaPackageId = "mp";
    String image = "element1";

    WorkingFileRepository repo = EasyMock.createMock(WorkingFileRepository.class);
    EasyMock.expect(repo.hashMediaPackageElement(mediaPackageId, image)).andReturn("foo");
    EasyMock.expect(repo.get(mediaPackageId, image)).andReturn(getClass().getResourceAsStream("/opencast_header.gif"));
    EasyMock.expect(repo.get(mediaPackageId, image)).andReturn(getClass().getResourceAsStream("/opencast_header.gif"));
    EasyMock.replay(repo);
    endpoint.setRepository(repo);

    // execute gets, and ensure that the content types are correct
    Response response = endpoint.get(mediaPackageId, image, null);

    Assert.assertEquals("Gif content type", "image/gif", response.getMetadata().getFirst("Content-Type"));
        
    // Make sure the image byte stream was not modified by the content type detection
    InputStream in = getClass().getResourceAsStream("/opencast_header.gif");
    byte[] bytesFromClasspath = IOUtils.toByteArray(in);
    byte[] bytesFromRepo = IOUtils.toByteArray((InputStream)response.getEntity());
    Assert.assertTrue(Arrays.equals(bytesFromClasspath, bytesFromRepo));

    // Make sure the repo method(s) were called as expected
    EasyMock.verify(repo);
  }

  @Test
  public void testExtractXmlContentType() throws Exception {
    WorkingFileRepositoryRestEndpoint endpoint = new WorkingFileRepositoryRestEndpoint();

    String mediaPackageId = "mp";
    String dc = "element1";

    WorkingFileRepository repo = EasyMock.createMock(WorkingFileRepository.class);
    EasyMock.expect(repo.hashMediaPackageElement(mediaPackageId, dc)).andReturn("foo");
    EasyMock.expect(repo.get(mediaPackageId, dc)).andReturn(getClass().getResourceAsStream("/dublincore.xml"));
    EasyMock.expect(repo.get(mediaPackageId, dc)).andReturn(getClass().getResourceAsStream("/dublincore.xml"));
    EasyMock.replay(repo);
    endpoint.setRepository(repo);

    // execute gets, and ensure that the content types are correct
    Response response = endpoint.get(mediaPackageId, dc, null);

    Assert.assertEquals("Gif content type", "application/xml", response.getMetadata().getFirst("Content-Type"));
        
    // Make sure the image byte stream was not modified by the content type detection
    InputStream imageIn = getClass().getResourceAsStream("/dublincore.xml");
    byte[] imageBytesFromClasspath = IOUtils.toByteArray(imageIn);
    byte[] imageBytesFromRepo = IOUtils.toByteArray((InputStream)response.getEntity());
    Assert.assertTrue(Arrays.equals(imageBytesFromClasspath, imageBytesFromRepo));

    // Make sure the repo method(s) were called as expected
    EasyMock.verify(repo);
  }
  
  public void testEtag() throws Exception {
    WorkingFileRepositoryRestEndpoint endpoint = new WorkingFileRepositoryRestEndpoint();

    String mediaPackageId = "mp";
    String dc = "element1";
    String md5 = "foo";

    WorkingFileRepository repo = EasyMock.createNiceMock(WorkingFileRepository.class);
    EasyMock.expect(repo.hashMediaPackageElement(mediaPackageId, dc)).andReturn(md5).anyTimes();
    EasyMock.expect(repo.get(mediaPackageId, dc)).andReturn(getClass().getResourceAsStream("/dublincore.xml")).anyTimes();
    EasyMock.replay(repo);
    endpoint.setRepository(repo);

    Response response = endpoint.get(mediaPackageId, dc, "foo");
    Assert.assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), response.getStatus());
    Assert.assertNull(response.getEntity());

    response = endpoint.get(mediaPackageId, dc, "bar");
    Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    Assert.assertNotNull(response.getEntity());
  }

}
