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
package org.opencastproject.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

/**
 * JPA-annotated user object for use with the JpaUserDetailService. See
 * org.springframework.security.core.userdetails.User.
 */
@Entity
@Access(AccessType.FIELD)
@Table(name = "MH_USER")
public class JpaUser implements UserDetails {

  /** The java.io.serialization uid */
  private static final long serialVersionUID = -6693877536928844019L;

  @Id
  @Column
  protected String username;

  @Column
  protected String password;

  @ElementCollection
  @CollectionTable(name = "MH_ROLE", joinColumns = @JoinColumn(name = "USERNAME"))
  @Column(name = "ROLE")
  protected Set<String> authorities;

  @Column
  protected boolean accountNonExpired;

  @Column
  protected boolean accountNonLocked;

  @Column
  protected boolean credentialsNonExpired;

  @Column
  protected boolean enabled;

  /**
   * No-arg constructor needed by JPA
   */
  public JpaUser() {

  }

  /**
   * Construct the <code>User</code> with the details required by
   * {@link org.springframework.security.authentication.dao.DaoAuthenticationProvider}.
   * 
   * @param username
   *          the username presented to the <code>DaoAuthenticationProvider</code>
   * @param password
   *          the password that should be presented to the <code>DaoAuthenticationProvider</code>
   * @param enabled
   *          set to <code>true</code> if the user is enabled
   * @param accountNonExpired
   *          set to <code>true</code> if the account has not expired
   * @param credentialsNonExpired
   *          set to <code>true</code> if the credentials have not expired
   * @param accountNonLocked
   *          set to <code>true</code> if the account is not locked
   * @param authorities
   *          the authorities that should be granted to the caller if they presented the correct username and password
   *          and the user is enabled. Not null.
   * 
   * @throws IllegalArgumentException
   *           if a <code>null</code> value was passed either as a parameter or as an element in the
   *           <code>GrantedAuthority</code> collection
   */
  public JpaUser(String username, String password, boolean enabled, boolean accountNonExpired,
          boolean credentialsNonExpired, boolean accountNonLocked, Collection<GrantedAuthority> authorities) {

    if (((username == null) || "".equals(username)) || (password == null)) {
      throw new IllegalArgumentException("Cannot pass null or empty values to constructor");
    }

    this.username = username;
    this.password = password;
    this.enabled = enabled;
    this.accountNonExpired = accountNonExpired;
    this.credentialsNonExpired = credentialsNonExpired;
    this.accountNonLocked = accountNonLocked;
    this.authorities = sortAuthorities(authorities);
  }

  // ~ Methods ========================================================================================================

  public boolean equals(Object rhs) {
    if (!(rhs instanceof JpaUser) || (rhs == null)) {
      return false;
    }

    JpaUser user = (JpaUser) rhs;

    // We rely on constructor to guarantee any User has non-null
    // authorities
    if (!authorities.equals(user.authorities)) {
      return false;
    }

    // We rely on constructor to guarantee non-null username and password
    return (this.getPassword().equals(user.getPassword()) && this.getUsername().equals(user.getUsername())
            && (this.isAccountNonExpired() == user.isAccountNonExpired())
            && (this.isAccountNonLocked() == user.isAccountNonLocked())
            && (this.isCredentialsNonExpired() == user.isCredentialsNonExpired()) && (this.isEnabled() == user
            .isEnabled()));
  }

  public Collection<GrantedAuthority> getAuthorities() {
    Set<GrantedAuthority> set = new HashSet<GrantedAuthority>();
    for (String authority : authorities) {
      set.add(new GrantedAuthorityImpl(authority));
    }
    return set;
  }

  public String getPassword() {
    return password;
  }

  public String getUsername() {
    return username;
  }

  public int hashCode() {
    int code = 9792;

    for (GrantedAuthority authority : getAuthorities()) {
      code = code * (authority.hashCode() % 7);
    }

    if (this.getPassword() != null) {
      code = code * (this.getPassword().hashCode() % 7);
    }

    if (this.getUsername() != null) {
      code = code * (this.getUsername().hashCode() % 7);
    }

    if (this.isAccountNonExpired()) {
      code = code * -2;
    }

    if (this.isAccountNonLocked()) {
      code = code * -3;
    }

    if (this.isCredentialsNonExpired()) {
      code = code * -5;
    }

    if (this.isEnabled()) {
      code = code * -7;
    }

    return code;
  }

  public boolean isAccountNonExpired() {
    return accountNonExpired;
  }

  public boolean isAccountNonLocked() {
    return this.accountNonLocked;
  }

  public boolean isCredentialsNonExpired() {
    return credentialsNonExpired;
  }

  public boolean isEnabled() {
    return enabled;
  }

  private static SortedSet<String> sortAuthorities(Collection<GrantedAuthority> authorities) {
    Assert.notNull(authorities, "Cannot pass a null GrantedAuthority collection");
    // Ensure array iteration order is predictable (as per UserDetails.getAuthorities() contract and SEC-717)
    SortedSet<String> sortedAuthorities = new TreeSet<String>();

    for (GrantedAuthority grantedAuthority : authorities) {
      Assert.notNull(grantedAuthority, "GrantedAuthority list cannot contain any null elements");
      sortedAuthorities.add(grantedAuthority.getAuthority());
    }

    return sortedAuthorities;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(super.toString()).append(": ");
    sb.append("Username: ").append(this.username).append("; ");
    sb.append("Password: [PROTECTED]; ");
    sb.append("Enabled: ").append(this.enabled).append("; ");
    sb.append("AccountNonExpired: ").append(this.accountNonExpired).append("; ");
    sb.append("credentialsNonExpired: ").append(this.credentialsNonExpired).append("; ");
    sb.append("AccountNonLocked: ").append(this.accountNonLocked).append("; ");

    if (!authorities.isEmpty()) {
      sb.append("Granted Authorities: ");

      boolean first = true;
      for (String auth : authorities) {
        if (!first) {
          sb.append(",");
        }
        first = false;

        sb.append(auth);
      }
    } else {
      sb.append("Not granted any authorities");
    }

    return sb.toString();
  }
}
