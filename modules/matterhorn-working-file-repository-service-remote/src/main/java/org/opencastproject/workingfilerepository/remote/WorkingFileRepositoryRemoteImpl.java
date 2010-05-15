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
package org.opencastproject.workingfilerepository.remote;

import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.workingfilerepository.api.WorkingFileRepository;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.osgi.service.component.ComponentContext;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * A remote service proxy for a working file repository
 */
public class WorkingFileRepositoryRemoteImpl implements WorkingFileRepository {
  public static final String REMOTE_FILE_REPO = "remote.filerepository";
  
  protected TrustedHttpClient client;
  protected String remoteHost;
  
  public void setTrustedHttpClient(TrustedHttpClient client) {
    this.client = client;
  }
  
  public void activate(ComponentContext cc) {
    this.remoteHost = cc.getBundleContext().getProperty(REMOTE_FILE_REPO);
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#copyTo(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public URI copyTo(String fromCollection, String fromFileName, String toMediaPackage, String toMediaPackageElement) {
    String url = UrlSupport.concat(new String[] {remoteHost, "files", "copy", fromCollection, fromFileName, toMediaPackage, toMediaPackageElement});
    HttpPost post = new HttpPost(url);
    HttpResponse response = client.execute(post);
    try {
      String content = EntityUtils.toString(response.getEntity());
      return new URI(content);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#moveTo(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public URI moveTo(String fromCollection, String fromFileName, String toMediaPackage, String toMediaPackageElement) {
    String url = UrlSupport.concat(new String[] {remoteHost, "files", "move", fromCollection, fromFileName, toMediaPackage, toMediaPackageElement});
    HttpPost post = new HttpPost(url);
    HttpResponse response = client.execute(post);
    try {
      String content = EntityUtils.toString(response.getEntity());
      return new URI(content);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * {@inheritDoc}
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#delete(java.lang.String, java.lang.String)
   */
  @Override
  public void delete(String mediaPackageID, String mediaPackageElementID) {
    String url = UrlSupport.concat(new String[] {remoteHost, "files", mediaPackageID, mediaPackageElementID});
    HttpDelete del = new HttpDelete(url);
    HttpResponse response = client.execute(del);
    if(response.getStatusLine().getStatusCode() == HttpStatus.SC_NO_CONTENT) {
      return;
    } else {
      throw new RuntimeException(response.getStatusLine().getReasonPhrase());
    }
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#get(java.lang.String, java.lang.String)
   */
  @Override
  public InputStream get(String mediaPackageID, String mediaPackageElementID) {
    String url = UrlSupport.concat(new String[] {remoteHost, "files", mediaPackageID, mediaPackageElementID});
    HttpGet get = new HttpGet(url);
    HttpResponse response = client.execute(get);
    try {
      return response.getEntity().getContent();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#getCollectionContents(java.lang.String)
   */
  @Override
  public URI[] getCollectionContents(String collectionId) {
    String url = UrlSupport.concat(new String[] {remoteHost, "files", "list", collectionId + ".json"});
    HttpGet get = new HttpGet(url);
    HttpResponse response = client.execute(get);
    try {
      String json = EntityUtils.toString(response.getEntity());
      JSONArray jsonArray = (JSONArray)JSONValue.parse(json);
      URI[] uris = new URI[jsonArray.size()];
      for(int i=0; i< jsonArray.size(); i++) {
        uris[i] = new URI((String)jsonArray.get(i));
      }
      return uris;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#getCollectionSize(java.lang.String)
   */
  @Override
  public long getCollectionSize(String id) {
    return getCollectionContents(id).length;
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#getDiskSpace()
   */
  @Override
  public String getDiskSpace() {
    return (String)getStorageReport().get("summary");
  }

  protected JSONObject getStorageReport() {
    String url = UrlSupport.concat(new String[] {remoteHost, "files", "storage"});
    HttpGet get = new HttpGet(url);
    HttpResponse response = client.execute(get);
    try {
      String json = EntityUtils.toString(response.getEntity());
      return (JSONObject)JSONValue.parse(json);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#getFromCollection(java.lang.String, java.lang.String)
   */
  @Override
  public InputStream getFromCollection(String collectionId, String fileName) {
    String url = UrlSupport.concat(new String[] {remoteHost, "files", "collection", collectionId, fileName});
    HttpGet get = new HttpGet(url);
    HttpResponse response = client.execute(get);
    try {
      return response.getEntity().getContent();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#getTotalSpace()
   */
  @Override
  public long getTotalSpace() {
    return (Long)(getStorageReport().get("size"));
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#getURI(java.lang.String, java.lang.String)
   */
  @Override
  public URI getURI(String mediaPackageID, String mediaPackageElementID) {
    String url = UrlSupport.concat(new String[] {remoteHost, "files", "uri", mediaPackageID, mediaPackageElementID});
    HttpGet get = new HttpGet(url);
    HttpResponse response = client.execute(get);
    try {
      return new URI(EntityUtils.toString(response.getEntity()));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#getUsableSpace()
   */
  @Override
  public long getUsableSpace() {
    return (Long)(getStorageReport().get("usable"));
  }
    
  /**
   * {@inheritDoc}
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#put(java.lang.String, java.lang.String, java.io.InputStream)
   */
  @Override
  public URI put(String mediaPackageID, String mediaPackageElementID, InputStream in) {
    return put(mediaPackageID, mediaPackageElementID, mediaPackageElementID, in);
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#put(java.lang.String, java.lang.String, java.lang.String, java.io.InputStream)
   */
  @Override
  public URI put(String mediaPackageID, String mediaPackageElementID, String filename, InputStream in) {
    String url = UrlSupport.concat(new String[] {remoteHost, "files", "mp", mediaPackageElementID, mediaPackageElementID});
    HttpPost post = new HttpPost(url);
    MultipartEntity entity = new MultipartEntity();
    ContentBody body = new InputStreamBody(in, filename);
    entity.addPart("file", body);
    post.setEntity(entity);
    HttpResponse response = client.execute(post);
    try {
      String content = EntityUtils.toString(response.getEntity());
      return new URI(content);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#putInCollection(java.lang.String, java.lang.String, java.io.InputStream)
   */
  @Override
  public URI putInCollection(String collectionId, String fileName, InputStream in) throws URISyntaxException {
    String url = UrlSupport.concat(new String[] {remoteHost, "files", "collection", collectionId});
    HttpPost post = new HttpPost(url);
    MultipartEntity entity = new MultipartEntity();
    ContentBody body = new InputStreamBody(in, fileName);
    entity.addPart("file", body);
    post.setEntity(entity);
    HttpResponse response = client.execute(post);
    try {
      String content = EntityUtils.toString(response.getEntity());
      return new URI(content);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  /**
   * {@inheritDoc}
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#removeFromCollection(java.lang.String, java.lang.String)
   */
  @Override
  public void removeFromCollection(String collectionId, String fileName) {
    String url = UrlSupport.concat(new String[] {remoteHost, "files", "collection", collectionId});
    HttpDelete del = new HttpDelete(url);
    HttpResponse response = client.execute(del);
    if(response.getStatusLine().getStatusCode() == HttpStatus.SC_NO_CONTENT) return;
    throw new RuntimeException("Error removing file: " + response.getStatusLine().getReasonPhrase());
  }
  
}
