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
package org.opencastproject.workspace.impl;

import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.util.IoSupport;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.workingfilerepository.api.WorkingFileRepository;
import org.opencastproject.workspace.api.NotFoundException;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.TeeInputStream;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.Map.Entry;

/**
 * Implements a simple cache for remote URIs. Delegates methods to {@link WorkingFileRepository} wherever possible.
 * 
 * TODO Implement cache invalidation using the caching headers, if provided, from the remote server.
 */
public class WorkspaceImpl implements Workspace {
  public static final String WORKSPACE_ROOTDIR = "workspace.rootdir";
  private static final Logger logger = LoggerFactory.getLogger(WorkspaceImpl.class);

  protected WorkingFileRepository repo;
  protected TrustedHttpClient trustedHttpClient;
  protected String rootDirectory = null;
  protected Map<String, String> filesystemMappings;
  protected long maxAgeInSeconds = -1;
  protected long garbageCollectionPeriodInSeconds = -1;
  protected Timer garbageFileCollector;

  public WorkspaceImpl() {
    this(null);
  }

  public WorkspaceImpl(String rootDirectory) {
    if (rootDirectory == null) {
      rootDirectory = IoSupport.getSystemTmpDir() + "opencast" + File.separator + "workspace";
    }
    this.rootDirectory = rootDirectory;
  }

  public void activate(ComponentContext cc) {
    // NOTE: warning - the test calls activate() with a NULL cc
    if (cc != null && cc.getBundleContext().getProperty(WORKSPACE_ROOTDIR) != null) {
      // use rootDir from CONFIG
      this.rootDirectory = cc.getBundleContext().getProperty(WORKSPACE_ROOTDIR);
      logger.info("CONFIG " + WORKSPACE_ROOTDIR + ": " + this.rootDirectory);
    }
    createRootDirectory();

    filesystemMappings = new HashMap<String, String>();
    String filesUrl;
    if (cc == null || cc.getBundleContext().getProperty("org.opencastproject.server.url") == null) {
      filesUrl = UrlSupport.DEFAULT_BASE_URL + "/files";
    } else {
      filesUrl = cc.getBundleContext().getProperty("org.opencastproject.server.url") + "/files";
    }

    // Find the working file repository's root directory
    // FIXME: there may not be any local mappings.
    String repoRoot;
    if (cc == null || cc.getBundleContext().getProperty("workingFileRepoPath") == null) {
      repoRoot = System.getProperty("java.io.tmpdir") + File.separator + "opencast" + File.separator
              + "workingfilerepo";
    } else {
      repoRoot = cc.getBundleContext().getProperty("workingFileRepoPath");
    }
    logger.info("Workspace filesystem mapping " + filesUrl + " => " + repoRoot);
    filesystemMappings.put(filesUrl, repoRoot);

    // Set up the garbage file collection timer
    if (cc != null && cc.getBundleContext().getProperty("org.opencastproject.workspace.gc.period") != null) {
      String period = cc.getBundleContext().getProperty("org.opencastproject.workspace.gc.period");
      if (period != null) {
        try {
          garbageCollectionPeriodInSeconds = Long.parseLong(period);
        } catch (NumberFormatException e) {
          logger.warn("Workspace garbage collection period can not be set to {}. Please choose a valid number "
                  + "for the 'org.opencastproject.workspace.gc.period' setting", period);
        }
      }
    }

    if (cc != null && cc.getBundleContext().getProperty("org.opencastproject.workspace.gc.max.age") != null) {
      String age = cc.getBundleContext().getProperty("org.opencastproject.workspace.gc.max.age");
      if (age != null) {
        try {
          maxAgeInSeconds = Long.parseLong(age);
        } catch (NumberFormatException e) {
          logger.warn("Workspace garbage collection max age can not be set to {}. Please choose a valid number "
                  + "for the 'org.opencastproject.workspace.gc.max.age' setting", age);
        }
      }
    }
    activateGarbageFileCollectionTimer();
  }

  /**
   * Activate the garbage collection timer
   * @param cc The component context
   */
  protected void activateGarbageFileCollectionTimer() {
    if (garbageCollectionPeriodInSeconds > 0 && maxAgeInSeconds > 0) {
      logger.info("Workspace garbage collection policy: delete files older than {} seconds, scan every {} seconds.",
              maxAgeInSeconds, garbageCollectionPeriodInSeconds);
      garbageFileCollector = new Timer("Workspace Garbage File Collector");
      garbageFileCollector.schedule(new GarbageCollectionTimer(), 0, garbageCollectionPeriodInSeconds * 1000);
    }
  }

  public void deactivate() {
    if(garbageFileCollector != null) {
      garbageFileCollector.cancel();
    }
  }

  /**
   * If this URL is available on a mounted filesystem, return the file handle (otherwise, null).
   * 
   * @param urlString
   *          The URL as a string
   * @return The file, or null if the file is not on a configured mount.
   */
  protected File getLocallyMountedFile(String urlString) {
    if (urlString.startsWith("file")) {
      try {
        File f = new File(new URI(urlString));
        if (f.exists() && f.canRead())
          return f;
      } catch (URISyntaxException e) {
      }
    }
    for (Entry<String, String> entry : filesystemMappings.entrySet()) {
      String baseUrl = entry.getKey();
      String baseFilesystemPath = entry.getValue();
      if (urlString.startsWith(baseUrl)) {
        String pathExtraInfo = urlString.substring(baseUrl.length());
        File f = new File(baseFilesystemPath + pathExtraInfo);
        if (f.exists() && f.isFile() && f.canRead()) {
          logger.debug("found local file {} for URL {}", f.getAbsolutePath(), urlString);
          return f;
        }
      }
    }
    return null;
  }

  public File get(URI uri) throws NotFoundException {
    String urlString = uri.toString();

    // If any local filesystem mappings match this uri, just return the file handle
    File localFile = getLocallyMountedFile(urlString);
    if (localFile != null)
      return localFile;

    String safeFilename = toFilesystemSafeName(urlString);
    String fullPath = rootDirectory + File.separator + safeFilename;
    // See if there's a matching file under the root directory
    File f = new File(fullPath);
    if (f.exists()) {
      return f;
    } else {
      logger.info("Copying {} to {}", urlString, f.getAbsolutePath());
      HttpGet get = new HttpGet(urlString);
      InputStream in = null;
      OutputStream out = null;
      try {
        HttpResponse response = trustedHttpClient.execute(get);
        in = response.getEntity().getContent();
        out = new FileOutputStream(f);
        IOUtils.copyLarge(in, out);
      } catch (Exception e) {
        logger.warn("Could not copy {} to {}", urlString, f.getAbsolutePath());
        throw new NotFoundException(e);
      } finally {
        IOUtils.closeQuietly(in);
        IOUtils.closeQuietly(out);
      }
      return f;
    }
  }

  protected String toFilesystemSafeName(String urlString) {
    String urlExtension = FilenameUtils.getExtension(urlString);
    String baseName = urlString.substring(0, urlString.length() - urlExtension.length());
    String safeBaseName = baseName.replaceAll("\\W", ""); // TODO -- ensure that this filename is safe on all platforms
    String safeString = null;
    if ("".equals(urlExtension)) {
      safeString = safeBaseName;
    } else {
      safeString = safeBaseName + "." + urlExtension;
    }
    if (safeString.length() < 255)
      return safeString;
    String random = UUID.randomUUID().toString();
    random = random.concat(".").concat(urlExtension);
    logger.info("using '{}' to represent url '{}', which is too long to store as a filename", random, urlString);
    return random;
  }

  public void delete(String mediaPackageID, String mediaPackageElementID) throws NotFoundException {
    repo.delete(mediaPackageID, mediaPackageElementID);
  }

  public InputStream get(String mediaPackageID, String mediaPackageElementID) throws NotFoundException {
    URI uri = repo.getURI(mediaPackageID, mediaPackageElementID);
    File f = get(uri);
    try {
      return new FileInputStream(f);
    } catch (FileNotFoundException e) {
      throw new NotFoundException(e);
    }
  }

  public URI put(String mediaPackageID, String mediaPackageElementID, String fileName, InputStream in) {
    // Ensure the filename doesn't contain any path separators
    fileName = fileName.replace(File.separator, "");

    // Store this stream in a temp file so we can cache it quickly
    File tempFile = null;
    FileOutputStream out = null;
    try {
      tempFile = new File(rootDirectory, mediaPackageID + mediaPackageElementID + fileName);
      out = new FileOutputStream(tempFile);
    } catch (IOException e) {
      throw new RuntimeException(e); // this should never happen
    }
    InputStream tee = new TeeInputStream(in, out, true);
    String safeFilename = toFilesystemSafeName(fileName);
    URI uri = repo.put(mediaPackageID, mediaPackageElementID, safeFilename, tee);
    try {
      tee.close();
      out.close();
    } catch (IOException e) {
      logger.warn("Unable to close file stream: " + e.getLocalizedMessage());
    }

    File localFile = getLocallyMountedFile(uri.toString());
    if (localFile == null) {
      // The working file repo isn't mounted locally, so cache the file for subsequent calls to get(URI)
      // TODO uri can be null. Fix this in the repo API.
      File newFile = new File(rootDirectory, toFilesystemSafeName(uri.toString()));
      boolean success = tempFile.renameTo(newFile);
      if (!success)
        throw new IllegalStateException("could not cache " + uri + " at " + newFile.getAbsolutePath());
    } else {
      // remove the temp file
      tempFile.delete();
    }

    return uri;
  }

  public void setRepository(WorkingFileRepository repo) {
    this.repo = repo;
  }

  public void setTrustedHttpClient(TrustedHttpClient trustedHttpClient) {
    this.trustedHttpClient = trustedHttpClient;
  }

  private void createRootDirectory() {
    File f = new File(this.rootDirectory);
    if (!f.exists()) {
      try {
        FileUtils.forceMkdir(f);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workspace.api.Workspace#getURI(java.lang.String, java.lang.String)
   */
  public URI getURI(String mediaPackageID, String mediaPackageElementID) throws NotFoundException {
    URI uri = repo.getURI(mediaPackageID, mediaPackageElementID);
    if (uri == null)
      throw new NotFoundException();
    return uri;
  }

  class GarbageCollectionTimer extends TimerTask {
    /**
     * {@inheritDoc}
     * 
     * @see java.util.TimerTask#run()
     */
    @Override
    public void run() {
      logger.info("Running workspace garbage file collection");
      // Remove any file that was created more than maxAge seconds ago
      File root = new File(rootDirectory);
      File[] oldFiles = root.listFiles(new FileFilter() {
        public boolean accept(File pathname) {
          long ageInSeconds = (System.currentTimeMillis() - pathname.lastModified()) / 1000;
          return ageInSeconds > maxAgeInSeconds;
        }
      });
      for (File oldFile : oldFiles) {
        long ageInSeconds = (System.currentTimeMillis() - oldFile.lastModified()) / 1000;
        Object[] loggingArgs = new Object[] { oldFile, ageInSeconds - maxAgeInSeconds, maxAgeInSeconds };
        if (oldFile.delete()) {
          logger.info("Deleted {}, since its age was {} seconds older than the maximum age, {}", loggingArgs);
        } else {
          logger.warn("Can not delete {}, even though it is {} seconds older than the maximum age, {}", loggingArgs);
        }
      }
    }
  }
}
