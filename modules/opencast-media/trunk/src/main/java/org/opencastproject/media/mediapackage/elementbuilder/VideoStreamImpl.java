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

package org.opencastproject.media.mediapackage.elementbuilder;

import org.opencastproject.media.analysis.types.ScanOrder;
import org.opencastproject.media.analysis.types.ScanType;
import org.opencastproject.media.mediapackage.VideoStream;
import org.opencastproject.util.StringSupport;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;

/**
 * Implementation of {@link org.opencastproject.media.mediapackage.VideoStream}.
 * 
 * @author Christoph E. Driessen <ced@neopoly.de>
 */
class VideoStreamImpl extends AbstractStreamImpl implements VideoStream {

  private Float bitRate;
  private Float frameRate;
  private Integer frameWidth;
  private Integer frameHeight;
  private ScanType scanType;
  private ScanOrder scanOrder;
  private String captureDevice;
  private String captureDeviceVersion;
  private String captureDeviceVendor;
  private String format;
  private String formatVersion;
  private String encoderLibraryVendor;

  VideoStreamImpl(String identifier) {
    super(identifier);
  }

  /**
   * Create a video stream from the XML manifest.
   * 
   * @param streamIdHint
   *          stream ID that has to be used if the manifest does not provide one. This is the case when reading an old
   *          manifest.
   */
  public static VideoStreamImpl fromManifest(String streamIdHint, Node node, XPath xpath) throws IllegalStateException,
          XPathException {
    // Create stream
    String sid = (String) xpath.evaluate("@id", node, XPathConstants.STRING);
    if (StringSupport.isEmpty(sid))
      sid = streamIdHint;
    VideoStreamImpl vs = new VideoStreamImpl(sid);

    // bit rate
    try {
      String strBitrate = (String) xpath.evaluate("BitRate/text()", node, XPathConstants.STRING);
      if (strBitrate != null && !strBitrate.trim().equals(""))
        vs.bitRate = new Float(strBitrate.trim());
    } catch (NumberFormatException e) {
      throw new IllegalStateException("Bit rate was malformatted: " + e.getMessage());
    }

    // frame rate
    try {
      String strFrameRate = (String) xpath.evaluate("FrameRate/text()", node, XPathConstants.STRING);
      if (strFrameRate != null && !strFrameRate.trim().equals(""))
        vs.frameRate = new Float(strFrameRate.trim());
    } catch (NumberFormatException e) {
      throw new IllegalStateException("Frame rate was malformatted: " + e.getMessage());
    }

    // size
    String size = (String) xpath.evaluate("Size/text()", node, XPathConstants.STRING);
    if (size == null || size.trim().equals(""))
      throw new IllegalStateException("Video size is missing");
    try {
      String[] s = size.trim().split("x");
      if (s.length != 2)
        throw new IllegalStateException("Video size must be of the form <hsize>x<vsize>, found " + size);
      vs.frameWidth = new Integer(s[0].trim());
      vs.frameHeight = new Integer(s[1].trim());
    } catch (NumberFormatException e) {
      throw new IllegalStateException("Sampling rate was malformatted: " + e.getMessage());
    }

    // interlacing
    String scanType = (String) xpath.evaluate("ScanType/@type", node, XPathConstants.STRING);
    if (scanType != null && !scanType.trim().equals(""))
      vs.scanType = ScanType.fromString(scanType);

    String scanOrder = (String) xpath.evaluate("Interlacing/@order", node, XPathConstants.STRING);
    if (scanOrder != null && !scanOrder.trim().equals(""))
      vs.scanOrder = ScanOrder.fromString(scanOrder);

    // device
    String deviceType = (String) xpath.evaluate("Device/@type", node, XPathConstants.STRING);
    if (deviceType != null && !deviceType.trim().equals(""))
      vs.captureDevice = deviceType;

    String deviceVersion = (String) xpath.evaluate("Device/@version", node, XPathConstants.STRING);
    if (deviceVersion != null && !deviceVersion.trim().equals(""))
      vs.captureDeviceVersion = deviceVersion;

    String deviceVendor = (String) xpath.evaluate("Device/@vendor", node, XPathConstants.STRING);
    if (deviceVendor != null && !deviceVendor.trim().equals(""))
      vs.captureDeviceVendor = deviceVendor;

    // encoder
    String encoderType = (String) xpath.evaluate("Encoder/@type", node, XPathConstants.STRING);
    if (encoderType != null && !encoderType.trim().equals(""))
      vs.format = encoderType;

    String encoderVersion = (String) xpath.evaluate("Encoder/@version", node, XPathConstants.STRING);
    if (encoderVersion != null && !encoderVersion.trim().equals(""))
      vs.formatVersion = encoderVersion;

    String encoderVendor = (String) xpath.evaluate("Encoder/@vendor", node, XPathConstants.STRING);
    if (encoderVendor != null && !encoderVendor.trim().equals(""))
      vs.encoderLibraryVendor = encoderVendor;

    return vs;
  }

  public Node toManifest(Document document) {
    Element node = document.createElement("Video");
    // Stream ID
    node.setAttribute("id", getIdentifier());

    // Device
    Element deviceNode = document.createElement("Device");
    boolean hasAttr = false;
    if (captureDevice != null) {
      deviceNode.setAttribute("type", captureDevice);
      hasAttr = true;
    }
    if (captureDeviceVersion != null) {
      deviceNode.setAttribute("version", captureDeviceVersion);
      hasAttr = true;
    }
    if (captureDeviceVendor != null) {
      deviceNode.setAttribute("vendor", captureDeviceVendor);
      hasAttr = true;
    }
    if (hasAttr)
      node.appendChild(deviceNode);

    // Encoder
    Element encoderNode = document.createElement("Encoder");
    hasAttr = false;
    if (format != null) {
      encoderNode.setAttribute("type", format);
      hasAttr = true;
    }
    if (formatVersion != null) {
      encoderNode.setAttribute("version", formatVersion);
      hasAttr = true;
    }
    if (encoderLibraryVendor != null) {
      encoderNode.setAttribute("vendor", encoderLibraryVendor);
      hasAttr = true;
    }
    if (hasAttr)
      node.appendChild(encoderNode);

    // Size
    Element sizeNode = document.createElement("Size");
    String size = frameWidth + "x" + frameHeight;
    sizeNode.appendChild(document.createTextNode(size));
    node.appendChild(sizeNode);

    // Interlacing
    if (scanType != null) {
      Element interlacingNode = document.createElement("ScanType");
      interlacingNode.setAttribute("type", scanType.toString());
      if (scanOrder != null)
        interlacingNode.setAttribute("order", scanOrder.toString());
      node.appendChild(interlacingNode);
    }

    // Bit rate
    if (bitRate != null) {
      Element bitrateNode = document.createElement("BitRate");
      bitrateNode.appendChild(document.createTextNode(bitRate.toString()));
      node.appendChild(bitrateNode);
    }

    // Frame rate
    if (frameRate != null) {
      Element framerateNode = document.createElement("FrameRate");
      framerateNode.appendChild(document.createTextNode(frameRate.toString()));
      node.appendChild(framerateNode);
    }

    return node;
  }

  public Float getBitRate() {
    return bitRate;
  }

  public Float getFrameRate() {
    return frameRate;
  }

  public Integer getFrameWidth() {
    return frameWidth;
  }

  public Integer getFrameHeight() {
    return frameHeight;
  }

  public ScanType getScanType() {
    return scanType;
  }

  public ScanOrder getScanOrder() {
    return scanOrder;
  }

  public String getCaptureDevice() {
    return captureDevice;
  }

  public String getCaptureDeviceVersion() {
    return captureDeviceVersion;
  }

  public String getCaptureDeviceVendor() {
    return captureDeviceVendor;
  }

  public String getFormat() {
    return format;
  }

  public String getFormatVersion() {
    return formatVersion;
  }

  public String getEncoderLibraryVendor() {
    return encoderLibraryVendor;
  }

  // Setter

  void setBitRate(Float bitRate) {
    this.bitRate = bitRate;
  }

  void setFrameRate(Float frameRate) {
    this.frameRate = frameRate;
  }

  void setFrameWidth(Integer frameWidth) {
    this.frameWidth = frameWidth;
  }

  void setFrameHeight(Integer frameHeight) {
    this.frameHeight = frameHeight;
  }

  void setScanType(ScanType scanType) {
    this.scanType = scanType;
  }

  void setScanOrder(ScanOrder scanOrder) {
    this.scanOrder = scanOrder;
  }

  void setCaptureDevice(String captureDevice) {
    this.captureDevice = captureDevice;
  }

  void setCaptureDeviceVersion(String captureDeviceVersion) {
    this.captureDeviceVersion = captureDeviceVersion;
  }

  void setCaptureDeviceVendor(String captureDeviceVendor) {
    this.captureDeviceVendor = captureDeviceVendor;
  }

  void setFormat(String format) {
    this.format = format;
  }

  void setFormatVersion(String formatVersion) {
    this.formatVersion = formatVersion;
  }

  void setEncoderLibraryVendor(String encoderLibraryVendor) {
    this.encoderLibraryVendor = encoderLibraryVendor;
  }
}
