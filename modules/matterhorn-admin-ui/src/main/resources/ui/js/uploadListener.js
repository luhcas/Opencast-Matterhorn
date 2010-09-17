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
var UploadListener = {} || UploadListener;

UploadListener.jobId = "";
UploadListener.shortFilename = "";
UploadListener.appletPresent = false;
UploadListener.updateInterval = null;
UploadListener.updateRequested = false;

UploadListener.initialized = function() {
  ocUtils.log('Uploader initialized');
  $('#track').val();
  $('#BtnBrowse').attr('disabled', false);
  UploadListener.appletPresent = true;
}

UploadListener.resumeReady = function(jobId) {
  ocUtils.log('resume negotiation successful');
  var filename = document.Uploader.getFilename();
}


UploadListener.fileSelectedAjax = function(filename,jobId) {
  ocUtils.log("File selected for job " + jobId + ": " + filename);
  UploadListener.shortFilename = filename;
  UploadListener.jobId = jobId;
  $('#track').val(filename);
  var uploadForm = document.getElementById("filechooser-ajax").contentWindow.document.uploadForm;
  Upload.checkRequiredFields();
}

UploadListener.uploadStarted = function(uploadingFile) {
  ocUtils.log('upload started');
  if (uploadingFile) {
    UploadListener.updateInterval = window.setInterval('UploadListener.getProgress()', 1000);
  } else {
    Upload.setProgress('0%','moving file form Inbox to MediaPackage',' ', ' ');
  }
}

UploadListener.getProgress = function() {
  if (!UploadListener.updateRequested) {
    UploadListener.updateRequested = true;
    $.ajax({
      url        : '../ingest/rest/getProgress/' + UploadListener.jobId,
      type       : 'GET',
      dataType   : 'json',
      error      : function(XHR,status,e){
        ocUtils.log('failed to get progress information from ' + '../ingest/rest/getProgress/' + UploadListener.jobId);
        window.clearInterval(UploadListener.updateInterval); // ie in case of inbox ingest
      },
      success    : function(data, status) {
        UploadListener.updateRequested = false;
        UploadListener.uploadProgress(data.total, data.received);
      }
    });
  }
}

UploadListener.uploadProgress = function(total, transfered) {
  var MEGABYTE = 1024 * 1024;
  var percentage = 0;
  var megaBytes = 0;
  var totalMB = 0;
  if (transfered > 0) {
    percentage = transfered / total * 100;
    percentage = percentage.toFixed(2);
    percentage = percentage + '%';
    megaBytes = transfered / MEGABYTE;
    megaBytes = megaBytes.toFixed(2);
    totalMB = total / MEGABYTE;
    totalMB = totalMB.toFixed(2);
  }
  ocUtils.log("transfered: " + transfered + " of " + total + " MB, " + percentage + "%");
  Upload.setProgress(percentage,percentage,'Total: '+totalMB+' MB',megaBytes+' MB send');
}

UploadListener.uploadComplete = function() {
  UploadListener.updateRequested = false;
  window.clearInterval(UploadListener.updateInterval);
  ocUtils.log("upload complete");
  var uploadFrame = document.getElementById("filechooser-ajax");
  var mp = uploadFrame.contentWindow.document.getElementById("mp").value;
  ocIngest.addCatalog(mp, ocIngest.createDublinCoreCatalog(ocIngest.metadata), 'dublincore/episode');
}

UploadListener.uploadFailed = function() {
  UploadListener.updateRequested = false;
  window.clearInterval(UploadListener.updateInterval);
  ocUtils.log('ERROR: media fileupload has failed');
  Upload.showFailedScreen("Media file upload has failed.");
}

UploadListener.error = function(message) {
  UploadListener.updateRequested = false;
  window.clearInterval(UploadListener.updateInterval);
  ocUtils.log('ERROR: ' + message);
}
