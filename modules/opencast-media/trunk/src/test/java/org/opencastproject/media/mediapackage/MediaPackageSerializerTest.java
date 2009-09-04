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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.opencastproject.util.ConfigurationException;

import org.junit.Test;
import org.w3c.dom.Document;

import java.net.MalformedURLException;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/**
 * Test case used to make sure the media package serializer works as expected.
 */
public class MediaPackageSerializerTest extends AbstractMediaPackageTest {

  @Test
  public void testRelativePaths() {
    try {
      XPath xPath = XPathFactory.newInstance().newXPath();
      
      // Create a media package and add an element
      MediaPackage mediaPackage = mediaPackageBuilder.createNew();
      mediaPackage.add(dcFile.toURI().toURL());
      
      // Test relative path, using serializer
      MediaPackageSerializer serializer = null;
      serializer = new DefaultMediaPackageSerializerImpl(manifestFile.getParentFile());
      Document xml = mediaPackage.toXml(serializer);
      String expected = dcFile.getAbsolutePath().substring(packageDir.getAbsolutePath().length() + 1);
      assertEquals(expected, xPath.evaluate("//url", xml));

    } catch (MediaPackageException e) {
      fail("Media package excpetion while reading media package from manifest: " + e.getMessage());
    } catch (ConfigurationException e) {
      fail("Configuration exception while reading media package from manifest: " + e.getMessage());
    } catch (MalformedURLException e) {
      fail("Exception while creating url: " + e.getMessage());
    } catch (UnsupportedElementException e) {
      fail("Error while creating media package: " + e.getMessage());
    } catch (XPathExpressionException e) {
      fail("Selecting node form xml document failed: " + e.getMessage());
    }
  }

}