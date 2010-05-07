var UploadListener = {};

UploadListener.jobId = "";
UploadListener.shortFilename = "";
UploadListener.appletPresent = false;
UploadListener.updateInterval = null;
UploadListener.updateRequested = false;

UploadListener.initialized = function() {
  Upload.log('Uploader initialized');
  $('#track').val();
  $('#BtnBrowse').attr('disabled', false);
  UploadListener.appletPresent = true;
}

UploadListener.resumeReady = function(jobId) {
  Upload.log('resume negotiation successful');
  var filename = document.Uploader.getFilename();
}


UploadListener.fileSelectedAjax = function(filename,jobId) {
  Upload.log("File selected for job " + jobId + ": " + filename);
  UploadListener.shortFilename = filename;
  UploadListener.jobId = jobId;
  $('#track').val(filename);
  var uploadForm = document.getElementById("filechooser-ajax").contentWindow.document.uploadForm;
}

UploadListener.uploadStarted = function() {
  Upload.log('upload started');
  Upload.showProgressStage();
  if (!UploadListener.appletPresent) {
    UploadListener.updateInterval = window.setInterval('UploadListener.getProgress()', 1000);
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
        log('failed to get progress information from ' + '../ingest/rest/getProgress/' + UploadListener.jobId);
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
  Upload.log("transfered: " + transfered + " of " + total + " bytes, " + percentage + "%");
  Upload.setProgress(percentage,percentage,'Total: '+totalMB+' bytes',megaBytes+' MB send');
}

UploadListener.uploadComplete = function() {
  UploadListener.updateRequested = false;
  window.clearInterval(UploadListener.updateInterval);
  Upload.log("upload complete");
  var uploadFrame = document.getElementById("filechooser-ajax");
  var mp = uploadFrame.contentWindow.document.getElementById("mp").value;
  ocIngest.addCatalog(mp, ocIngest.createDublinCoreCatalog(ocIngest.metadata));
}

UploadListener.uploadFailed = function() {
  UploadListener.updateRequested = false;
  window.clearInterval(UploadListener.updateInterval);
  Upload.log('ERROR: media fileupload has failed');
  Upload.showFailedScreen("Media file upload has failed.");
}

UploadListener.error = function(message) {
  UploadListener.updateRequested = false;
  window.clearInterval(UploadListener.updateInterval);
  Upload.log('ERROR: ' + message);
}

UploadListener.log = function(message) {
  Upload.log('Uploader: ' + message);
}
