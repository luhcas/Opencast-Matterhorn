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
package org.opencastproject.authorization.xacml;

import static org.opencastproject.security.api.User.ANONYMOUS_USER;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workspace.api.Workspace;

import de.schlichtherle.io.FileOutputStream;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests XACML features of the security service
 */
public class XacmlSecurityTest {
  
  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(XacmlSecurityTest.class);
  
  /** The stub workspace to store xacml files */
  WorkspaceStub workspace = null;

  /** The username to use with the security service */
  final String currentUser = "me";
  
  /** The roles to use with the security service */
  final List<String> currentRoles = new ArrayList<String>();

  // Override the behavior of the security service to use the current user and roles defined here
  SecurityService securityService = null;

  XACMLAuthorizationService authzService = null;
  
  @Before
  public void setup() throws Exception {
    workspace = new WorkspaceStub();
    securityService = new SecurityService() {
      @Override
      public User getUser() {
        return new User(currentUser, currentRoles);
      }
    };
    authzService = new XACMLAuthorizationService();
    authzService.setWorkspace(new WorkspaceStub());
    authzService.setSecurityService(securityService);
  }
  
  @After
  public void teardown() throws Exception {
    workspace.file.delete();
  }
  
  @Test
  public void testSecurity() throws Exception {

    // Create a mediapackage and some role/action tuples
    MediaPackage mediapackage = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();

    List<AccessControlEntry> acl = new ArrayList<AccessControlEntry>();
    acl.add(new AccessControlEntry("admin", "delete", true));
    acl.add(new AccessControlEntry("admin", "read", true));

    acl.add(new AccessControlEntry("student", "read", true));
    acl.add(new AccessControlEntry("student", "comment", true));

    acl.add(new AccessControlEntry(ANONYMOUS_USER.getRoles()[0], "read", true));
    acl.add(new AccessControlEntry(ANONYMOUS_USER.getRoles()[0], "comment", false));

    String xacml = XACMLUtils.getXacml(mediapackage, acl);
    logger.debug("XACML contents: {}", xacml);
    
    // Add the security policy to the mediapackage
    mediapackage = authzService.setAccessControl(mediapackage, acl);
    
    // Ensure that the permissions specified are respected by the security service
    currentRoles.clear();
    currentRoles.add("admin");
    Assert.assertTrue(authzService.hasPermission(mediapackage, "delete"));
    Assert.assertTrue(authzService.hasPermission(mediapackage, "read"));
    Assert.assertFalse(authzService.hasPermission(mediapackage, "comment"));

    List<AccessControlEntry> computedAcl = authzService.getAccessControlList(mediapackage);
    Assert.assertTrue("ACLs are the same size?", computedAcl.size() == acl.size());
    Assert.assertTrue("ACLs contain the same ACEs?", computedAcl.containsAll(acl));
    
    
    currentRoles.clear();
    currentRoles.add("student");
    Assert.assertFalse(authzService.hasPermission(mediapackage, "delete"));
    Assert.assertTrue(authzService.hasPermission(mediapackage, "read"));
    Assert.assertTrue(authzService.hasPermission(mediapackage, "comment"));

    currentRoles.clear();
    currentRoles.add(ANONYMOUS_USER.getRoles()[0]);
    Assert.assertFalse(authzService.hasPermission(mediapackage, "delete"));
    Assert.assertTrue(authzService.hasPermission(mediapackage, "read"));
    Assert.assertFalse(authzService.hasPermission(mediapackage, "comment"));
    
  }
  
  static class WorkspaceStub implements Workspace {
    File file = null;
    
    public WorkspaceStub() throws IOException {
      this.file = File.createTempFile("xacml", "xml");
    }
    
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workspace.api.Workspace#get(java.net.URI)
     */
    @Override
    public File get(URI uri) throws NotFoundException, IOException {
      return file;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workspace.api.Workspace#getBaseUri()
     */
    @Override
    public URI getBaseUri() {
      // TODO Auto-generated method stub
      return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workspace.api.Workspace#put(java.lang.String, java.lang.String, java.lang.String, java.io.InputStream)
     */
    @Override
    public URI put(String mediaPackageID, String mediaPackageElementID, String fileName, InputStream in)
            throws IOException {
      FileOutputStream out = new FileOutputStream(file);
      IOUtils.copyLarge(in, out);
      IOUtils.closeQuietly(out);
      IOUtils.closeQuietly(in);
      return file.toURI();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workspace.api.Workspace#putInCollection(java.lang.String, java.lang.String, java.io.InputStream)
     */
    @Override
    public URI putInCollection(String collectionId, String fileName, InputStream in) throws IOException {
      // TODO Auto-generated method stub
      return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workspace.api.Workspace#getCollectionContents(java.lang.String)
     */
    @Override
    public URI[] getCollectionContents(String collectionId) throws IOException {
      // TODO Auto-generated method stub
      return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workspace.api.Workspace#delete(java.net.URI)
     */
    @Override
    public void delete(URI uri) throws NotFoundException, IOException {
      // TODO Auto-generated method stub
      
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workspace.api.Workspace#delete(java.lang.String, java.lang.String)
     */
    @Override
    public void delete(String mediaPackageID, String mediaPackageElementID) throws NotFoundException, IOException {
      // TODO Auto-generated method stub
      
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workspace.api.Workspace#deleteFromCollection(java.lang.String, java.lang.String)
     */
    @Override
    public void deleteFromCollection(String collectionId, String fileName) throws NotFoundException, IOException {
      // TODO Auto-generated method stub
      
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workspace.api.Workspace#getURI(java.lang.String, java.lang.String)
     */
    @Override
    public URI getURI(String mediaPackageID, String mediaPackageElementID) {
      return file.toURI();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workspace.api.Workspace#getCollectionURI(java.lang.String, java.lang.String)
     */
    @Override
    public URI getCollectionURI(String collectionID, String fileName) {
      // TODO Auto-generated method stub
      return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workspace.api.Workspace#moveTo(java.net.URI, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public URI moveTo(URI collectionURI, String toMediaPackage, String toMediaPackageElement, String toFileName)
            throws NotFoundException, IOException {
      // TODO Auto-generated method stub
      return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.workspace.api.Workspace#copyTo(java.net.URI, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public URI copyTo(URI collectionURI, String toMediaPackage, String toMediaPackageElement, String toFileName)
            throws NotFoundException, IOException {
      // TODO Auto-generated method stub
      return null;
    }
    
  }
}
