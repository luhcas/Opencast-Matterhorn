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

import org.opencastproject.media.mediapackage.AudioStream;
import org.opencastproject.util.StringSupport;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;

/**
 * Implementation of {@link org.opencastproject.media.mediapackage.AudioStream}. This implementation shall be hidden.
 * 
 * @author Christoph E. Driessen <ced@neopoly.de>
 */
public class AudioStreamImpl extends AbstractStreamImpl implements AudioStream {

  private Integer resolution;
  private Integer channels;
  private Integer samplingRate;
  private Float bitRate;
  private String captureDevice;
  private String captureDeviceVersion;
  private String captureDeviceVendor;
  private String format;
  private String formatVersion;
  private String encoderLibraryVendor;

  public AudioStreamImpl(String identifier) {
    super(identifier);
  }

  public Node toManifest(Document document) {
    Element node = document.createElement("Audio");
    // Stream ID
    node.setAttribute("id", getIdentifier());

    // Device
    Element deviceNode = document.createElement("device");
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

    // Channels
    if (channels != null) {
      Element channelsNode = document.createElement("channels");
      channelsNode.appendChild(document.createTextNode(channels.toString()));
      node.appendChild(channelsNode);
    }

    // Bit depth
    if (resolution != null) {
      Element bitdepthNode = document.createElement("bitdepth");
      bitdepthNode.appendChild(document.createTextNode(resolution.toString()));
      node.appendChild(bitdepthNode);
    }

    // Bit rate
    if (bitRate != null) {
      Element bitratenode = document.createElement("bitrate");
      bitratenode.appendChild(document.createTextNode(bitRate.toString()));
      node.appendChild(bitratenode);
    }

    // Sampling rate
    if (samplingRate != null) {
      Element samplingrateNode = document.createElement("samplingrate");
      samplingrateNode.appendChild(document.createTextNode(samplingRate.toString()));
      node.appendChild(samplingrateNode);
    }

    return node;
  }

  /**
   * Create an audio stream from the XML manifest.
   * 
   * @param streamIdHint
   *          stream ID that has to be used if the manifest does not provide one. This is the case when reading an old
   *          manifest.
   */
  public static AudioStreamImpl fromManifest(String streamIdHint, Node node, XPath xpath) throws IllegalStateException,
          XPathException {
    // Create stream
    String sid = (String) xpath.evaluate("@id", node, XPathConstants.STRING);
    if (StringSupport.isEmpty(sid))
      sid = streamIdHint;
    AudioStreamImpl as = new AudioStreamImpl(sid);

    // bit depth
    try {
      String bd = (String) xpath.evaluate("bitdepth/text()", node, XPathConstants.STRING);
      if (!StringUtils.isBlank(bd))
        as.resolution = new Integer(bd.trim());
    } catch (NumberFormatException e) {
      throw new IllegalStateException("Bit depth was malformatted: " + e.getMessage());
    }

    // channels
    try {
      String strChannels = (String) xpath.evaluate("channels/text()", node, XPathConstants.STRING);
      if (!StringUtils.isBlank(strChannels))
        as.channels = new Integer(strChannels.trim());
    } catch (NumberFormatException e) {
      throw new IllegalStateException("Number of channels was malformatted: " + e.getMessage());
    }

    // sampling rate
    try {
      String sr = (String) xpath.evaluate("framerate/text()", node, XPathConstants.STRING);
      if (!StringUtils.isBlank(sr))
        as.samplingRate = new Integer(sr.trim());
    } catch (NumberFormatException e) {
      throw new IllegalStateException("Frame rate was malformatted: " + e.getMessage());
    }

    // Bit rate
    try {
      String br = (String) xpath.evaluate("bitrate/text()", node, XPathConstants.STRING);
      if (!StringUtils.isBlank(br))
        as.bitRate = new Float(br.trim());
    } catch (NumberFormatException e) {
      throw new IllegalStateException("Bit rate was malformatted: " + e.getMessage());
    }

    // device
    String captureDevice = (String) xpath.evaluate("device/@type", node, XPathConstants.STRING);
    if (!StringUtils.isBlank(captureDevice))
      as.captureDevice = captureDevice;
    String captureDeviceVersion = (String) xpath.evaluate("device/@version", node, XPathConstants.STRING);
    if (!StringUtils.isBlank(captureDeviceVersion))
      as.captureDeviceVersion = captureDeviceVersion;
    String captureDeviceVendor = (String) xpath.evaluate("device/@vendor", node, XPathConstants.STRING);
    if (!StringUtils.isBlank(captureDeviceVendor))
      as.captureDeviceVendor = captureDeviceVendor;

    // encoder
    String format = (String) xpath.evaluate("encoder/@type", node, XPathConstants.STRING);
    if (!StringUtils.isBlank(format))
      as.format = format;
    String formatVersion = (String) xpath.evaluate("encoder/@version", node, XPathConstants.STRING);
    if (!StringUtils.isBlank(formatVersion))
      as.formatVersion = formatVersion;
    String encoderLibraryVendor = (String) xpath.evaluate("encoder/@vendor", node, XPathConstants.STRING);
    if (!StringUtils.isBlank(encoderLibraryVendor))
      as.encoderLibraryVendor = encoderLibraryVendor;

    return as;
  }

  public Integer getResolution() {
    return resolution;
  }

  public Integer getChannels() {
    return channels;
  }

  public Integer getSamplingRate() {
    return samplingRate;
  }

  public Float getBitRate() {
    return bitRate;
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

  public void setResolution(Integer resolution) {
    this.resolution = resolution;
  }

  public void setChannels(Integer channels) {
    this.channels = channels;
  }

  public void setSamplingRate(Integer samplingRate) {
    this.samplingRate = samplingRate;
  }

  public void setBitRate(Float bitRate) {
    this.bitRate = bitRate;
  }

  public void setCaptureDevice(String captureDevice) {
    this.captureDevice = captureDevice;
  }

  public void setCaptureDeviceVersion(String captureDeviceVersion) {
    this.captureDeviceVersion = captureDeviceVersion;
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