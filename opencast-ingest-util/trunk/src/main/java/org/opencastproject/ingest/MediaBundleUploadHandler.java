package org.opencastproject.ingest;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.opencastproject.api.OpencastJcrServer;
import org.opencastproject.rest.OpencastRestService;

import java.io.IOException;
import java.util.Calendar;
import java.util.UUID;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

@Path("/upload")
public class MediaBundleUploadHandler implements OpencastRestService {
  OpencastJcrServer jcrServer;
  public MediaBundleUploadHandler(OpencastJcrServer jcrServer) {
    this.jcrServer = jcrServer;
  }
  
  public void setJcrServer(OpencastJcrServer jcrServer) {
    this.jcrServer = jcrServer;
  }
  
  // Copies an input stream directly into the repository
  @POST
  @Produces(MediaType.TEXT_HTML)
  public String uploadMediaBundle(@Context HttpServletRequest request) {
    if ( ! ServletFileUpload.isMultipartContent(request)) {
      return "This URL is for uploading media bundles.";
    }
    ServletFileUpload upload = new ServletFileUpload();

 // Parse the request
    FileItemStream fileItemStream = null;
    try {
      FileItemIterator iter = upload.getItemIterator(request);
      while (iter.hasNext()) {
        FileItemStream item = iter.next();
        if ( ! item.isFormField()) {
          fileItemStream = item;
          break;
        }
      }
    } catch (Exception e) {
      return e.getMessage();
    }
 
    if(fileItemStream == null) {
      return "unable to parse a file from this request";
    }
    if(jcrServer == null) {
      return "unable to connect to media repository";
    }
    Repository repo = jcrServer.getRepository();
    Session session = null;
    try {
      session = repo.login(new SimpleCredentials("admin", "admmin".toCharArray()));
    } catch (LoginException e) {
      return e.getMessage();
    } catch (RepositoryException e) {
      return e.getMessage();
    }
    String fullPath = null;
    try {
      Node root = session.getRootNode();
      Node uploads = null;
      if (root.hasNode("uploads")) {
        uploads = root.getNode("uploads");
      } else {
        uploads = root.addNode("uploads");
      }
      Node uploadNode = uploads.addNode(UUID.randomUUID().toString(), "nt:file");
      Node resNode = uploadNode.addNode("jcr:content", "nt:resource");
      fullPath = resNode.getPath();
      resNode.addMixin("mix:referenceable");
      resNode.setProperty("jcr:mimeType", "application/octet-stream");
      resNode.setProperty("jcr:encoding", "");
      resNode.setProperty("jcr:data", fileItemStream.openStream());
      resNode.setProperty("jcr:lastModified", Calendar.getInstance());
      root.save();
    } catch (RepositoryException e) {
      return e.getMessage();
    } catch (IOException e) {
      return e.getMessage();
    }
    return "<h1>success</h1><div>Node UUID=<b>" + fullPath + "</b></div>";
  }
}
