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
package org.opencastproject.security.api;

import static org.opencastproject.security.api.SecurityService.ANONYMOUS_USER;

import java.util.Arrays;
import java.util.Collection;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

/**
 * A simple user model.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "user", namespace = "http://org.opencastproject.security")
@XmlRootElement(name = "user", namespace = "http://org.opencastproject.security")
public final class User {

  /** The user name */
  protected String userName;

  /** The roles */
  protected String[] roles;

  /** The optional password. Note that this will never be serialized to xml */
  @XmlTransient
  protected String password;

  /**
   * No-arg constructor needed by JAXB
   */
  public User() {
  }

  /**
   * Constructs a user with anonymous role.
   * 
   * @param userName
   *          the username
   */
  public User(String userName) {
    this(userName, ANONYMOUS_USER.getRoles());
  }

  /**
   * Constructs a user with the specified roles.
   * 
   * @param userName
   *          the username
   * @param roleCollection
   *          the set of roles for this user
   */
  public User(String userName, Collection<String> roleCollection) {
    this.userName = userName;
    if (roleCollection == null) {
      roles = ANONYMOUS_USER.getRoles();
    } else {
      roles = roleCollection.toArray(new String[roleCollection.size()]);
    }
  }

  /**
   * Constructs a user with the specified roles.
   * 
   * @param userName
   *          the username
   * @param roles
   *          the set of roles for this user
   */
  public User(String userName, String[] roles) {
    this(userName, null, roles);
  }

  /**
   * Constructs a user with the specified roles.
   * 
   * @param userName
   *          the username
   * @param password
   *          the password
   * @param roles
   *          the set of roles for this user
   */
  public User(String userName, String password, String[] roles) {
    this.userName = userName;
    this.password = password;
    if (roles == null || roles.length == 0) {
      this.roles = ANONYMOUS_USER.getRoles();
    } else {
      Arrays.sort(roles);
      this.roles = roles;
    }
  }

  /**
   * Gets this user's unique account name.
   * 
   * @return the account name
   */
  public String getUserName() {
    return userName;
  }

  /**
   * Gets this user's password, if available.
   * 
   * @return the password
   */
  public String getPassword() {
    return password;
  }

  /**
   * Gets the user's roles. For anonymous users, this will return {@link Anonymous}.
   * 
   * @return the user's roles
   */
  public String[] getRoles() {
    return roles;
  }

  /**
   * Returns whether the user is in a specific role.
   * 
   * @param role
   *          the role to check
   * @return whether the role is one of this user's roles
   */
  public boolean hasRole(String role) {
    for (String r : roles) {
      if (r.equals(role))
        return true;
    }
    return false;
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof User))
      return false;
    User other = (User) obj;
    return this.userName.equals(other.userName);
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return userName.hashCode();
  };

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "User '" + userName + "'";
  }
}
