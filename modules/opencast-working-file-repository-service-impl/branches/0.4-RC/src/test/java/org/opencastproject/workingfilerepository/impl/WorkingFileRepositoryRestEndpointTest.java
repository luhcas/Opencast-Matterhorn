package org.opencastproject.workingfilerepository.impl;

import junit.framework.Assert;

import org.junit.Test;

public class WorkingFileRepositoryRestEndpointTest {

  @Test
  public void testDocumentation() {
    WorkingFileRepositoryRestEndpoint endpoint = new WorkingFileRepositoryRestEndpoint();
    String docs = endpoint.getDocumentation();
    Assert.assertTrue(docs.startsWith("<html>"));
  }
}
