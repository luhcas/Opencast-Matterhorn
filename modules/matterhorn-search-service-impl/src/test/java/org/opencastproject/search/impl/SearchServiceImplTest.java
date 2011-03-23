/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */

package org.opencastproject.search.impl;

import static org.junit.Assert.assertTrue;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;

import org.opencastproject.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.api.SearchResultItem;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.workspace.api.Workspace;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Date;

/**
 * Tests the functionality of the search service.
 */
public class SearchServiceImplTest {

  /** The search service */
  private SearchServiceImpl service = null;

  /** The solr root directory */
  private String solrRoot = "target" + File.separator + "opencast" + File.separator + "searchindex";

  /** The access control list returned by the mocked authorization service */
  private AccessControlList acl = null;

  /** The security service */
  private SecurityService securityService = null;
  
  /** The user returned by the mocked security service */
  private User user = null;

  @Before
  public void setup() throws Exception {
    Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    final File dcFile = new File(getClass().getResource("/dublincore.xml").toURI());
    final File dcSeriesFile = new File(getClass().getResource("/series-dublincore.xml").toURI());
    Assert.assertNotNull(dcFile);
    EasyMock.expect(workspace.get((URI) EasyMock.anyObject())).andAnswer(new IAnswer<File>() {
      public File answer() throws Throwable {
        return EasyMock.getCurrentArguments()[0].toString().contains("series") ? dcSeriesFile : dcFile;
      }
    }).anyTimes();
    EasyMock.replay(workspace);

    service = new SearchServiceImpl();
    service.setDublincoreService(new DublinCoreCatalogService());
    service.setWorkspace(workspace);
    service.setupSolr(new File(solrRoot));
    ServiceRegistry serviceRegistry = EasyMock.createNiceMock(ServiceRegistry.class);
    EasyMock.replay(serviceRegistry);
    service.setServiceRegistry(serviceRegistry);

    user = new User("sample", new String[] { "ROLE_STUDENT", "ROLE_OTHERSTUDENT" });

    acl = new AccessControlList();
    AuthorizationService authorizationService = EasyMock.createNiceMock(AuthorizationService.class);
    EasyMock.expect(authorizationService.getAccessControlList((MediaPackage) EasyMock.anyObject())).andReturn(acl)
            .anyTimes();
    EasyMock.expect(
            authorizationService.hasPermission((MediaPackage) EasyMock.anyObject(), (String) EasyMock.anyObject()))
            .andReturn(true).anyTimes();
    service.setAuthorizationService(authorizationService);
    EasyMock.replay(authorizationService);

    securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getUser()).andReturn(user).anyTimes();
    service.setSecurityService(securityService);
    EasyMock.replay(securityService);
  }

  @After
  public void teardown() {
    service.deactivate();
    try {
      FileUtils.deleteDirectory(new File(solrRoot));
    } catch (IOException e) {
      fail("Error on cleanup");
    }
  }

  /**
   * Test wether an empty search index will work.
   */
  @Test
  public void testEmptySearchIndex() {
    SearchResult result = service.getByQuery(new SearchQueryImpl().withId("foo"));
    assertEquals(0, result.size());
  }

  /**
   * Adds a simple media package that has a dublin core for the episode only.
   */
  @Test
  public void testGetMediaPackage() throws Exception {
    MediaPackageBuilderFactory builderFactory = MediaPackageBuilderFactory.newInstance();
    MediaPackageBuilder mediaPackageBuilder = builderFactory.newMediaPackageBuilder();
    URL rootUrl = SearchServiceImplTest.class.getResource("/");
    mediaPackageBuilder.setSerializer(new DefaultMediaPackageSerializerImpl(rootUrl));

    // Load the simple media package
    MediaPackage mediaPackage = null;
    InputStream is = null;
    try {
      is = SearchServiceImplTest.class.getResourceAsStream("/manifest-simple.xml");
      mediaPackage = mediaPackageBuilder.loadFromXml(is);
    } finally {
      IOUtils.closeQuietly(is);
    }

    // Make sure our mocked ACL has the read and write permission
    acl.getEntries().add(new AccessControlEntry(user.getRoles()[0], SearchService.READ_PERMISSION, true));
    acl.getEntries().add(new AccessControlEntry(user.getRoles()[0], SearchService.WRITE_PERMISSION, true));

    // Add the media package to the search index
    service.add(mediaPackage);

    // Make sure it's properly indexed and returned for authorized users
    SearchQueryImpl q = new SearchQueryImpl();
    q.includeEpisodes(true);
    q.includeSeries(false);
    q.withId("10.0000/1");
    assertEquals(1, service.getByQuery(q).size());

    acl.getEntries().clear();
    acl.getEntries().add(new AccessControlEntry("ROLE_UNKNOWN", SearchService.READ_PERMISSION, true));
    acl.getEntries().add(new AccessControlEntry(user.getRoles()[0], SearchService.WRITE_PERMISSION, true));

    // Add the media package to the search index
    service.add(mediaPackage);

    // This mediapackage should not be readable by the current user (due to the lack of role ROLE_UNKNOWN)
    q = new SearchQueryImpl();
    q.includeEpisodes(true);
    q.includeSeries(false);
    q.withId("10.0000/1");
    assertEquals(0, service.getByQuery(q).size());
  }

  /**
   * Adds a simple media package that has a dublin core for the episode only.
   */
  @Test
  public void testAddSimpleMediaPackage() throws Exception {
    MediaPackageBuilderFactory builderFactory = MediaPackageBuilderFactory.newInstance();
    MediaPackageBuilder mediaPackageBuilder = builderFactory.newMediaPackageBuilder();
    URL rootUrl = SearchServiceImplTest.class.getResource("/");
    mediaPackageBuilder.setSerializer(new DefaultMediaPackageSerializerImpl(rootUrl));

    // Load the simple media package
    MediaPackage mediaPackage = null;
    InputStream is = null;
    try {
      is = SearchServiceImplTest.class.getResourceAsStream("/manifest-simple.xml");
      mediaPackage = mediaPackageBuilder.loadFromXml(is);
    } finally {
      IOUtils.closeQuietly(is);
    }

    // Make sure our mocked ACL has the read and write permission
    acl.getEntries().add(new AccessControlEntry(user.getRoles()[0], SearchService.READ_PERMISSION, true));
    acl.getEntries().add(new AccessControlEntry(user.getRoles()[0], SearchService.WRITE_PERMISSION, true));

    // Add the media package to the search index
    service.add(mediaPackage);

    // Make sure it's properly indexed and returned
    SearchQueryImpl q = new SearchQueryImpl();
    q.includeEpisodes(true);
    q.includeSeries(false);
    q.withId("10.0000/1");
    assertEquals(1, service.getByQuery(q).size());

    q = new SearchQueryImpl();
    q.includeEpisodes(true);
    q.includeSeries(false);

    assertEquals(1, service.getByQuery(q).size());

    // Test for various fields
    q = new SearchQueryImpl();
    q.includeEpisodes(true);
    q.includeSeries(false);
    q.withId("10.0000/1");
    SearchResult result = service.getByQuery(q);
    assertEquals(1, result.getTotalSize());
    SearchResultItem resultItem = result.getItems()[0];
    assertNotNull(resultItem.getMediaPackage());
    assertEquals(1, resultItem.getMediaPackage().getCatalogs().length);
  }

  /**
   * Ads a simple media package that has a dublin core for the episode only.
   */
  @Test
  public void testAddFullMediaPackage() throws Exception {
    MediaPackageBuilderFactory builderFactory = MediaPackageBuilderFactory.newInstance();
    MediaPackageBuilder mediaPackageBuilder = builderFactory.newMediaPackageBuilder();
    URL rootUrl = SearchServiceImplTest.class.getResource("/");
    mediaPackageBuilder.setSerializer(new DefaultMediaPackageSerializerImpl(rootUrl));

    // Load the simple media package
    MediaPackage mediaPackage = null;
    InputStream is = null;
    try {
      is = SearchServiceImplTest.class.getResourceAsStream("/manifest-full.xml");
      mediaPackage = mediaPackageBuilder.loadFromXml(is);
    } finally {
      IOUtils.closeQuietly(is);
    }

    // Make sure our mocked ACL has the read and write permission
    acl.getEntries().add(new AccessControlEntry(user.getRoles()[0], SearchService.READ_PERMISSION, true));
    acl.getEntries().add(new AccessControlEntry(user.getRoles()[0], SearchService.WRITE_PERMISSION, true));

    // Add the media package to the search index
    service.add(mediaPackage);

    // Make sure it's properly indexed and returned
    SearchQueryImpl q = new SearchQueryImpl();
    q.includeEpisodes(true);
    q.includeSeries(false);
    q.withId("10.0000/2");
    assertEquals(1, service.getByQuery(q).size());
    q.withId(null); // Clear the ID requirement
    assertEquals(1, service.getByQuery(q).size());
  }

  /**
   * Test removal from the search index.
   */
  @Test
  public void testDeleteMediaPackage() throws Exception {
    MediaPackageBuilderFactory builderFactory = MediaPackageBuilderFactory.newInstance();
    MediaPackageBuilder mediaPackageBuilder = builderFactory.newMediaPackageBuilder();
    URL rootUrl = SearchServiceImplTest.class.getResource("/");
    mediaPackageBuilder.setSerializer(new DefaultMediaPackageSerializerImpl(rootUrl));

    // Load the simple media package
    MediaPackage mediaPackage = null;
    InputStream is = null;
    try {
      is = SearchServiceImplTest.class.getResourceAsStream("/manifest-simple.xml");
      mediaPackage = mediaPackageBuilder.loadFromXml(is);
    } finally {
      IOUtils.closeQuietly(is);
    }

    // Make sure our mocked ACL has the read and write permission
    acl.getEntries().add(new AccessControlEntry(user.getRoles()[0], SearchService.READ_PERMISSION, true));
    acl.getEntries().add(new AccessControlEntry(user.getRoles()[0], SearchService.WRITE_PERMISSION, true));

    // Add the media package to the search index
    service.add(mediaPackage);

    // Try to delete it
    Date deletedDate = new Date();
    try {
      service.delete(mediaPackage.getIdentifier().toString());
      fail("Unauthorized user was able to delete a mediapackage");
    } catch (UnauthorizedException e) {
      // That's expected
    }

    // Second try with a "fixed" roleset
    User adminUser = new User("admin", new String[] {AuthorizationService.ADMIN_ROLE});
    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getUser()).andReturn(adminUser).anyTimes();
    service.setSecurityService(securityService);
    EasyMock.replay(securityService);
    boolean deleted = service.delete(mediaPackage.getIdentifier().toString());
    assertTrue(deleted);

    // Now go back to the original security service and user
    service.setSecurityService(this.securityService);

    SearchQueryImpl q = new SearchQueryImpl();
    q.includeEpisodes(true);
    q.includeSeries(false);
    q.withId("10.0000/1");
    assertEquals(0, service.getByQuery(q).size());
    q.withId(null); // Clear the ID requirement
    assertEquals(0, service.getByQuery(q).size());

    q = new SearchQueryImpl();
    q.withDeletedSince(deletedDate);
    assertEquals(1, service.getByQuery(q).size());
  }

  /**
   * Ads a media package with one dublin core for the episode and one for the series.
   */
  @Test
  public void testAddSeriesMediaPackage() throws Exception {
    MediaPackageBuilderFactory builderFactory = MediaPackageBuilderFactory.newInstance();
    MediaPackageBuilder mediaPackageBuilder = builderFactory.newMediaPackageBuilder();
    URL rootUrl = SearchServiceImplTest.class.getResource("/");
    mediaPackageBuilder.setSerializer(new DefaultMediaPackageSerializerImpl(rootUrl));

    // Load the simple media package
    MediaPackage mediaPackage = null;
    InputStream is = null;
    try {
      is = SearchServiceImplTest.class.getResourceAsStream("/manifest-full.xml");
      mediaPackage = mediaPackageBuilder.loadFromXml(is);
    } catch (MediaPackageException e) {
      fail("Error loading full media package");
    } finally {
      IOUtils.closeQuietly(is);
    }

    // Make sure our mocked ACL has the read and write permission
    acl.getEntries().add(new AccessControlEntry(user.getRoles()[0], SearchService.READ_PERMISSION, true));
    acl.getEntries().add(new AccessControlEntry(user.getRoles()[0], SearchService.WRITE_PERMISSION, true));

    // Add the media package to the search index
    service.add(mediaPackage);

    // Make sure it's properly indexed and returned
    SearchQueryImpl q = new SearchQueryImpl();
    q.includeEpisodes(false);
    q.includeSeries(true);

    SearchResult result = service.getByQuery(q);
    assertEquals(1, result.size());
    assertEquals("foobar-serie", result.getItems()[0].getId());
  }

}