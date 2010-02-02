/**
 *  Copyright 2010 The Regents of the University of California
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

import org.opencastproject.media.mediapackage.MediaPackageElement.Type;
import org.opencastproject.media.mediapackage.identifier.IdImpl;
import org.opencastproject.media.mediapackage.track.TrackImpl;
import org.opencastproject.media.mediapackage.track.VideoStreamImpl;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.ChecksumType;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URL;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

public class MediaPackageJaxbSerializationTest {
  static JAXBContext context;
  
  @BeforeClass
  public static void setup() throws Exception {
    context = JAXBContext.newInstance("org.opencastproject.media.mediapackage", MediaPackageJaxbSerializationTest.class.getClassLoader());
  }
  
  @Test
  public void testManifestSerialization() throws Exception {
    MediaPackageBuilderFactory builderFactory = MediaPackageBuilderFactory.newInstance();
    MediaPackageBuilder mediaPackageBuilder = builderFactory.newMediaPackageBuilder();
    URL rootUrl = getClass().getResource("/");
    mediaPackageBuilder.setSerializer(new DefaultMediaPackageSerializerImpl(rootUrl));
    
    // Load the simple media package
    MediaPackage mediaPackage = null;
    InputStream is = getClass().getResourceAsStream("/manifest-simple.xml");
    mediaPackage = mediaPackageBuilder.loadFromXml(is);
    
    Assert.assertEquals(0, mediaPackage.getTracks().length);
    Assert.assertEquals(1, mediaPackage.getCatalogs().length);
    Assert.assertEquals(0, mediaPackage.getAttachments().length);
  }
  
  @Test
  public void testJaxbSerialization() throws Exception {
    // Build a media package
    MediaPackageElementBuilder elementBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
    MediaPackage mp = new MediaPackageImpl(new IdImpl("123"));
    Attachment attachment = (Attachment)elementBuilder.elementFromURI(new URI("http://opencastproject.org/index.html"),
            Type.Attachment, Attachment.FLAVOR);
    attachment.setChecksum(Checksum.create(ChecksumType.DEFAULT_TYPE, "123456abcd"));
    mp.add(attachment);
    Catalog cat1 = (Catalog)elementBuilder.elementFromURI(new URI("http://opencastproject.org/index.html"),
            Catalog.TYPE, MediaPackageElements.DUBLINCORE_CATALOG);
    cat1.setChecksum(Checksum.create(ChecksumType.DEFAULT_TYPE, "7891011abcd"));
    mp.add(cat1);
    
    Catalog cat2 = (Catalog)elementBuilder.elementFromURI(new URI("http://opencastproject.org/index.html"),
            Catalog.TYPE, MediaPackageElements.DUBLINCORE_CATALOG);
    cat2.setChecksum(Checksum.create(ChecksumType.DEFAULT_TYPE, "7891011abcd"));
    mp.addDerived(cat2, cat1);
    
    TrackImpl track = (TrackImpl)elementBuilder.elementFromURI(new URI("http://opencastproject.org/video.mpg"), Track.TYPE, MediaPackageElements.PRESENTER_TRACK);
    track.addStream(new VideoStreamImpl("video-stream-1"));
    track.addStream(new VideoStreamImpl("video-stream-2"));
    mp.add(track);
    
    // Serialize the media package
    Marshaller marshaller = context.createMarshaller();
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

    Writer writer = new StringWriter();
    marshaller.marshal(mp, writer);
    String xml = writer.toString();
    Assert.assertNotNull(xml);
    
    // Deserialize the media package
    Unmarshaller unmarshaller = context.createUnmarshaller();
    MediaPackage deserialized = (MediaPackage)unmarshaller.unmarshal(IOUtils.toInputStream(xml));
    
    // Ensure that the deserialized mediapackage is correct
    Assert.assertEquals(2, deserialized.getCatalogs().length);
    Assert.assertEquals(1, deserialized.getAttachments().length);
    Assert.assertEquals(1, deserialized.getTracks().length);
    Assert.assertEquals(2, deserialized.getTracks()[0].getStreams().length);
    Assert.assertEquals(1, deserialized.getCatalogs(new MediaPackageReferenceImpl(cat1)).length);
  }

  /**
   * JAXB produces xml with an xsi:type="" attribute on the root element.  Be sure that we can unmarshall objects without
   * that attribute.
   */
  @Test
  public void testJaxbWithoutXsi() throws Exception {
    String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><ns2:mediapackage start=\"0\" id=\"123\" duration=\"0\" xmlns:ns2=\"http://mediapackage.opencastproject.org\"><metadata><catalog type=\"metadata/dublincore\"><mimetype>text/xml</mimetype><tags/><checksum type=\"md5\">7891011abcd</checksum><url>http://opencastproject.org/index.html</url></catalog></metadata><attachments><attachment id=\"attachment-1\"><tags/><checksum type=\"md5\">123456abcd</checksum><url>http://opencastproject.org/index.html</url></attachment></attachments></ns2:mediapackage>";
    Unmarshaller unmarshaller = context.createUnmarshaller();
    MediaPackage mp = (MediaPackage)unmarshaller.unmarshal(IOUtils.toInputStream(xml));
    Marshaller marshaller = context.createMarshaller();
    Writer writer = new StringWriter();
    marshaller.marshal(mp, writer);
    Assert.assertEquals(2, mp.getElements().length);
  }
  
  @Test
  public void testJaxbUnmarshallingFromFile() throws Exception {
    Unmarshaller unmarshaller = context.createUnmarshaller();
    InputStream in = this.getClass().getResourceAsStream("/manifest.xml");
    MediaPackage mp = (MediaPackage)unmarshaller.unmarshal(in);
    Assert.assertEquals(2, mp.getTracks().length);
    Assert.assertTrue(mp.getTracks()[0].hasVideo());
    Assert.assertTrue( ! mp.getTracks()[0].hasAudio());
    Assert.assertTrue(mp.getTracks()[1].hasAudio());
    Assert.assertTrue( ! mp.getTracks()[1].hasVideo());
    Assert.assertEquals(3, mp.getCatalogs().length);
    Assert.assertEquals(2, mp.getAttachments().length);
  }
}
