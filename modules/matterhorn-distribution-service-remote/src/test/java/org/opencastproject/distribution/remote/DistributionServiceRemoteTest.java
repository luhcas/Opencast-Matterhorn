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
package org.opencastproject.distribution.remote;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DistributionServiceRemoteTest {
  DistributionServiceRemoteImpl service;
  
  @Before
  public void setup() throws Exception {
    service = new DistributionServiceRemoteImpl();
  }
  
  @After
  public void tearDown() throws Exception {
    
  }
  
  @Test
  public void testDistribution() throws Exception {
    
  }

  @Test
  public void testRetract() throws Exception {
    
  }
}
