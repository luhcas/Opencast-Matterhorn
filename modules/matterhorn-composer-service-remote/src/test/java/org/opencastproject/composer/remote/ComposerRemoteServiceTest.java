package org.opencastproject.composer.remote;


import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.security.TrustedHttpClientImpl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.net.URI;

import javax.ws.rs.WebApplicationException;

/**
 * This test makes assumptions about a running application at localhost:8080, with network access to svn.  It is
 * therefore @Ignored by default.
 */
@Ignore
public class ComposerRemoteServiceTest {
  
  private ComposerServiceRemoteImpl service;
 
  @Before
  public void setUp() throws Exception {
    service = new ComposerServiceRemoteImpl();
    service.setTrustedHttpClient(new TrustedHttpClientImpl("matterhorn_system_account", "CHANGE_ME"));
  }

  @Test
  public void testCount() throws Exception {
    try {
      Assert.assertTrue(service.countJobs(Status.FINISHED) > -1);
    } catch(WebApplicationException e) {
      e.printStackTrace();
      Assert.fail();
    }
  }
  
  @Test
  public void testProfiles() throws Exception {
    Assert.assertTrue(service.listProfiles().length > 0);
  }
  
  @Test
  public void testReceipts() throws Exception {
    MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    Track track = (Track)MediaPackageElementBuilderFactory.newInstance().newElementBuilder().elementFromURI(
            new URI("https://opencast.jira.com/svn/MH/trunk/modules/matterhorn-media/src/test/resources/av.mov"),
            Track.TYPE, MediaPackageElements.PRESENTER_SOURCE);
    mp.add(track);
    Job r = service.encode(track, "feed-m4a.http.http");
    Assert.assertNotNull(service.getJob(r.getId()));
    Assert.assertNull(service.getJob(-87656));
  }
  
}
