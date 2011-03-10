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
package org.opencastproject.userdirectory.ldap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Dictionary;
import java.util.Hashtable;


public class LdapUserDetailServiceTest {
  
  protected LdapUserDetailService service = null;
  
  @Before
  public void setup() throws Exception {
    Dictionary<String, String> props = new Hashtable<String, String>();
    props.put("org.opencastproject.userdirectory.ldap.searchbase", "ou=people,dc=berkeley,dc=edu");
    props.put("org.opencastproject.userdirectory.ldap.searchfilter", "(uid={0})");
    props.put("org.opencastproject.userdirectory.ldap.url", "ldap://ldap.berkeley.edu");

    service = new LdapUserDetailService();
    service.updated(props);
  }
  
  @Ignore("Ignore this test by default, since it requires internet connectivity")
  @Test
  public void testLookup() throws Exception {
    UserDetails user = service.loadUserByUsername("231693");
    Assert.assertNotNull(user);
  }
}
