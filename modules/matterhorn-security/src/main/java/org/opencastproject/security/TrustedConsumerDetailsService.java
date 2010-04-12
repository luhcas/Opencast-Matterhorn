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

import org.osgi.service.component.ComponentContext;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.oauth.common.OAuthException;
import org.springframework.security.oauth.common.signature.SharedConsumerSecret;
import org.springframework.security.oauth.common.signature.SignatureSecret;
import org.springframework.security.oauth.provider.ConsumerDetails;
import org.springframework.security.oauth.provider.ConsumerDetailsService;
import org.springframework.security.oauth.provider.ExtraTrustConsumerDetails;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides details about our trusted OAuth consumers.
 */
public class TrustedConsumerDetailsService implements ConsumerDetailsService {
  public static final String CONSUMER_KEY = "org.opencastproject.oauth.trusted.consumer";
  public static final String SHARED_SECRET_KEY = "org.opencastproject.oauth.secret";
  protected SignatureSecret sharedSecret;
  
  public void activate(ComponentContext cc) {
    String secret = cc.getBundleContext().getProperty(SHARED_SECRET_KEY);
    if(secret == null) {
      throw new IllegalStateException("Can not activate " + this.getClass().getName() + " without a shared secret");
    }
    this.sharedSecret = new SharedConsumerSecret(secret);
  }
  
  /**
   * {@inheritDoc}
   * @see org.springframework.security.oauth.provider.ConsumerDetailsService#loadConsumerByConsumerKey(java.lang.String)
   */
  @Override
  public ConsumerDetails loadConsumerByConsumerKey(String consumerKey) throws OAuthException {
    if( ! CONSUMER_KEY.equals(consumerKey)) {
      throw new OAuthException("consumer " + consumerKey + " is not a trusted consumer");
    }
    return new ExtraTrustConsumerDetails() {
      private static final long serialVersionUID = 1L;

      @Override
      public SignatureSecret getSignatureSecret() {
        return sharedSecret;
      }
      
      @Override
      public String getConsumerName() {
        return "Trusted OAuth Consumer";
      }
      
      @Override
      public String getConsumerKey() {
        return CONSUMER_KEY;
      }

      /**
       * {@inheritDoc}
       * @see org.springframework.security.oauth.provider.ConsumerDetails#getAuthorities()
       */
      @Override
      public List<GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
        authorities.add(new GrantedAuthorityImpl("ROLE_USER"));
        authorities.add(new GrantedAuthorityImpl("ROLE_ADMIN"));
        return authorities;
      }
      
      @Override
      public boolean isRequiredToObtainAuthenticatedToken() {
        return false;
      }
    };
  }
}
