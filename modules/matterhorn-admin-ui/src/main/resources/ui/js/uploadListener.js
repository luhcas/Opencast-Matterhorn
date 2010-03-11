var UploadListener = {};

UploadListener.jobId = "";
UploadListener.shortFilename = "";
UploadListener.appletPresent = false;
UploadListener.updateInterval = null;
UploadListener.updateRequested = false;

UploadListener.initialized = function() {
  log('Uploader initialized');
  $('#track').val();
  $('#BtnBrowse').attr('disabled', false);
  UploadListener.appletPresent = true;
}

UploadListener.resumeReady = function(jobId) {
  log('resume negotiation successful');
  var filename = document.Uploader.getFilename();
}


UploadListener.fileSelectedAjax = function(filename,jobId) {
  log("File selected for job " + jobId + ": " + filename);
  UploadListener.shortFilename = filename;
  UploadListener.jobId = jobId;
  $('#track').val(filename);
  var uploadForm = document.getElementById("filechooser-ajax").contentWindow.document.uploadForm;
}

UploadListener.uploadStarted = function() {
  log('upload started');
  showProgressStage();
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
  var percentage = 0;
  if (transfered > 0) {
    percentage = transfered / total * 100;
    percentage = percentage.toFixed(2);
    percentage = percentage + '%';
  }
  log("transfered: " + transfered + " of " + total + " bytes, " + percentage + "%");
  setProgress(percentage,percentage,'Total: '+total+' bytes',transfered+' bytes send');
}

UploadListener.uploadComplete = function() {
  UploadListener.updateRequested = false;
  window.clearInterval(UploadListener.updateInterval);
  log("upload complete");
  var uploadFrame = document.getElementById("filechooser-ajax");
  var mp = uploadFrame.contentWindow.document.getElementById("mp").value;
  ocIngest.addCatalog(mp, ocIngest.createDublinCoreCatalog(ocIngest.metadata));
}

UploadListener.uploadFailed = function() {
  UploadListener.updateRequested = false;
  window.clearInterval(UploadListener.updateInterval);
  log('ERROR: media fileupload has failed');
  showFailedScreen("Media file upload has failed.");
}

UploadListener.error = function(message) {
  UploadListener.updateRequested = false;
  window.clearInterval(UploadListener.updateInterval);
  log('ERROR: ' + message);
}

UploadListener.log = function(message) {
  log('Uploader: ' + message);
}
