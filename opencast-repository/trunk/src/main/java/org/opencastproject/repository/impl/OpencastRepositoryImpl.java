/**
 *  Copyright 2009 Opencast Project (http://www.opencastproject.org)
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
package org.opencastproject.repository.impl;

import org.opencastproject.authentication.api.AuthenticationService;
import org.opencastproject.repository.api.OpencastRepository;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.api.JackrabbitNodeTypeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

public class OpencastRepositoryImpl implements OpencastRepository {
  private static final Logger logger = LoggerFactory.getLogger(OpencastRepositoryImpl.class);
  public static final String PREFIX = "oc:";
  private Repository repo;
  private AuthenticationService authn;

  public OpencastRepositoryImpl(Repository repo) {
    this.repo = repo;
    
    // TODO Remove jackrabbit-specific dependencies
    Session session = getSession();
    try {
      JackrabbitNodeTypeManager manager = (JackrabbitNodeTypeManager)
        session.getWorkspace().getNodeTypeManager();
      manager.registerNodeTypes(this.getClass().getResourceAsStream("/nodeTypes.cnd"),
          JackrabbitNodeTypeManager.TEXT_X_JCR_CND);
    } catch (Exception e) {
      e.printStackTrace();
      logger.error(e.getLocalizedMessage());
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.repository.api.OpencastRepository#getObject(java.lang.Class, java.lang.String)
   */
  @SuppressWarnings("unchecked")
  public <T> T getObject(Class<T> type, String path) {
    assertSupported(type);
    logger.debug("getting data from " + path);
    Session session = getSession();
    if (session == null) {
      throw new RuntimeException("Couldn't log in to the repo");
    } else {
      Node node = null;
      try {
        node = (Node)session.getItem(path + "/jcr:content");
        return (T) node.getProperty("jcr:data").getStream();
      } catch (PathNotFoundException e) {
        throw new RuntimeException(e);
      } catch (RepositoryException e) {
        throw new RuntimeException(e);
      }
    }
  }

  protected Session getSession() {
    Session session = null;
    try {
      // TODO Use authentication service to log in to the repo
      session = repo.login(new SimpleCredentials("foo", "bar".toCharArray()));
    } catch (LoginException e) {
      e.printStackTrace();
    } catch (RepositoryException e) {
      e.printStackTrace();
    }
    return session;
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.repository.api.OpencastRepository#getSupportedTypes()
   */
  public Class<?>[] getSupportedTypes() {
    // TODO: Support MediaBundle
    return new Class<?>[] {InputStream.class};
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.repository.api.OpencastRepository#hasObject(java.lang.String)
   */
  public boolean hasObject(String path) {
    Session session = getSession();
    if (session == null) {
      throw new RuntimeException("Couldn't log in to the repo");
    } else {
      try {
        return session.itemExists(path + "/jcr:content");
      } catch (RepositoryException e) {
        throw new RuntimeException(e);
      }
    }
  }

  protected void assertSupported(Class<?> type) {
    Class<?>[] supportedTypes = getSupportedTypes();
    boolean objectClassSupported = false;
    for (int i=0; i<supportedTypes.length; i++) {
      Class<?> currentClass = supportedTypes[i];
      if (currentClass.isAssignableFrom(type)) {
        objectClassSupported = true;
        break;
      }
    }
    if( ! objectClassSupported) {
      throw new IllegalArgumentException("Class " + type + " is not a supported object type");
    }
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.repository.api.OpencastRepository#putObject(java.lang.Object, java.lang.String)
   */
  public String putObject(Object object, String path) {
    assertSupported(object.getClass());
    Session session = getSession();
    if (session == null) {
      throw new RuntimeException("Couldn't log in to the repo");
    }
    try {
      Node fileNode = null;
      if (session.itemExists(path)) {
        fileNode = (Node)session.getItem(path);
      } else {
        fileNode = buildPath(session, path);
      }
      logger.debug("fileNode path=" + fileNode.getPath());
      Node resNode;
      try {
        resNode = (Node) fileNode.getNode("jcr:content");
        logger.debug("resource node exists: " + resNode.getPath());
      } catch(PathNotFoundException e) {
        resNode = fileNode.addNode("jcr:content", "nt:resource");
        logger.debug("resource node created: " + resNode.getPath());
        resNode.addMixin("mix:referenceable");
        resNode.setProperty("jcr:mimeType", "application/octet-stream");
        resNode.setProperty("jcr:encoding", "");
      }
      resNode.setProperty("jcr:data", convertToStream(object));
      resNode.setProperty("jcr:lastModified", Calendar.getInstance());
      session.save();
      return fileNode.getUUID();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Build each "nt:unstructured" node as needed
   * @param root
   * @param path
   * @return The leaf nt:unstructured node
   */
  protected Node buildPath(Session session, String path) throws Exception {
    logger.debug("Building nodes for " + path);
    StringBuilder partialPath = new StringBuilder();
    Node currentNode = session.getRootNode();
    String[] sa = path.split("/");
    for(int i=0; i<sa.length; i++) {
      String pathElement = sa[i];
      if(StringUtils.isEmpty(pathElement)) continue; // skip any empty path segments
      partialPath.append("/");
      partialPath.append(pathElement);
      logger.debug("checking for node " + pathElement + " at path " + partialPath);
      if(session.itemExists(partialPath.toString())) {
        currentNode = currentNode.getNode(pathElement);
      } else {
        logger.debug("Adding node " + pathElement);
        currentNode = currentNode.addNode(pathElement, "nt:unstructured");
      }
      currentNode.addMixin("mix:referenceable");
    }
    return currentNode;
  }
  protected InputStream convertToStream(Object object) {
    // TODO Handle Media Bundles and anything else this should support
    return (InputStream)object;
  }

  public Map<String, String> getMetadata(String path) {
    Session session = getSession();
    if (session == null) {
      throw new RuntimeException("Couldn't log in to the repo");
    }
    try {
      Node node = null;
      if (session.itemExists(path)) {
        node = (Node)session.getItem(path);
        Map<String, String> map = new HashMap<String, String>();
        PropertyIterator iter = node.getProperties();
        while(iter.hasNext()) {
          Property prop = iter.nextProperty();
          if( ! prop.getName().startsWith(PREFIX)) continue;
          map.put(prop.getName().substring(PREFIX.length()), prop.getString());
        }
        return map;
      } else {
        throw new RuntimeException("no object exists at path " + path);
      }
    } catch(PathNotFoundException e) {
      throw new RuntimeException(e);
    } catch(RepositoryException e) {
      throw new RuntimeException(e);
    }
  }

  public void putMetadata(String value, String key, String path) {
    Session session = getSession();
    if (session == null) {
      throw new RuntimeException("Couldn't log in to the repo");
    }
    try {
      Node node = null;
      if (session.itemExists(path)) {
        node = (Node)session.getItem(path);
        node.setProperty(PREFIX + key, value);
        session.save();
      } else {
        throw new RuntimeException("no object exists at path " + path);
      }
    } catch(PathNotFoundException e) {
      throw new RuntimeException(e);
    } catch(RepositoryException e) {
      throw new RuntimeException(e);
    }
  }

}
