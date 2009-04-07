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

package org.opencastproject.media.analysis;

import org.opencastproject.media.analysis.types.BitRateMode;
import org.opencastproject.media.analysis.types.FrameRateMode;
import org.opencastproject.media.analysis.types.ScanType;
import org.opencastproject.util.MapBuilder;
import org.opencastproject.util.ReflectionSupport;
import org.opencastproject.util.StringSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * This MediaAnalyzer implementation leverages MediaInfo (<a
 * href="http://mediainfo.sourceforge.net/"
 * >http://mediainfo.sourceforge.net</a>) for analysis.
 * <p/>
 * <strong>Please note</strong> that this implementation is stateful and cannot
 * be shared or used multiple times.
 * 
 * @author Christoph E. Driessen <ced@neopoly.de>
 */
public class MediaInfoAnalyzer extends CmdlineMediaAnalyzerSupport {

  private static final Logger log_ = LoggerFactory
      .getLogger(MediaInfoAnalyzer.class);

  private static final Map<String, Setter> CommonStreamProperties = new MapBuilder<String, Setter>()
      .put("Format", new Setter("format", "string")).put("Format_Profile",
          new Setter("formatProfile", "string")).put("Format/Info",
          new Setter("formatInfo", "string")).put("Format/Url",
          new Setter("formatURL", "url")).put("Duration",
          new Setter("duration", "duration")).put("BitRate",
          new Setter("bitRate", "float")).put("BitRate_Mode",
          new Setter("bitRateMode", "bitRateMode")).put("BitRate_Minimum",
          new Setter("bitRateMinimum", "float")).put("BitRate_Maximum",
          new Setter("bitRateMaximum", "float")).put("BitRate_Nominal",
          new Setter("bitRateNominal", "float")).put("Resolution",
          new Setter("resolution", "int")).put("Encoded_Date",
          new Setter("encodedDate", "date")).toMap();

  private static final Map<StreamSection, Map<String, Setter>> Parser =
    new MapBuilder<StreamSection, Map<String, Setter>>()
      .put(
          StreamSection.general,
          new MapBuilder<String, Setter>().put("FileName",
              new Setter("fileName", "string")).put("FileExtension",
              new Setter("fileExtension", "string")).put("FileSize",
              new Setter("size", "long")).put("Duration",
              new Setter("duration", "duration")).put("OverallBitRate",
              new Setter("bitRate", "float")).put("Encoded_Date",
              new Setter("encodedDate", "date")).toMap())
      .put(
          StreamSection.video,
          new MapBuilder<String, Setter>().putAll(CommonStreamProperties)
          //
              .put("Width", new Setter("frameWidth", "int")).put("Height",
                  new Setter("frameHeight", "int")).put("PixelAspectRatio",
                  new Setter("pixelAspectRatio", "float")).put("FrameRate",
                  new Setter("frameRate", "float")).put("FrameRate_Mode",
                  new Setter("frameRateMode", "frameRateMode")).put("ScanType",
                  new Setter("scanType", "scanType")).put("ScanOrder",
                  new Setter("scanOrder", "scanOrder")).toMap())
      .put(
          StreamSection.audio,
          new MapBuilder<String, Setter>().putAll(CommonStreamProperties)
              //
              .put("Channel(s)", new Setter("channels", "int")).put(
                  "ChannelPositions", new Setter("channelPositions", "string"))
              .put("SamplingRate", new Setter("samplingRate", "int")).put(
                  "SamplingCount", new Setter("samplingCount", "long")).toMap())
      .toMap();

  private StreamSection streamSection;

  /** Holds the current metadata set. */
  private Object currentMetadata;

  // --------------------------------------------------------------------------------------------

  public MediaInfoAnalyzer() {
    super("/usr/local/bin/mediainfo");
  }

  public MediaInfoAnalyzer(String binary) {
    super(binary);
  }

  /* Analysis */

  protected String getAnalysisOptions(File media) {
    return "--Language=raw --Full " + media.getAbsolutePath();
  }

  protected void onAnalysis(String line) {
    // Detect section
    for (StreamSection section : StreamSection.values()) {
      if (section.name().equalsIgnoreCase(line)) {
        streamSection = section;
        log_.debug("New section " + streamSection);
        switch (streamSection) {
        case general:
          currentMetadata = metadata;
          break;
        case video:
          currentMetadata = new VideoStreamMetadata();
          metadata.getVideoStreamMetadata().add(
              (VideoStreamMetadata) currentMetadata);
          break;
        case audio:
          currentMetadata = new AudioStreamMetadata();
          metadata.getAudioStreamMetadata().add(
              (AudioStreamMetadata) currentMetadata);
          break;
        default:
          throw new RuntimeException("Bug: Unknown stream section "
              + streamSection);
        }
        return; // LEAVE
      }
    }

    // Parse data line
    if (currentMetadata != null) {
      // Split the line into key and value
      String[] kv = line.split("\\s*:\\s*", 2);
      Setter setter = Parser.get(streamSection).get(kv[0]);
      if (setter != null) {
        // A setter for this key is registered
        setter.set(currentMetadata, kv[1]);
      }
    }
  }

  /* Version check */

  @Override
  protected String getVersionCheckOptions() {
    return "--Version";
  }

  @Override
  protected boolean onVersionCheck(String line) {
    return "MediaInfoLib - v0.7.8".equals(line);
  }

  // --------------------------------------------------------------------------------------------

  static String convertString(String value) {
    return value;
  }

  static Long convertLong(String value) {
    return new Long(value);
  }

  static Integer convertInt(String value) {
    return new Integer(value);
  }

  static Float convertFloat(String value) {
    return new Float(value);
  }

  static Long convertDuration(String value) {
    return new Long(value);
  }

  static URL convertUrl(String value) throws MalformedURLException {
    return new URL(value);
  }

  static FrameRateMode convertFrameRateMode(String value) {
    if ("cfr".equalsIgnoreCase(value))
      return FrameRateMode.ConstantFrameRate;
    if ("vfr".equalsIgnoreCase(value))
      return FrameRateMode.VariableFrameRate;
    throw new RuntimeException("Cannot parse FrameRateMode " + value);
  }

  static ScanType convertScanType(String value) {
    if ("interlaced".equalsIgnoreCase(value))
      return ScanType.Interlaced;
    if ("progressive".equalsIgnoreCase(value))
      return ScanType.Progressive;
    throw new RuntimeException("Cannot parse ScanType " + value);
  }

  static BitRateMode convertBitRateMode(String value) {
    if ("vbr".equalsIgnoreCase(value))
      return BitRateMode.VariableBitRate;
    if ("cbr".equalsIgnoreCase(value))
      return BitRateMode.ConstantBitRate;
    throw new RuntimeException("Cannot parse BitRateMode " + value);
  }

  static Date convertDate(String value) throws ParseException {
    return new SimpleDateFormat("z yyyy-MM-dd hh:mm:ss").parse(value);
  }

  // --------------------------------------------------------------------------------------------

  private static class Setter {

    private static final String CONVERTER_METHOD_PREFIX = "convert";

    private String property;
    private String converterMethodName;

    private Setter(String property, String type) {
      this.property = property;
      this.converterMethodName = CONVERTER_METHOD_PREFIX
          + StringSupport.capitalize(type);
    }

    public void set(Object target, String value) {
      try {
        Method converter = MediaInfoAnalyzer.class.getDeclaredMethod(
            converterMethodName, String.class);
        Object converted = converter.invoke(null, value);
        ReflectionSupport.setProperty(property, target, converted);
      } catch (NoSuchMethodException e) {
        throw new RuntimeException(e);
      } catch (InvocationTargetException e) {
        throw new RuntimeException(e);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private enum StreamSection {

    general, video, audio
  }
}
