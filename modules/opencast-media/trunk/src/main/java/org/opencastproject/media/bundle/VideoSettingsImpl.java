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

package org.opencastproject.media.bundle;

import org.opencastproject.media.analysis.VideoStreamMetadata;
import org.opencastproject.media.analysis.types.ScanOrder;
import org.opencastproject.media.analysis.types.ScanType;
import org.opencastproject.util.ReflectionSupport;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;

/**
 * Default implementation for video settings as found in the bundle manifest as
 * part of a presenter or presentation track.
 * 
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @author Christoph E. Driessen <ced@neopoly.de>
 * @version $Id$
 */
public class VideoSettingsImpl implements VideoSettings, Cloneable {

  protected VideoStreamMetadata metadata = new VideoStreamMetadata();

  public VideoSettingsImpl() {
  }

  public VideoSettingsImpl(VideoStreamMetadata metadata) {
    this.metadata = metadata;
  }

  public VideoStreamMetadata getMetadata() {
    return metadata;
  }

  public void setMetadata(VideoStreamMetadata metadata) {
    this.metadata = metadata;
  }

  /**
   * Creates a new video settings object from the given xml document node.
   * 
   * @param node
   *          the settings node
   * @param xpath
   *          the xpath facility
   * @return the video settings
   * @throws IllegalStateException
   *           if the parser encountered an unexpected state
   * @throws XPathException
   *           if parsing the settings fails
   */
  public static VideoSettings fromManifest(Node node, XPath xpath)
      throws IllegalStateException, XPathException {
    VideoSettingsImpl settings = new VideoSettingsImpl();
    VideoStreamMetadata metadata = settings.getMetadata();
    // bit rate
    try {
      String strBitrate = (String) xpath.evaluate("BitRate/text()", node,
          XPathConstants.STRING);
      if (strBitrate != null && !strBitrate.trim().equals(""))
        metadata.setBitRate(new Float(strBitrate.trim()));
    } catch (NumberFormatException e) {
      throw new IllegalStateException("Bit rate was malformatted: "
          + e.getMessage());
    }

    // frame rate
    try {
      String strFrameRate = (String) xpath.evaluate("FrameRate/text()", node,
          XPathConstants.STRING);
      if (strFrameRate != null && !strFrameRate.trim().equals(""))
        metadata.setFrameRate(new Float(strFrameRate.trim()));
    } catch (NumberFormatException e) {
      throw new IllegalStateException("Frame rate was malformatted: "
          + e.getMessage());
    }

    // size
    String size = (String) xpath.evaluate("Size/text()", node,
        XPathConstants.STRING);
    if (size == null || size.trim().equals(""))
      throw new IllegalStateException("Video size is missing");
    try {
      String[] s = size.trim().split("x");
      if (s.length != 2)
        throw new IllegalStateException(
            "Video size must be of the form <hsize>x<vsize>, found " + size);
      metadata.setFrameWidth(new Integer(s[0].trim()));
      metadata.setFrameHeight(new Integer(s[1].trim()));
    } catch (NumberFormatException e) {
      throw new IllegalStateException("Sampling rate was malformatted: "
          + e.getMessage());
    }

    // interlacing
    String scanType = (String) xpath.evaluate("ScanType/@type", node,
        XPathConstants.STRING);
    if (scanType != null && !scanType.trim().equals(""))
      metadata.setScanType(ScanType.fromString(scanType));

    String scanOrder = (String) xpath.evaluate("Interlacing/@order", node,
        XPathConstants.STRING);
    if (scanOrder != null && !scanOrder.trim().equals(""))
      metadata.setScanOrder(ScanOrder.fromString(scanOrder));

    // device
    String deviceType = (String) xpath.evaluate("Device/@type", node,
        XPathConstants.STRING);
    if (deviceType != null && !deviceType.trim().equals(""))
      metadata.setCaptureDevice(deviceType);

    String deviceVersion = (String) xpath.evaluate("Device/@version", node,
        XPathConstants.STRING);
    if (deviceVersion != null && !deviceVersion.trim().equals(""))
      metadata.setCaptureDeviceVersion(deviceVersion);

    String deviceVendor = (String) xpath.evaluate("Device/@vendor", node,
        XPathConstants.STRING);
    if (deviceVendor != null && !deviceVendor.trim().equals(""))
      metadata.setCaptureDeviceVendor(deviceVendor);

    // encoder
    String encoderType = (String) xpath.evaluate("Encoder/@type", node,
        XPathConstants.STRING);
    if (encoderType != null && !encoderType.trim().equals(""))
      metadata.setFormat(encoderType);

    String encoderVersion = (String) xpath.evaluate("Encoder/@version", node,
        XPathConstants.STRING);
    if (encoderVersion != null && !encoderVersion.trim().equals(""))
      metadata.setFormatVersion(encoderVersion);

    String encoderVendor = (String) xpath.evaluate("Encoder/@vendor", node,
        XPathConstants.STRING);
    if (encoderVendor != null && !encoderVendor.trim().equals(""))
      metadata.setEncoderLibraryVendor(encoderVendor);

    return settings;
  }

  /** @see org.opencastproject.media.bundle.VideoSettings#toManifest(org.w3c.dom.Document) */
  public Node toManifest(Document document) {
    Element node = document.createElement("Video");

    // Device
    Element deviceNode = document.createElement("Device");
    boolean hasAttr = false;
    if (metadata.getCaptureDevice() != null) {
      deviceNode.setAttribute("type", metadata.getCaptureDevice());
      hasAttr = true;
    }
    if (metadata.getCaptureDeviceVersion() != null) {
      deviceNode.setAttribute("version", metadata.getCaptureDeviceVersion());
      hasAttr = true;
    }
    if (metadata.getCaptureDeviceVendor() != null) {
      deviceNode.setAttribute("vendor", metadata.getCaptureDeviceVendor());
      hasAttr = true;
    }
    if (hasAttr)
      node.appendChild(deviceNode);

    // Encoder
    Element encoderNode = document.createElement("Encoder");
    hasAttr = false;
    if (metadata.getFormat() != null) {
      encoderNode.setAttribute("type", metadata.getFormat());
      hasAttr = true;
    }
    if (metadata.getFormatVersion() != null) {
      encoderNode.setAttribute("version", metadata.getFormatVersion());
      hasAttr = true;
    }
    if (metadata.getEncoderLibraryVendor() != null) {
      encoderNode.setAttribute("vendor", metadata.getEncoderLibraryVendor());
      hasAttr = true;
    }
    if (hasAttr)
      node.appendChild(encoderNode);

    // Size
    Element sizeNode = document.createElement("Size");
    String size = metadata.getFrameWidth() + "x" + metadata.getFrameHeight();
    sizeNode.appendChild(document.createTextNode(size));
    node.appendChild(sizeNode);

    // Interlacing
    if (metadata.getScanType() != null) {
      Element interlacingNode = document.createElement("ScanType");
      interlacingNode.setAttribute("type", metadata.getScanType().toString());
      if (metadata.getScanOrder() != null)
        interlacingNode.setAttribute("order", metadata.getScanOrder()
            .toString());
      node.appendChild(interlacingNode);
    }

    // Bit rate
    if (metadata.getBitRate() != null) {
      Element bitrateNode = document.createElement("BitRate");
      bitrateNode.appendChild(document.createTextNode(metadata.getBitRate()
          .toString()));
      node.appendChild(bitrateNode);
    }

    // Frame rate
    if (metadata.getFrameRate() != null) {
      Element framerateNode = document.createElement("FrameRate");
      framerateNode.appendChild(document.createTextNode(metadata.getFrameRate()
          .toString()));
      node.appendChild(framerateNode);
    }

    return node;
  }

  /** @see java.lang.Object#clone() */
  @Override
  public Object clone() throws CloneNotSupportedException {
    VideoSettingsImpl s = new VideoSettingsImpl();
    ReflectionSupport.copy(metadata, s.getMetadata());
    return s;
  }

  /** @see java.lang.Object#toString() */
  @Override
  public String toString() {
    if (metadata.getFormat() != null) {
      StringBuffer buf = new StringBuffer(metadata.getFormat());
      buf.append(", ");
      buf.append(metadata.getFrameWidth());
      buf.append("x");
      buf.append(metadata.getFrameHeight());
      buf.append(", ");
      buf.append(metadata.getFrameRate());
      buf.append(" fps");
      return buf.toString();
    }
    return "Video settings (unknown)";
  }

}