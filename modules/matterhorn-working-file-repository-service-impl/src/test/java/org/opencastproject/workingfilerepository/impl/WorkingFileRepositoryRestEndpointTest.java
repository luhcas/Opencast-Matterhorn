package org.opencastproject.workingfilerepository.impl;

import org.opencastproject.workingfilerepository.api.WorkingFileRepository;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.util.Arrays;
import org.easymock.EasyMock;
import org.junit.Test;

import java.io.InputStream;

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
    EasyMock.expect(repo.get(mediaPackageId, image)).andReturn(getClass().getResourceAsStream("/opencast_header.gif"));
    EasyMock.expect(repo.get(mediaPackageId, image)).andReturn(getClass().getResourceAsStream("/opencast_header.gif"));
    EasyMock.replay(repo);
    endpoint.setRepository(repo);

    // execute gets, and ensure that the content types are correct
    Response response = endpoint.get(mediaPackageId, image);

    Assert.assertEquals("Gif content type", "image/gif", response.getMetadata().getFirst("Content-Type"));
        
    // Make sure the image byte stream was not modified by the content type detection
    InputStream in = getClass().getResourceAsStream("/opencast_header.gif");
    byte[] bytesFromClasspath = IOUtils.toByteArray(in);
    byte[] bytesFromRepo = IOUtils.toByteArray((InputStream)response.getEntity());
    Assert.assertTrue(Arrays.areEqual(bytesFromClasspath, bytesFromRepo));

    // Make sure the repo method(s) were called as expected
    EasyMock.verify(repo);
  }

  @Test
  public void testExtractXmlContentType() throws Exception {
    WorkingFileRepositoryRestEndpoint endpoint = new WorkingFileRepositoryRestEndpoint();

    String mediaPackageId = "mp";
    String dc = "element1";

    WorkingFileRepository repo = EasyMock.createMock(WorkingFileRepository.class);
    EasyMock.expect(repo.get(mediaPackageId, dc)).andReturn(getClass().getResourceAsStream("/dublincore.xml"));
    EasyMock.expect(repo.get(mediaPackageId, dc)).andReturn(getClass().getResourceAsStream("/dublincore.xml"));
    EasyMock.replay(repo);
    endpoint.setRepository(repo);

    // execute gets, and ensure that the content types are correct
    Response response = endpoint.get(mediaPackageId, dc);

    Assert.assertEquals("Gif content type", "application/xml", response.getMetadata().getFirst("Content-Type"));
        
    // Make sure the image byte stream was not modified by the content type detection
    InputStream imageIn = getClass().getResourceAsStream("/dublincore.xml");
    byte[] imageBytesFromClasspath = IOUtils.toByteArray(imageIn);
    byte[] imageBytesFromRepo = IOUtils.toByteArray((InputStream)response.getEntity());
    Assert.assertTrue(Arrays.areEqual(imageBytesFromClasspath, imageBytesFromRepo));

    // Make sure the repo method(s) were called as expected
    EasyMock.verify(repo);
  }

}
