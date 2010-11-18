/**
 *  Copyright 2009, 2010 The Regents of the University of California
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
package org.opencastproject.capture.pipeline;

import org.opencastproject.capture.api.CaptureAgent;
import org.opencastproject.capture.api.CaptureParameters;
import org.opencastproject.capture.pipeline.bins.CaptureDevice;
import org.opencastproject.capture.pipeline.bins.CaptureDeviceBin;
import net.luniks.linux.jv4linfo.JV4LInfo;
import net.luniks.linux.jv4linfo.JV4LInfoException;
import net.luniks.linux.jv4linfo.V4LInfo;

import org.gstreamer.Gst;
import org.gstreamer.Pipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

/**
 * Given a Properties object describing devices this class will create a suitable pipeline to capture from all those
 * devices simultaneously.
 */
public class PipelineFactory {

  public static final Logger logger = LoggerFactory.getLogger(PipelineFactory.class);

  private static Properties properties;
  
  public static boolean broken;
  
  public static int v4LSrcIndex;
  
  protected static CaptureAgent captureAgent = null;

  /**
   * Create a bin that contains multiple pipelines using each source in the properties object as the gstreamer source
   * 
   * @param props
   *          {@code Properties} object defining sources 
   * @return The {@code Pipeline} to control the pipelines
   * @throws Exception 
   * @throws UnsupportedDeviceException
   */
  public static Pipeline create(Properties props, boolean confidence, CaptureAgent ca) {
    properties = props;
    captureAgent = ca;
    ArrayList<CaptureDevice> devices = new ArrayList<CaptureDevice>();

    String[] friendlyNames;
    try {
      friendlyNames = getDeviceNames();
    } catch (NoCaptureDevicesSpecifiedException e) {
      e.printStackTrace();
      return null;
    }
    
    String outputDirectory = properties.getProperty(CaptureParameters.RECORDING_ROOT_URL);

    devices = initDevices(friendlyNames, outputDirectory, confidence);
    if (devices == null) {
      return null;
    }

    return startPipeline(devices, confidence);
  }

  /**
   * Splits the device names from the pipeline's properties.
   * @return The device names to capture from.
   * @throws NoCaptureDevicesSpecifiedException - If there are no capture devices 
   * specified in the configuration file we throw an exception. 
   */
  private static String[] getDeviceNames() throws NoCaptureDevicesSpecifiedException {
    // Setup pipeline for all the devices specified
    String deviceNames = properties.getProperty(CaptureParameters.CAPTURE_DEVICE_NAMES);
    if (deviceNames == null) {
      throw new NoCaptureDevicesSpecifiedException("No capture devices specified in "
              + CaptureParameters.CAPTURE_DEVICE_NAMES);
    }

    //Sanity checks for the device list
    String[] friendlyNames = deviceNames.split(",");
    if (friendlyNames.length < 1) {
      throw new NoCaptureDevicesSpecifiedException("Insufficient number of capture devices listed.  Aborting!");
    } else if (friendlyNames.length == 1) {
      //Java gives us an array even if the string being split is blank...
      if (friendlyNames[0].trim().equals("")) {
        throw new NoCaptureDevicesSpecifiedException("Invalid capture device listed.  Aborting!");
      }
    }

    return friendlyNames;
  }

  /**
   * Returns an {@code ArrayList} of {@code CaptureDevice}s which contain everything the rest of this class needs 
   * to start the pipeline
   * @param friendlyNames  The list of friendly names we will be capturing from.
   * @param outputDirectory  The destination directory of the captures.
   * @param confidence  True to enable confidence monitoring, false otherwise.
   * @return A list of {@code CaptureDevice}s which can be captured from.
   * @throws InvalidDeviceNameException The device name specified in the list of devices could not be found in 
   * the properties list.
   * @throws UnrecognizedDeviceException JV4L could not recognize the device after the type was not specified.
   * @throws UnableToCreateSampleOutputFileException Failure while trying to create a test capture file.
   */
  protected static ArrayList<CaptureDevice> initDevices(String[] friendlyNames, String outputDirectory,
          boolean confidence) {
    ArrayList<CaptureDevice> devices = new ArrayList<CaptureDevice>();
    for (String name : friendlyNames) {
      try {
        name = createDevice(outputDirectory, confidence, devices, name);
      } catch (CannotFindSourceFileOrDeviceException e) {
        logger.error("Can't find source file or device: ", e);
      } catch (InvalidDeviceNameException e) {
        logger.error("Invalid device name: " + name, e);
      } catch (UnableToCreateSampleOutputFileException e) {
        logger.error("Unable to create sample output file " + outputDirectory, e);
      } catch (UnrecognizedDeviceException e) {
        logger.error("Unrecognized device ", e);
      }
    }

    return devices;
  }

  private static String createDevice(String outputDirectory, boolean confidence, ArrayList<CaptureDevice> devices,
          String name) throws InvalidDeviceNameException, UnableToCreateSampleOutputFileException,
          UnrecognizedDeviceException, CannotFindSourceFileOrDeviceException {
    name = name.trim();
    SourceDeviceName devName;
  
    // Get properties from the configuration
    String srcProperty = CaptureParameters.CAPTURE_DEVICE_PREFIX  + name + CaptureParameters.CAPTURE_DEVICE_SOURCE;
    String outputProperty = CaptureParameters.CAPTURE_DEVICE_PREFIX  + name + CaptureParameters.CAPTURE_DEVICE_DEST;
    String typeProperty = CaptureParameters.CAPTURE_DEVICE_PREFIX  + name + CaptureParameters.CAPTURE_DEVICE_TYPE;
    
    if (outputDirectory == null && confidence == false) {
      logger.warn("Output directory is null, this may not work because we may not be able to write to the current " +
        "output dir!");
    }
    if (!properties.containsKey(outputProperty)) {
      throw new InvalidDeviceNameException("Invalid device name: " + name + ".  No keys named " 
              + CaptureParameters.CAPTURE_DEVICE_PREFIX  + name + " exist in the properties!");
    }
    String srcLoc = properties.getProperty(srcProperty);
    File outputFile = new File(outputDirectory, properties.getProperty(outputProperty));
    
    logger.debug("Device {} has source at {}.", name, srcLoc);
    logger.debug("Device {} has output at {}.", name, outputFile);
    
    String type = properties.getProperty(typeProperty);
    logger.debug("Device {} has type {}.", name, type);
    
    //Only try and create an output file if this pipeline will *not* be used for confidence monitoring
    if (!confidence) {
      try {
        if(!outputFile.createNewFile()){
          throw new UnableToCreateSampleOutputFileException("Could not create ouput file for " + name
                  + " file may already exist.");
        }
      } catch (IOException e) {
        throw new UnableToCreateSampleOutputFileException("An error occured while creating output file for " + name
                + ". " + e.getMessage());
      }
    }
    String outputLoc = outputFile.getAbsolutePath();
        
  
    if (srcLoc == null) {
      throw new CannotFindSourceFileOrDeviceException("Unable to create pipeline for " + name
              + " because its source file/device does not exist!");
    }
    
    if(type != null){
      devName = SourceDeviceName.valueOf(type);
      logger.debug("Device {} has been confirmed to be type {}", name, devName.toString());
    }
    else{
      logger.debug("Device {} has no type so we will determine it's type.", name);
      if (new File(srcLoc).isFile()) {
        // Non-V4L file. If it exists, assume it is ingestable
        // TODO: Fix security risk. Any file on CaptureAgent filesytem could be ingested
        devName = SourceDeviceName.FILE;
        logger.debug("Device {} is a File device.", name);
      } else {
        devName = determineSourceFromJ4VLInfo(srcLoc);
      }
    }
    // devices will store the CaptureDevice list arbitrary order
    CaptureDevice capdev = createCaptureDevice(srcLoc, devName, name, outputLoc);
    if (!devices.add(capdev)) {
      logger.error("Unable to add device: {}.", capdev);
    }
    return name;
  }

  private static SourceDeviceName determineSourceFromJ4VLInfo(String srcLoc) throws UnrecognizedDeviceException{
    // ALSA source
    if (srcLoc.contains("hw:")){
      return SourceDeviceName.ALSASRC;
    } else if (srcLoc.equals("dv1394")) {
      return SourceDeviceName.DV_1394;
    } else { // V4L devices
      // Attempt to determine what the device is using the JV4LInfo library 
      try {
        V4LInfo v4linfo = JV4LInfo.getV4LInfo(srcLoc);
        String deviceString = v4linfo.toString();
        if (deviceString.contains("Epiphan VGA2USB"))
          return SourceDeviceName.EPIPHAN_VGA2USB;
        else if (deviceString.contains("Hauppauge") || deviceString.contains("WinTV"))
          return SourceDeviceName.HAUPPAUGE_WINTV;
        else if (deviceString.contains("BT878"))
          return SourceDeviceName.BLUECHERRY_PROVIDEO;
        else {
          throw new UnrecognizedDeviceException("Do not recognized device: " + srcLoc);
          
        }
      } catch (JV4LInfoException e) {
        // The v4l device caused an exception
        if (e.getMessage().equalsIgnoreCase("No medium found")) {
          logger.warn("No VGA signal detected. Trying to start capture regardless...");
          return SourceDeviceName.EPIPHAN_VGA2USB;
        } else {
          throw new UnrecognizedDeviceException("Unexpected jv4linfo exception: " + e.getMessage() + " for " + srcLoc);
        }
      }
    }
  }
  
  /**
   * Initializes the pipeline itself, but does not start capturing
   * 
   * @param devices  The list of devices to capture from.
   * @param confidence  True to enable confidence monitoring.
   * @return The created {@code Pipeline}, or null in the case of an error.
   */
  private static Pipeline startPipeline(ArrayList<CaptureDevice> devices, boolean confidence) {
    logger.info("Successfully initialised {} devices.", devices.size());
    for (int i = 0; i < devices.size(); i++)
      logger.debug("Device #{}: {}.", i, devices.get(i));

    // setup gstreamer pipeline using capture devices 
    Gst.init(); // cannot using gst library without first initialising it

    Pipeline pipeline = new Pipeline();
    
    if (confidence) {
      for (CaptureDevice c : devices) {
        if (c.getName() == SourceDeviceName.ALSASRC)
          AudioMonitoring.getConfidencePipeline(pipeline, c, properties);
        else
          VideoMonitoring.getConfidencePipeline(pipeline, c, properties);
      }
    } else {
      for (CaptureDevice c : devices) {
        if (!addPipeline(c, pipeline))
          logger.error("Failed to create pipeline for {}.", c);
      }
    }

    return pipeline;
  }

  //TODO:  Document me!
  public static CaptureDevice createCaptureDevice(String srcLoc, SourceDeviceName devName, String name, 
          String outputLoc) {
    CaptureDevice capdev = new CaptureDevice(srcLoc, devName, name, outputLoc);
    String codecProperty = CaptureParameters.CAPTURE_DEVICE_PREFIX + name + CaptureParameters.CAPTURE_DEVICE_CODEC;
    String containerProperty = CaptureParameters.CAPTURE_DEVICE_PREFIX + name
            + CaptureParameters.CAPTURE_DEVICE_CONTAINER;
    String bitrateProperty = codecProperty + CaptureParameters.CAPTURE_DEVICE_BITRATE;
    String quantizerProperty = codecProperty + CaptureParameters.CAPTURE_DEVICE_QUANTIZER;
    String bufferProperty = CaptureParameters.CAPTURE_DEVICE_PREFIX + name + CaptureParameters.CAPTURE_DEVICE_BUFFER;
    String bufferCountProperty = bufferProperty + CaptureParameters.CAPTURE_DEVICE_BUFFER_MAX_BUFFERS;
    String bufferByteProperty = bufferProperty + CaptureParameters.CAPTURE_DEVICE_BUFFER_MAX_BYTES;
    String bufferTimeProperty = bufferProperty + CaptureParameters.CAPTURE_DEVICE_BUFFER_MAX_TIME;
    String framerateProperty = CaptureParameters.CAPTURE_DEVICE_PREFIX + name
            + CaptureParameters.CAPTURE_DEVICE_FRAMERATE;
    String codec = properties.getProperty(codecProperty);
    String container = properties.getProperty(containerProperty);
    String bitrate = properties.getProperty(bitrateProperty);
    String quantizer = properties.getProperty(quantizerProperty);
    String bufferCount = properties.getProperty(bufferCountProperty);
    String bufferBytes = properties.getProperty(bufferByteProperty);
    String bufferTime = properties.getProperty(bufferTimeProperty);
    String framerate = properties.getProperty(framerateProperty);

    if (codec != null)
      capdev.properties.setProperty("codec", codec);
    if (bitrate != null)
      capdev.properties.setProperty("bitrate", bitrate);
    if (quantizer != null)
      capdev.properties.setProperty("quantizer", quantizer);
    if (container != null)
      capdev.properties.setProperty("container", container);
    if (bufferCount != null)
      capdev.properties.setProperty("bufferCount", bufferCount);
    if (bufferBytes != null)
      capdev.properties.setProperty("bufferBytes", bufferBytes);
    if (bufferTime != null)
      capdev.properties.setProperty("bufferTime", bufferTime);
    if (framerate != null)
      capdev.properties.setProperty("framerate", framerate);

    return capdev;
  }

  /**
   * addPipeline will add a pipeline for the specified capture device to the bin.
   * 
   * @param captureDevice
   *          {@code CaptureDevice} to create pipeline around
   * @param pipeline
   *          The {@code Pipeline} bin to add it to
   * @return True, if successful
   */
  public static boolean addPipeline(CaptureDevice captureDevice, Pipeline pipeline) {
    
    CaptureDeviceBin captureDeviceBin = null;
    try {
      captureDeviceBin = new CaptureDeviceBin(captureDevice, properties, captureAgent);
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
   
    if(captureDeviceBin != null){
      pipeline.add(captureDeviceBin.getBin());
      return true;
    }
    
    return false;
  }
}


