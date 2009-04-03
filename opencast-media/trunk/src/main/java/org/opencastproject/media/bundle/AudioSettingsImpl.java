/**
 *  Copyright 2009 Opencast Project (http://www.opencastproject.org)
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

import org.opencastproject.media.analysis.AudioStreamMetadata;
import org.opencastproject.util.ReflectionSupport;
import org.opencastproject.util.StringSupport;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;

/**
 * Default implementation for audio settings as found in the bundle manifest
 * as part of a presenter or presentation track.
 *
 * @author Tobias Wunden <tobias.wunden@id.ethz.ch>
 * @author Christoph E. Driessen <ced@neopoly.de>
 * @version $Id$
 */
public class AudioSettingsImpl implements AudioSettings, Cloneable {

    private AudioStreamMetadata metadata = new AudioStreamMetadata();

    public AudioSettingsImpl() {
    }

    public AudioSettingsImpl(AudioStreamMetadata metadata) {
        this.metadata = metadata;
    }

    public AudioStreamMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(AudioStreamMetadata metadata) {
        this.metadata = metadata;
    }

    /**
     * Creates a new audio settings object from the given xml document node.
     *
     * @param node the settings node
     * @param xpath the xpath facility
     * @return the audio settings
     * @throws IllegalStateException if the parser encountered an unexpected state
     * @throws XPathException if parsing the settings fails
     */
    public static AudioSettings fromManifest(Node node, XPath xpath) throws IllegalStateException, XPathException {
        AudioSettingsImpl settings = new AudioSettingsImpl();
        AudioStreamMetadata metadata = settings.getMetadata();
        // bit depth
        try {
            String bd = (String) xpath.evaluate("BitDepth/text()", node, XPathConstants.STRING);
            if (!StringSupport.isEmpty(bd))
                metadata.setResolution(new Integer(bd.trim()));
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Bit depth was malformatted: " + e.getMessage());
        }

        // channels
        try {
            String strChannels = (String) xpath.evaluate("Channels/text()", node, XPathConstants.STRING);
            if (!StringSupport.isEmpty(strChannels))
                metadata.setChannels(new Integer(strChannels.trim()));
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Number of channels was malformatted: " + e.getMessage());
        }

        // sampling rate
        try {
            String sr = (String) xpath.evaluate("FrameRate/text()", node, XPathConstants.STRING);
            if (!StringSupport.isEmpty(sr))
                metadata.setSamplingRate(new Integer(sr.trim()));
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Frame rate was malformatted: " + e.getMessage());
        }

        // Bit rate
        try {
            String br = (String) xpath.evaluate("BitRate/text()", node, XPathConstants.STRING);
            if (!StringSupport.isEmpty(br))
                metadata.setBitRate(new Float(br.trim()));
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Bit rate was malformatted: " + e.getMessage());
        }

        // device
        String captureDevice = (String) xpath.evaluate("Device/@type", node, XPathConstants.STRING);
        if (!StringSupport.isEmpty(captureDevice))
            metadata.setCaptureDevice(captureDevice);
        String captureDeviceVersion = (String) xpath.evaluate("Device/@version", node, XPathConstants.STRING);
        if (!StringSupport.isEmpty(captureDeviceVersion))
            metadata.setCaptureDeviceVersion(captureDeviceVersion);
        String captureDeviceVendor = (String) xpath.evaluate("Device/@vendor", node, XPathConstants.STRING);
        if (!StringSupport.isEmpty(captureDeviceVendor))
            metadata.setCaptureDeviceVendor(captureDeviceVendor);

        // encoder
        String format = (String) xpath.evaluate("Encoder/@type", node, XPathConstants.STRING);
        if (!StringSupport.isEmpty(format))
            metadata.setFormat(format);
        String formatVersion = (String) xpath.evaluate("Encoder/@version", node, XPathConstants.STRING);
        if (!StringSupport.isEmpty(formatVersion))
            metadata.setFormatVersion(formatVersion);
        String encoderLibraryVendor = (String) xpath.evaluate("Encoder/@vendor", node, XPathConstants.STRING);
        if (!StringSupport.isEmpty(encoderLibraryVendor))
            metadata.setEncoderLibraryVendor(encoderLibraryVendor);

        return settings;
    }

    /** @see org.opencastproject.media.bundle.AudioSettings#toManifest(org.w3c.dom.Document) */
    public Node toManifest(Document document) {
        Element node = document.createElement("Audio");

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

        // Channels
        if (metadata.getChannels() != null) {
            Element channelsNode = document.createElement("Channels");
            channelsNode.appendChild(document.createTextNode(metadata.getChannels().toString()));
            node.appendChild(channelsNode);
        }

        // Bit depth
        if (metadata.getResolution() != null) {
            Element bitdepthNode = document.createElement("BitDepth");
            bitdepthNode.appendChild(document.createTextNode(metadata.getResolution().toString()));
            node.appendChild(bitdepthNode);
        }

        // Bit rate
        if (metadata.getBitRate() != null) {
            Element bitratenode = document.createElement("BitRate");
            bitratenode.appendChild(document.createTextNode(metadata.getBitRate().toString()));
            node.appendChild(bitratenode);
        }

        // Sampling rate
        if (metadata.getSamplingRate() != null) {
            Element samplingrateNode = document.createElement("SamplingRate");
            samplingrateNode.appendChild(document.createTextNode(metadata.getSamplingRate().toString()));
            node.appendChild(samplingrateNode);
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
            if (metadata.getChannels() != null) {
                buf.append(", ");
                switch (metadata.getChannels()) {
                    case 1:
                        buf.append("Mono");
                        break;
                    case 2:
                        buf.append("Stereo");
                        break;
                    default:
                        buf.append(metadata.getChannels() + " channels");
                        break;
                }
            }
            if (metadata.getResolution() != null) {
                buf.append(", ");
                buf.append(metadata.getResolution());
                buf.append(" bit");
            }
            if (metadata.getSamplingRate() != null) {
                buf.append(", ");
                buf.append(metadata.getSamplingRate());
                buf.append(" bps");
            }
            return buf.toString();
        }
        return "Audio settings (unknown)";
    }
}