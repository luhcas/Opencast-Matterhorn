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
package org.opencastproject.sampleservice;

import org.opencastproject.api.OpencastJcrServer;
import org.opencastproject.sampleservice.api.SampleService;

import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

public class SampleServiceImpl implements SampleService {

  public static final String PROPERTY_KEY = "sample-property";

  protected Repository repo;

  public void setJcrServer(OpencastJcrServer jcrServer) {
    this.repo = jcrServer.getRepository();
  }

  protected Session getSession() {
    Session session = null;
    try {
      session = repo.login(new SimpleCredentials("foo", "bar".toCharArray()));
    } catch (LoginException e) {
      e.printStackTrace();
    } catch (RepositoryException e) {
      e.printStackTrace();
    }
    return session;
  }

  public String getSomething(String path) {
    Session session = getSession();
    if (session == null) {
      return "Couldn't log in to the repository";
    } else {
      Node node = null;
      try {
        node = session.getRootNode().getNode(path);
      } catch (PathNotFoundException e) {
        e.printStackTrace();
      } catch (RepositoryException e) {
        e.printStackTrace();
      }
      if (node == null) {
        return "Couldn't find node " + path;
      } else {
        try {
          return node.getProperty(PROPERTY_KEY).getString();
        } catch (ItemNotFoundException e) {
          e.printStackTrace();
          return e.getMessage();
        } catch (RepositoryException e) {
          e.printStackTrace();
          return e.getMessage();
        }
      }
    }
  }

  public void setSomething(String path, String content) {
    Session session = getSession();
    if (session == null) {
      throw new RuntimeException("Couldn't log in to the repository");
    } else {
      Node node = null;
      try {
        node = session.getRootNode().getNode(path);
      } catch (PathNotFoundException e) {
        e.printStackTrace();
      } catch (RepositoryException e) {
        e.printStackTrace();
      }
      if (node == null) {
        try {
          node = session.getRootNode().addNode(path);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
      try {
        node.setProperty(PROPERTY_KEY, content);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
}
