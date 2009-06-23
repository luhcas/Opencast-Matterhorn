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
package org.opencastproject.authentication;

import org.opencastproject.authentication.api.AuthenticationService;
import org.jasig.cas.client.authentication.AuthenticationFilter;
import java.util.Map;
import javax.servlet.Filter;

public class AuthenticationServiceCasImpl implements AuthenticationService {
  public String getUserId() {
    // Get the CAS receipt from threadlocal
    // FIXME return the user ID
    return null;
  }
  public Filter newAuthenticationFilter(Map<String, String> props) {
    String casServerLoginUrl = props.get("casServerLoginUrl");
    String casService = props.get("casService");
    AuthenticationFilter authenticationFilter = new AuthenticationFilter();
    authenticationFilter.setIgnoreInitConfiguration(true);
    authenticationFilter.setCasServerLoginUrl(casServerLoginUrl);
    authenticationFilter.setRenew(false);
    authenticationFilter.setGateway(false);
    authenticationFilter.setService(casService);
    authenticationFilter.setServerName(casService);
    return authenticationFilter;
  }
}
