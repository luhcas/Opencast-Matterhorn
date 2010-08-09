
var ocIngest = ocIngest || {};

ocIngest.debug = true;
ocIngest.mediaPackage = null;
ocIngest.metadata = null;
ocIngest.seriesDC = null;
ocIngest.previousMediaPackage = null;
ocIngest.previousFiles = new Array();

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
      if (Upload.retryId != '') {
        // add tracks from old mediaPackage to the new one
        Upload.log("adding files from previous mediaPackge");
        ocIngest.copyPreviousFiles(ocIngest.mediaPackage);
      } else {
        var uploadFrame = document.getElementById("filechooser-ajax");
        uploadFrame.contentWindow.document.uploadForm.flavor.value = $('#flavor').val();
        uploadFrame.contentWindow.document.uploadForm.mediaPackage.value = ocUtils.xmlToString(data);
        var uploadingFile = $('#ingest-upload').is(':checked');
        UploadListener.uploadStarted(uploadingFile);
        uploadFrame.contentWindow.document.uploadForm.submit();
      }
    }
  });
}

/* not needed for now
ocIngest.copyPreviousMediaFile = function() {
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
}*/

ocIngest.copyPreviousFiles = function(data) {
  if (ocIngest.previousFiles.length != 0) {
    var fileItem = ocIngest.previousFiles.pop();
    $.ajax({
      url        : '../ingest/rest/addTrack',
      type       : 'POST',
      dataType   : 'xml',
      data       : {
        mediaPackage: ocUtils.xmlToString(ocIngest.mediaPackage),
        flavor: fileItem.flavor,
        url: fileItem.url
      },
      error      : function(XHR,status,e){
        Upload.showFailedScreen('Could not add DublinCore catalog to MediaPackage.');
      },
      success    : function(data, status) {
        ocIngest.mediaPackage = data;
        ocIngest.copyPreviousFiles(data);
      }
    });
  } else {
    ocIngest.addCatalog(ocUtils.xmlToString(ocIngest.mediaPackage), ocIngest.createDublinCoreCatalog(ocIngest.metadata), 'dublincore/episode');
  }
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

ocIngest.addCatalog = function(mediaPackage, dcCatalog, flavor) {
  Upload.log("Adding DublinCore catalog");
  Upload.setProgress('100%','adding Metadata',' ', ' ');
  $.ajax({
    url        : '../ingest/rest/addDCCatalog',
    type       : 'POST',
    dataType   : 'xml',
    data       : {
      flavor : flavor,
      mediaPackage: mediaPackage,
      dublinCore  : ocUtils.xmlToString(dcCatalog)
    },
    error      : function(XHR,status,e){
      showFailedScreen('Could not add DublinCore catalog to MediaPackage.');
    },
    success    : function(data, status) {
      Upload.log("DublinCore catalog added");
      ocIngest.mediaPackage = data;
      var seriesId = $('#isPartOf').val();
      if (seriesId && ocIngest.seriesDC == null) {
        ocIngest.addSeriesCatalog(seriesId);
      } else {
        ocIngest.startIngest(data);
      }
    }
  });
}

ocIngest.addSeriesCatalog = function(seriesId) {
  Upload.log("Getting sweries DublinCore");
  Upload.setProgress('100%','Getting series Metadata',' ', ' ');
  $.ajax({
    url        : '../series/rest/'+seriesId+'.xml',
    type       : 'GET',
    error      : function(XHR,status,e){
      showFailedScreen('The metadata for the series you selected could not be retrieved.');
    },
    success    : function(data, status) {
      Upload.log("Adding series metadata");
      ocIngest.seriesDC = data;
      ocIngest.addCatalog(ocUtils.xmlToString(ocIngest.mediaPackage), data, 'dublincore/series');
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
  var data = ocWorkflow.getConfiguration($('#workflow-config-container'));
  data['mediaPackage'] = ocUtils.xmlToString(mediaPackage);
  $.ajax({
    url        : '../ingest/rest/ingest/' + $('#workflow-selector').val(),
    type       : 'POST',
    dataType   : 'text',
    data       : data,
    error      : function(XHR,status,e) {
      showFailedScreen("Could not start Ingest on MediaPackage");
    },
    success    : function(data, status) {
      if (Upload.retryId != '') {
        ocIngest.removeWorkflowInstance(Upload.retryId);
      } else {
        Upload.hideProgressStage();
        Upload.showSuccessScreen();
      }
    }
  });
}

ocIngest.removeWorkflowInstance = function(wfId) {
  $.ajax({
    url : '../workflow/rest/stop/',
    data: {id: wfId},
    type: 'POST',
    error: function() {
      Upload.hideProgressStage();   // better than showing error since new workflow has already been successfully started at this point
      Upload.showSuccessScreen();
    },
    success: function() {
      Upload.hideProgressStage();
      Upload.showSuccessScreen();
    }
  });
}