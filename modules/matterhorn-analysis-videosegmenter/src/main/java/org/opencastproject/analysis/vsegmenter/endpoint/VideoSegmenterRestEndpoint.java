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
package org.opencastproject.analysis.vsegmenter.endpoint;

import org.opencastproject.analysis.vsegmenter.VideoSegmenter;
import org.opencastproject.media.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.media.mediapackage.MediaPackageElement;
import org.opencastproject.media.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.receipt.api.Receipt;
import org.opencastproject.util.DocUtil;
import org.opencastproject.util.doc.DocRestData;
import org.opencastproject.util.doc.Format;
import org.opencastproject.util.doc.Param;
import org.opencastproject.util.doc.RestEndpoint;
import org.opencastproject.util.doc.RestTestForm;
import org.opencastproject.util.doc.Param.Type;

import org.apache.commons.io.IOUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * A REST endpoint for the {@link VideoSegmenter} media analysis service
 */
@Path("")
public class VideoSegmenterRestEndpoint {
  private static final Logger logger = LoggerFactory.getLogger(VideoSegmenterRestEndpoint.class);
  protected String docs;
  protected VideoSegmenter videoSegmenter;
  
  public void setVideoSegmenter(VideoSegmenter videoSegmenter) {
    this.videoSegmenter = videoSegmenter;
  }
  
  public void activate(ComponentContext cc) {
    this.docs = generateDocs();
  }

  public void deactivate() {}
  
  @POST
  @Produces(MediaType.TEXT_XML)
  @Path("/analyze")
  public Response analyze(@FormParam("track") String trackAsXml) {
    try {
      DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document doc = docBuilder.parse(IOUtils.toInputStream(trackAsXml));
      MediaPackageElement element = MediaPackageElementBuilderFactory.newInstance().newElementBuilder().elementFromManifest(
              doc.getDocumentElement(), new DefaultMediaPackageSerializerImpl());
      Receipt receipt = videoSegmenter.analyze(element, false);
      return Response.ok(receipt).build();
    } catch(Exception e) {
      logger.warn(e.getMessage(), e);
      return Response.serverError().build();
    }
  }
  
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("receipt/{id}.xml")
  public Response getReceipt(@PathParam("id") String id) {
    Receipt receipt = videoSegmenter.getReceipt(id);
    if(receipt == null) {
      return Response.status(404).build();
    } else {
      return Response.ok(receipt).build();
    }
  }
  

  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocs() {
    return docs;
  }

  protected String generateDocs() {
    DocRestData data = new DocRestData("VideoSegmenter", "Video Segmenter Service", "/vsegmenter/rest",
            new String[] { "$Rev$" });
    // analyze
    RestEndpoint analyzeEndpoint = new RestEndpoint("analyze", RestEndpoint.Method.POST, "/analyze",
            "Submit a track for analysis");
    analyzeEndpoint.addStatus(org.opencastproject.util.doc.Status
            .OK("The receipt to use when polling for the resulting mpeg7 catalog"));
    analyzeEndpoint.addRequiredParam(new Param("track", Type.TEXT, "", "The track to analyze.  Must be a motion jpeg file."));
    analyzeEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.WRITE, analyzeEndpoint);
    
    // receipt
    RestEndpoint receiptEndpoint = new RestEndpoint("receipt", RestEndpoint.Method.GET, "/receipt/{id}.xml",
            "Retrieve a receipt for an analysis task");
    receiptEndpoint.addStatus(org.opencastproject.util.doc.Status.OK(
            "Results in an xml document containing the status of the analysis job, and the catalog produced by this " +
            "analysis job if it the task is finished"));
    receiptEndpoint.addPathParam(new Param("id", Param.Type.STRING, null, "the receipt id"));
    receiptEndpoint.addFormat(new Format("xml", null, null));
    receiptEndpoint.setTestForm(RestTestForm.auto());
    data.addEndpoint(RestEndpoint.Type.READ, receiptEndpoint);

    return DocUtil.generate(data);
  }
}
