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
package org.opencastproject.media.mediapackage;

import org.opencastproject.media.mediapackage.dublincore.DublinCoreValue;
import org.opencastproject.media.mediapackage.mpeg7.MediaLocatorImpl;
import org.opencastproject.media.mediapackage.mpeg7.MediaTimeImpl;
import org.opencastproject.util.MimeTypes;

import org.junit.Assert;
import org.junit.Test;

import java.net.URI;


public class MediaPackageElementJaxbSerializationTest {
  @Test
  public void testDublinCoreClone() throws Exception {
    DublinCoreCatalog cat = (DublinCoreCatalog) MediaPackageElementBuilderFactory.newInstance().newElementBuilder().newElement(Catalog.TYPE, DublinCoreCatalog.FLAVOR);
    cat.setIdentifier("123");
    cat.setMimeType(MimeTypes.XML);
    cat.set(DublinCoreCatalog.PROPERTY_TITLE, new DublinCoreValue("testing"));
    DublinCoreCatalog clone = (DublinCoreCatalog)cat.clone();
    Assert.assertEquals(cat.getIdentifier(), clone.getIdentifier());
  }

  @Test
  public void testMpeg7Clone() throws Exception {
    Mpeg7Catalog cat = (Mpeg7Catalog) MediaPackageElementBuilderFactory.newInstance().newElementBuilder().newElement(Catalog.TYPE, Mpeg7Catalog.FLAVOR);
    cat.setIdentifier("123");
    cat.setMimeType(MimeTypes.XML);
    cat.addVideoContent("video1", new MediaTimeImpl(1, 2), new MediaLocatorImpl(new URI("http://nothing")));
    Mpeg7Catalog clone = (Mpeg7Catalog)cat.clone();
    Assert.assertEquals(cat.getIdentifier(), clone.getIdentifier());
    Assert.assertNotNull(clone.getVideoById("video1"));
  }

}
