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

package org.opencastproject.media.mediapackage.track;

import org.opencastproject.media.mediapackage.MediaPackageSerializer;
import org.opencastproject.media.mediapackage.VideoStream;
import org.opencastproject.util.StringSupport;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;

/**
 * Implementation of {@link org.opencastproject.media.mediapackage.videoStream}.
 * 
 * @author Christoph E. Driessen <ced@neopoly.de>
 */
public class VideoStreamImpl extends AbstractStreamImpl implements VideoStream {

  private Float bitRate;
  private Float frameRate;
  private Integer frameWidth;
  private Integer frameHeight;
  private ScanType scanType;
  private ScanOrder scanOrder;
  private String capturedevice;
  private String capturedeviceVersion;
  private String captureDeviceVendor;
  private String format;
  private String formatVersion;
  private String encoderLibraryVendor;

  public VideoStreamImpl(String identifier) {
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
      String strBitrate = (String) xpath.evaluate("bitrate/text()", node, XPathConstants.STRING);
      if (strBitrate != null && !strBitrate.trim().equals(""))
        vs.bitRate = new Float(strBitrate.trim());
    } catch (NumberFormatException e) {
      throw new IllegalStateException("Bit rate was malformatted: " + e.getMessage());
    }

    // frame rate
    try {
      String strFrameRate = (String) xpath.evaluate("framerate/text()", node, XPathConstants.STRING);
      if (strFrameRate != null && !strFrameRate.trim().equals(""))
        vs.frameRate = new Float(strFrameRate.trim());
    } catch (NumberFormatException e) {
      throw new IllegalStateException("Frame rate was malformatted: " + e.getMessage());
    }

    // resolution
    String resolution = (String) xpath.evaluate("resolution/text()", node, XPathConstants.STRING);
    if (resolution == null || resolution.trim().equals(""))
      throw new IllegalStateException("Video resolution is missing");
    try {
      String[] s = resolution.trim().split("x");
      if (s.length != 2)
        throw new IllegalStateException("video size must be of the form <hsize>x<vsize>, found " + resolution);
      vs.frameWidth = new Integer(s[0].trim());
      vs.frameHeight = new Integer(s[1].trim());
    } catch (NumberFormatException e) {
      throw new IllegalStateException("Resolution was malformatted: " + e.getMessage());
    }

    // interlacing
    String scanType = (String) xpath.evaluate("scantype/@type", node, XPathConstants.STRING);
    if (scanType != null && !scanType.trim().equals(""))
      vs.scanType = ScanType.fromString(scanType);

    String scanOrder = (String) xpath.evaluate("interlacing/@order", node, XPathConstants.STRING);
    if (scanOrder != null && !scanOrder.trim().equals(""))
      vs.scanOrder = ScanOrder.fromString(scanOrder);

    // device
    String deviceType = (String) xpath.evaluate("device/@type", node, XPathConstants.STRING);
    if (deviceType != null && !deviceType.trim().equals(""))
      vs.capturedevice = deviceType;

    String deviceVersion = (String) xpath.evaluate("device/@version", node, XPathConstants.STRING);
    if (deviceVersion != null && !deviceVersion.trim().equals(""))
      vs.capturedeviceVersion = deviceVersion;

    String DeviceVendor = (String) xpath.evaluate("device/@vendor", node, XPathConstants.STRING);
    if (DeviceVendor != null && !DeviceVendor.trim().equals(""))
      vs.captureDeviceVendor = DeviceVendor;

    // encoder
    String encoderType = (String) xpath.evaluate("encoder/@type", node, XPathConstants.STRING);
    if (encoderType != null && !encoderType.trim().equals(""))
      vs.format = encoderType;

    String encoderVersion = (String) xpath.evaluate("encoder/@version", node, XPathConstants.STRING);
    if (encoderVersion != null && !encoderVersion.trim().equals(""))
      vs.formatVersion = encoderVersion;

    String encoderVendor = (String) xpath.evaluate("encoder/@vendor", node, XPathConstants.STRING);
    if (encoderVendor != null && !encoderVendor.trim().equals(""))
      vs.encoderLibraryVendor = encoderVendor;

    return vs;
  }

  /**
   * @see org.opencastproject.media.mediapackage.ManifestContributor#toManifest(org.w3c.dom.Document,
   *      org.opencastproject.media.mediapackage.MediaPackageSerializer)
   */
  public Node toManifest(Document document, MediaPackageSerializer serializer) {
    Element node = document.createElement("video");
    // Stream ID
    node.setAttribute("id", getIdentifier());

    // device
    Element deviceNode = document.createElement("device");
    boolean hasAttr = false;
    if (capturedevice != null) {
      deviceNode.setAttribute("type", capturedevice);
      hasAttr = true;
    }
    if (capturedeviceVersion != null) {
      deviceNode.setAttribute("version", capturedeviceVersion);
      hasAttr = true;
    }
    if (captureDeviceVendor != null) {
      deviceNode.setAttribute("vendor", captureDeviceVendor);
      hasAttr = true;
    }
    if (hasAttr)
      node.appendChild(deviceNode);

    // encoder
    Element encoderNode = document.createElement("encoder");
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

    // Resolution
    Element resolutionNode = document.createElement("resolution");
    String size = frameWidth + "x" + frameHeight;
    resolutionNode.appendChild(document.createTextNode(size));
    node.appendChild(resolutionNode);

    // Interlacing
    if (scanType != null) {
      Element interlacingNode = document.createElement("scantype");
      interlacingNode.setAttribute("type", scanType.toString());
      if (scanOrder != null)
        interlacingNode.setAttribute("order", scanOrder.toString());
      node.appendChild(interlacingNode);
    }

    // Bit rate
    if (bitRate != null) {
      Element bitrateNode = document.createElement("bitrate");
      bitrateNode.appendChild(document.createTextNode(bitRate.toString()));
      node.appendChild(bitrateNode);
    }

    // Frame rate
    if (frameRate != null) {
      Element framerateNode = document.createElement("framerate");
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
    return capturedevice;
  }

  public String getCaptureDeviceVersion() {
    return capturedeviceVersion;
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

  public void setBitRate(Float bitRate) {
    this.bitRate = bitRate;
  }

  public void setFrameRate(Float frameRate) {
    this.frameRate = frameRate;
  }

  public void setFrameWidth(Integer frameWidth) {
    this.frameWidth = frameWidth;
  }

  public void setFrameHeight(Integer frameHeight) {
    this.frameHeight = frameHeight;
  }

  public void setScanType(ScanType scanType) {
    this.scanType = scanType;
  }

  public void setScanOrder(ScanOrder scanOrder) {
    this.scanOrder = scanOrder;
  }

  public void setCaptureDevice(String capturedevice) {
    this.capturedevice = capturedevice;
  }

  public void setCaptureDeviceVersion(String capturedeviceVersion) {
    this.capturedeviceVersion = capturedeviceVersion;
  }

  public void setCaptureDeviceVendor(String captureDeviceVendor) {
    this.captureDeviceVendor = captureDeviceVendor;
  }

  public void setFormat(String format) {
    this.format = format;
  }

  public void setFormatVersion(String formatVersion) {
    this.formatVersion = formatVersion;
  }

  public void setEncoderLibraryVendor(String encoderLibraryVendor) {
    this.encoderLibraryVendor = encoderLibraryVendor;
  }

}
