
var ocIngest = ocIngest || {};

ocIngest.debug = true;
ocIngest.mediaPackage = null;
ocIngest.metadata = null;

ocIngest.createMediaPackage = function() {
  Upload.log("creating MediaPackage")
  Upload.setProgress('0%','creating MediaPackage',' ', ' ');
  $.ajax({
    url        : '../ingest/rest/createMediaPackage',
    type       : 'GET',
    dataType   : 'xml',
    error      : function(XHR,status,e){
      showFailedScreen('Could not create MediaPackage on server.');
    },
    success    : function(data, status) {
      Upload.log("MediaPackage created");
      ocIngest.mediaPackage = data;
      if ( (Upload.retryId != '') && ($('.use-file:checked').val() == 'previous-file') ) {
        ocIngest.copyPreviousMediaFile();
      } else {
        var uploadFrame = document.getElementById("filechooser-ajax");
        uploadFrame.contentWindow.document.uploadForm.flavor.value = $('#flavor').val();
        uploadFrame.contentWindow.document.uploadForm.mediaPackage.value = ocUtils.xmlToString(data);
        UploadListener.uploadStarted();
        uploadFrame.contentWindow.document.uploadForm.submit();
      }
    }
  });
}

ocIngest.copyPreviousMediaFile = function() {
  //alert("adding previous file");
  var flavor = $('#previous-file-flavor').val();
  var url = $('#previous-file-url').val();
  $.ajax({
    url        : '../ingest/rest/addTrack',
    type       : 'POST',
    dataType   : 'xml',
    data       : {
      mediaPackage: ocUtils.xmlToString(ocIngest.mediaPackage),
      flavor: flavor,
      url: url
    },
    error      : function(XHR,status,e){
      Upload.showFailedScreen('Could not add DublinCore catalog to MediaPackage.');
    },
    success    : function(data, status) {
      ocIngest.mediaPackage = data;
      ocIngest.addCatalog(ocUtils.xmlToString(ocIngest.mediaPackage), ocIngest.createDublinCoreCatalog(ocIngest.metadata));
    }
  });
}

ocIngest.createDublinCoreCatalog = function(data) {
  var dc = ocUtils.createDoc('dublincore','http://www.opencastproject.org/xsd/1.0/dublincore/');
  dc.documentElement.setAttribute('xmlns:dcterms','http://purl.org/dc/terms/');
  var key = '';
  for (key in data) {
    if (data[key] instanceof Array) {
      jQuery.each(data[key], function(k,val) {
        var elm = dc.createElement('dcterms:' + key);
        elm.appendChild(dc.createTextNode(val));    // FIXME get rid of xmlns="" attribute
        dc.documentElement.appendChild(elm);  
      });
    } else {
      var elm = dc.createElement('dcterms:' + key);
      elm.appendChild(dc.createTextNode(data[key]));    // FIXME get rid of xmlns="" attribute
      dc.documentElement.appendChild(elm);
    }
  }
  return dc;
}

ocIngest.addCatalog = function(mediaPackage, dcCatalog) {
  Upload.log("Creating DublinCore catalog");
  Upload.setProgress('100%','adding Metadata',' ', ' ');
  $.ajax({
    url        : '../ingest/rest/addDCCatalog',
    type       : 'POST',
    dataType   : 'xml',
    data       : {
      mediaPackage: mediaPackage,
      dublinCore: ocUtils.xmlToString(dcCatalog)
    },
    error      : function(XHR,status,e){
      showFailedScreen('Could not add DublinCore catalog to MediaPackage.');
    },
    success    : function(data, status) {
      Upload.log("DublinCore catalog added");
      ocIngest.mediaPackage = data;
      ocIngest.startIngest(data);
    }
  });
}

/*
ocIngest.addTrack = function(mediaPackage, jobId, flavor) {
    log("Adding track to MediaPackage");
    setProgress('100%','adding Track',' ', ' ');
    $.ajax({
    url        : '../fileupload/'+jobId+'/addToMediaPackage',
    type       : 'POST',
    dataType   : 'xml',
    data       : {mediaPackage: ocUtils.xmlToString(mediaPackage), flavor: flavor},
    error      : function(XHR,status,e){
      showFailedScreen();
    },
    success    : function(data, status) {
      log("Track added successfully");
      ocIngest.mediaPackage = data;
      ocIngest.startIngest(data);
    }
  });
}
*/

ocIngest.startIngest = function(mediaPackage) {
  Upload.log("Starting Ingest on MediaPackage with Workflow " + $('#workflow-selector').val());
  Upload.setProgress('100%','starting Ingest',' ', ' ');
  var data = Upload.collectWorkflowConfig();
  data['mediaPackage'] = ocUtils.xmlToString(mediaPackage);
  $.ajax({
    url        : '../ingest/rest/ingest/' + $('#workflow-selector').val(),
    type       : 'POST',
    dataType   : 'text',
    data       : data,
    error      : function(XHR,status,e){
      alert("Error!!");
      showFailedScreen("Could not start Ingest on MediaPackage");
    },
    success    : function(data, status) {
      Upload.hideProgressStage();
      Upload.showSuccessScreen();
    }
  });
}