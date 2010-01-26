/**
 *  Copyright 2009 The Regents of the University of California
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
package org.opencastproject.composer.impl.endpoint;


import org.opencastproject.media.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.media.mediapackage.MediaPackageElements;
import org.opencastproject.media.mediapackage.Track;

import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URI;
import java.util.UUID;

public class ReceiptPersistenceTest {
  private static JdbcConnectionPool cp = null;
  private static ComposerServiceDaoJdbcImpl dao = null;

  /**
   * @throws java.lang.Exception
   */
  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    // instantiate a service implementation and its DAO
    String randomId = UUID.randomUUID().toString();
    cp = JdbcConnectionPool.create("jdbc:h2:target/" + randomId + ";LOCK_MODE=1;MVCC=TRUE", "sa", "sa");

    dao = new ComposerServiceDaoJdbcImpl();
    dao.setDataSource(cp);
    dao.activate(null);
  }

  /**
   * @throws java.lang.Exception
   */
  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    cp.dispose();
  }
  
  @Test
  public void testCrudOperations() throws Exception {
    Receipt receipt = dao.createReceipt();
    Track t = (Track) MediaPackageElementBuilderFactory.newInstance().newElementBuilder().elementFromURI(
            new URI("file://test.mov"), Track.TYPE, MediaPackageElements.PRESENTATION_TRACK);
    receipt.setTrack(t);
    dao.updateReceipt(receipt);
    
    Receipt receiptFromDb = dao.getReceipt(receipt.getId());
    Assert.assertEquals(receipt.getTrack().getIdentifier(), receiptFromDb.getTrack().getIdentifier());
  }
}
